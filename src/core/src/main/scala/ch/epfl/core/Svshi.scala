package ch.epfl.core

import ch.epfl.core.compiler.IncompatibleBindingsException
import ch.epfl.core.compiler.knxProgramming.Programmer
import ch.epfl.core.model.application.ApplicationLibrary
import ch.epfl.core.model.physical.PhysicalStructure
import ch.epfl.core.parser.json.physical.PhysicalStructureJsonParser
import ch.epfl.core.utils.Constants._
import ch.epfl.core.utils.{Constants, FileUtils, ProcRunner}
import ch.epfl.core.verifier.exceptions.{VerifierError, VerifierInfo, VerifierMessage, VerifierWarning}

import java.io.File
import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

object Svshi {
  val SUCCESS_CODE = 0
  val ERROR_CODE = 1
  val PIP_SUCCESS_CODE = 0
  private var runtimeModule: String = RUNTIME_PYTHON_MODULE
  private var runtimeModulePath: os.Path = RUNTIME_PYTHON_MODULE_PATH

  def getVersion(success: String => Unit): Int = {
    val version = getClass.getPackage.getImplementationVersion
    success(s"svshi v$version")
    SUCCESS_CODE
  }

  def run(
      knxAddress: Option[String],
      existingAppsLibrary: ApplicationLibrary
  )(success: String => Unit = _ => (), info: String => Unit = _ => (), warning: String => Unit = _ => (), err: String => Unit = _ => ()): Int = {
    knxAddress match {
      case Some(address) =>
        val addressRegex = """^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3}):(\d)+$""".r
        if (addressRegex.matches(address)) {
          if (existingAppsLibrary.apps.isEmpty) {
            err("No apps are installed!")
            return ERROR_CODE
          }

          info("Running the apps...")
          // Copy verification_file.py, runtime_file.py and conditions.py in runtime module
          val appLibraryPath = existingAppsLibrary.path
          FileUtils.copyFiles(List(appLibraryPath / "verification_file.py", appLibraryPath / "runtime_file.py", appLibraryPath / "conditions.py"), runtimeModulePath)

          // Copy files used by each app
          val filesDirPath = runtimeModulePath / "files"
          os.remove.all(filesDirPath)
          os.makeDir.all(filesDirPath)

          val appToFiles = existingAppsLibrary.apps.map(a => (a.name, a.appProtoStructure.files)).toMap
          appToFiles.foreach {
            case (appName, appFiles) => {
              val appFilesPath = filesDirPath / appName
              if (!os.exists(appFilesPath)) os.makeDir.all(appFilesPath)
              FileUtils.copyFiles(appFiles.map(fName => appLibraryPath / appName / fName).filter(fPath => os.exists(fPath)), appFilesPath)
            }
          }

          // Install apps' requirements
          existingAppsLibrary.apps.foreach { app =>
            info(s"Installing requirements of the app '${app.name}...'")
            val path = app.appFolderPath
            val (i, msgs) = ProcRunner.callPython(None, None, "pip", path, "install", "-r", "requirements.txt")
            if (i != PIP_SUCCESS_CODE) {
              err(s"Cannot install requirements for app '${app.name}'. See pip outputs below:")
              msgs.foreach(err)
              return ERROR_CODE
            }
          }

          // Run the runtime module
          runPythonModule(
            module = runtimeModule,
            args = address.split(":"),
            errorMessageBuilder = exitCode => s"The runtime module failed with exit code $exitCode and above stdout",
            success = success,
            info = info,
            err = err
          )

          // Clear all files used by apps
          os.remove.all(filesDirPath)
          SUCCESS_CODE
        } else {
          err("The KNX address and port need to have the format 'address:port' where address is a valid IPv4 address and port a valid port")
          ERROR_CODE
        }
      case None => {
        err("The KNX address and port need to be specified to run the apps")
        ERROR_CODE
      }
    }
  }

  def compileApps(
      existingAppsLibrary: ApplicationLibrary,
      newAppsLibrary: ApplicationLibrary,
      newPhysicalStructure: PhysicalStructure
  )(success: String => Unit = _ => (), info: String => Unit = _ => (), warning: String => Unit = _ => (), err: String => Unit = _ => ()): Int = {
    // First check that no app with the same name as any new apps is already installed
    val d = checkForAppDuplicates(existingAppsLibrary = existingAppsLibrary, newAppsLibrary = newAppsLibrary, success = success, info = info, err = err)
    if (d != SUCCESS_CODE) {
      return d
    }

    info("Compiling and verifying the apps...")

    Try(compileAppsOperations(existingAppsLibrary, newAppsLibrary, newPhysicalStructure)) match {
      case Failure(exception: CompileErrorException) => {
        err(exception.msg)
        ERROR_CODE
      }
      case Failure(exception) => {
        err(s"$exception\nCompilation/verification failed, see messages above.")
        ERROR_CODE
      }
      case Success((ok, verifierMessages)) => {
        outputTrace(verifierMessages)(success = success, info = info, warning = warning, err = err)
        if (ok) {
          success("The apps have been successfully compiled and verified!")
          SUCCESS_CODE
        } else {
          err("Compilation/verification failed, see messages above.")
          ERROR_CODE
        }
      }
    }
  }

  def updateApp(
      existingAppsLibrary: ApplicationLibrary,
      newAppsLibrary: ApplicationLibrary,
      appToUpdateName: String,
      existingPhysicalStructure: PhysicalStructure
  )(success: String => Unit = _ => (), info: String => Unit = _ => (), warning: String => Unit = _ => (), err: String => Unit = _ => ()): Int = {
    if (!existingAppsLibrary.apps.exists(a => a.name == appToUpdateName)) {
      err(s"The app '$appToUpdateName' must be installed!")
      return ERROR_CODE
    }

    if (!newAppsLibrary.apps.exists(a => a.name == appToUpdateName)) {
      err(s"The app '$appToUpdateName' must be in the generated folder!")
      return ERROR_CODE
    }

    val otherAppsFound = newAppsLibrary.apps.filter(a => a.name != appToUpdateName)
    if (otherAppsFound.nonEmpty) {
      err(s"The app '$appToUpdateName' must be the only one in the generated folder! Other apps found: ${otherAppsFound.map(_.name).mkString(", ")}")
      return ERROR_CODE
    }

    val oldProtoStructure = existingAppsLibrary.apps.find(a => a.name == appToUpdateName).get.appProtoStructure
    val newProtoStructure = newAppsLibrary.apps.find(a => a.name == appToUpdateName).get.appProtoStructure
    if (oldProtoStructure != newProtoStructure) {
      err(s"The prototypical structure of the app '$appToUpdateName' has changed: the update cannot be performed!")
      return ERROR_CODE
    }

    info(s"Updating app '$appToUpdateName'...")

    // Backup old appLibrary in case of error
    backupAppLibrary(APP_LIBRARY_TEMP_FOLDER_DURING_UPDATE_PATH)

    // Copy old bindings
    os.copy.into(APP_LIBRARY_FOLDER_PATH / APP_PROTO_BINDINGS_JSON_FILE_NAME, GENERATED_FOLDER_PATH)

    // Uninstall the app to update
    val resRemove = removeApps(allFlag = false, appName = Some(appToUpdateName), existingAppsLibrary = existingAppsLibrary)()
    if (resRemove != SUCCESS_CODE) {
      err(s"An error occurred while removing the app to update. Please uninstall and reinstall it manually.")
      return ERROR_CODE
    }

    val newExistingAppLibrary = ApplicationLibrary(existingAppsLibrary.apps.filter(a => a.name != appToUpdateName), existingAppsLibrary.path)

    // Install the generated folder
    Try(compileAppsOperations(newExistingAppLibrary, newAppsLibrary, existingPhysicalStructure)) match {
      case Failure(exception: CompileErrorException) => {
        err(exception.msg)
        err(s"The compilation of the new version of '$appToUpdateName' failed. Rollbacking to the old set of apps...")
        restoreAppLibraryFromBackup(APP_LIBRARY_TEMP_FOLDER_DURING_UPDATE_PATH)
        ERROR_CODE
      }
      case Failure(exception) => {
        err(s"$exception\nCompilation/verification failed, see messages above.")
        err(s"The compilation of the new version of '$appToUpdateName' failed. Rollbacking to the old set of apps...")
        restoreAppLibraryFromBackup(APP_LIBRARY_TEMP_FOLDER_DURING_UPDATE_PATH)
        ERROR_CODE
      }
      case Success((ok, verifierMessages)) => {
        outputTrace(verifierMessages)(success, info, warning, err)
        if (ok) {
          success(s"The app '$appToUpdateName' has been successfully compiled and verified! Update successful!")
          SUCCESS_CODE
        } else {
          err("Compilation/verification failed, see messages above.")
          err(s"The compilation of the new version of '$appToUpdateName' failed. Rollbacking to the old set of apps...")
          restoreAppLibraryFromBackup(APP_LIBRARY_TEMP_FOLDER_DURING_UPDATE_PATH)
          ERROR_CODE
        }
      }
    }
  }

  def generateBindings(
      existingAppsLibrary: ApplicationLibrary,
      newAppsLibrary: ApplicationLibrary,
      existingPhysicalStructure: PhysicalStructure,
      newPhysicalStructure: PhysicalStructure
  )(success: String => Unit = _ => (), info: String => Unit = _ => (), warning: String => Unit = _ => (), err: String => Unit = _ => ()): Int = {
    // First check that no app with the same name as any new apps is already installed
    val d = checkForAppDuplicates(existingAppsLibrary = existingAppsLibrary, newAppsLibrary = newAppsLibrary, success = success, info = info, err = err)
    if (d != SUCCESS_CODE) {
      return d
    }

    info("Generating the bindings...")
    compiler.Compiler.generateBindingsFiles(newAppsLibrary, existingAppsLibrary, newPhysicalStructure, existingPhysicalStructure)
    success("The bindings have been successfully created!")
    SUCCESS_CODE
  }

  def generateApp(
      appName: Option[String],
      devicesPrototypicalStructureFile: Option[String]
  )(success: String => Unit = _ => (), info: String => Unit = _ => (), warning: String => Unit = _ => (), err: String => Unit = _ => ()): Int = {
    info("Generating the app...")
    (appName, devicesPrototypicalStructureFile) match {
      case (Some(name), Some(devicesJson)) =>
        val nameRegex = "^_*[a-z]+[a-z_]*_*$".r
        if (nameRegex.matches(name)) {
          if (!new File(devicesJson).isAbsolute) {
            err("The devices prototypical structure JSON file name has to be absolute")
            return ERROR_CODE
          } else
            runPythonModule(
              APP_GENERATOR_PYTHON_MODULE,
              Seq(name, devicesJson),
              exitCode => s"The app generator failed with exit code $exitCode and above stdout",
              success = success,
              info = info,
              err = err
            )
        } else {
          err("The app name has to contain only lowercase letters and underscores")
          return ERROR_CODE
        }
        success(s"The app '$name' has been successfully created!")
        return SUCCESS_CODE
      case (None, Some(_)) => {
        err("The app name has to be provided to generate a new app")
        return ERROR_CODE
      }
      case (Some(_), None) => {
        err("The devices prototypical structure JSON file has to be provided to generate a new app")
        return ERROR_CODE
      }
      case _ => {
        err("The devices prototypical structure JSON file and the app name have to be provided to generate a new app")
        return ERROR_CODE
      }
    }
  }

  def removeApps(
      allFlag: Boolean,
      appName: Option[String],
      existingAppsLibrary: ApplicationLibrary
  )(success: String => Unit = _ => (), info: String => Unit = _ => (), warning: String => Unit = _ => (), err: String => Unit = _ => ()): Int = {
    def removeAllApps(verbose: Boolean = true): Int = {
      if (verbose) info(s"Removing all applications...")
      if (!os.exists(DELETED_APPS_FOLDER_PATH)) os.makeDir.all(DELETED_APPS_FOLDER_PATH)

      List(
        APP_PYTHON_ADDR_BINDINGS_FILE_NAME,
        APP_PROTO_BINDINGS_JSON_FILE_NAME,
        GENERATED_VERIFICATION_FILE_NAME,
        GENERATED_RUNTIME_FILE_NAME,
        GENERATED_CONDITIONS_FILE_NAME,
        PHYSICAL_STRUCTURE_JSON_FILE_NAME,
        GROUP_ADDRESSES_LIST_FILE_NAME
      ).foreach(filename => os.remove.all(existingAppsLibrary.path / filename))

      os.remove.all(INSTALLED_APPS_FOLDER_PATH)

      // Delete 'addresses.json' in all apps we are moving
      FileUtils.getListOfFolders(existingAppsLibrary.path).foreach(p => os.remove(p / APP_PYTHON_ADDR_BINDINGS_FILE_NAME))

      FileUtils.moveAllFileToOtherDirectory(existingAppsLibrary.path, DELETED_APPS_FOLDER_PATH)

      if (verbose) {
        success("All the installed apps have been removed!")
      }
      return SUCCESS_CODE
    }

    def removeApp(name: String): Int = {
      def backupGeneratedFolder(): Unit = {
        os.remove.all(GENERATED_TEMP_FOLDER_DURING_REMOVING_PATH)
        os.makeDir.all(GENERATED_TEMP_FOLDER_DURING_REMOVING_PATH)
        FileUtils.moveAllFileToOtherDirectory(GENERATED_FOLDER_PATH, GENERATED_TEMP_FOLDER_DURING_REMOVING_PATH)
        // To be sure generated is empty
        os.remove.all(GENERATED_FOLDER_PATH)
        os.makeDir.all(GENERATED_FOLDER_PATH)
      }

      def restoreGeneratedFolder(): Unit = {
        os.remove.all(GENERATED_FOLDER_PATH)
        os.makeDir.all(GENERATED_FOLDER_PATH)
        FileUtils.moveAllFileToOtherDirectory(GENERATED_TEMP_FOLDER_DURING_REMOVING_PATH, GENERATED_FOLDER_PATH)
        os.remove.all(GENERATED_TEMP_FOLDER_DURING_REMOVING_PATH)
      }

      def deleteFromDeletedApps(name: String): Unit = {
        os.remove.all(DELETED_APPS_FOLDER_PATH / name)
      }

      if (!os.exists(DELETED_APPS_FOLDER_PATH)) os.makeDir.all(DELETED_APPS_FOLDER_PATH)
      if (!existingAppsLibrary.apps.exists(a => a.name == name)) {
        err(s"The app '$name' is not installed!")
        return ERROR_CODE
      }

      // First check if it is the last app of the library, if yes, call removeAllApps
      if (existingAppsLibrary.apps.forall(a => a.name == name)) {
        info(s"Removing application '$name'...")
        val removeAllCode = removeAllApps(verbose = false)
        if (removeAllCode == SUCCESS_CODE) {
          success(s"The app '$name' has been successfully removed!")
          return SUCCESS_CODE
        } else {
          return removeAllCode
        }
      }

      info(s"Removing application '$name'...")
      // To remove an app, we recompile the existing appLibrary without the new application library after removing the
      // app to uninstall from the existing appLibrary
      // IMPORTANT: First compile and verify remaining apps to see if everything still passes (it should). Only then remove the app from the folder

      // IMPORTANT: As the pipeline uses the GENERATED folder for doing its things, we need to first empty it by moving everything it contains in a temp folder
      // and we will copy everything back when the removing is done
      backupGeneratedFolder()
      backupAppLibrary(APP_LIBRARY_TEMP_FOLDER_DURING_REMOVING_PATH)

      val emptyAppLibrary = ApplicationLibrary(Nil, GENERATED_FOLDER_PATH)
      val existingPhysicalStructure = PhysicalStructureJsonParser.parse(existingAppsLibrary.path / Constants.PHYSICAL_STRUCTURE_JSON_FILE_NAME)

      // Remove the app from the library
      val newExistingLibrary = ApplicationLibrary(existingAppsLibrary.apps.filterNot(a => a.name == name), existingAppsLibrary.path)

      // Generate new bindings to remove the application
      // They will go in generated
      val genBindingCode = generateBindings(
        existingAppsLibrary = newExistingLibrary,
        newAppsLibrary = emptyAppLibrary,
        existingPhysicalStructure = existingPhysicalStructure,
        newPhysicalStructure = existingPhysicalStructure
      )(success = s => (), info = s => (), warning = warning, err = err)
      if (genBindingCode != SUCCESS_CODE) {
        restoreGeneratedFolder()
        restoreAppLibraryFromBackup(APP_LIBRARY_TEMP_FOLDER_DURING_REMOVING_PATH)
        deleteFromDeletedApps(name)
        err(
          s"Removing the application '$name' causes the bindings generation of the remaining applications to fail.\nThe app '$name' has not been removed.'"
        )
        return ERROR_CODE
      }

      // Remove the app from the folder
      os.makeDir.all(DELETED_APPS_FOLDER_PATH / name)
      FileUtils.moveAllFileToOtherDirectory(existingAppsLibrary.path / name, DELETED_APPS_FOLDER_PATH / name)
      os.remove.all(existingAppsLibrary.path / name)

      Try(compileAppsOperations(newExistingLibrary, emptyAppLibrary, existingPhysicalStructure)) match {
        case Failure(exception) => {
          restoreGeneratedFolder()
          restoreAppLibraryFromBackup(APP_LIBRARY_TEMP_FOLDER_DURING_REMOVING_PATH)
          deleteFromDeletedApps(name)
          err(
            s"Removing the application '$name' causes the verification of the remaining applications to fail. Compiler threw the exception: ${exception.getLocalizedMessage}\n\n The app '$name' has not been removed.'"
          )
          return ERROR_CODE
        }
        case Success((ok, verifierMessages)) => {
          restoreGeneratedFolder()
          if (ok) {
            // Delete 'addresses.json' in all app we are moving
            os.remove(DELETED_APPS_FOLDER_PATH / name / APP_PYTHON_ADDR_BINDINGS_FILE_NAME)

            // Delete temp generated folder + backup app library
            os.remove.all(GENERATED_TEMP_FOLDER_DURING_REMOVING_PATH)
            os.remove.all(APP_LIBRARY_TEMP_FOLDER_DURING_REMOVING_PATH)

            // Remove app from installedApps folder
            os.remove.all(INSTALLED_APPS_FOLDER_PATH / name)

            success(s"The app '$name' has been successfully removed!")
            return SUCCESS_CODE
          } else {
            // Move the app back to revert
            restoreAppLibraryFromBackup(APP_LIBRARY_TEMP_FOLDER_DURING_REMOVING_PATH)
            deleteFromDeletedApps(name)
            outputTrace(verifierMessages)(success = success, info = info, warning = warning, err = err)
            err(
              s"Removing the application '$name' causes the verification of the remaining applications to fail. Please see trace above for more information. The app '$name' has not been removed.'"
            )
            return ERROR_CODE
          }
        }
      }
    }

    if (allFlag) {
      removeAllApps()
    } else {
      appName match {
        case Some(name) => removeApp(name)
        case None       => throw new IllegalArgumentException("The app name has to be provided to remove an app when the 'all' flag is not used")
      }
    }
  }

  def listApps(
      existingAppsLibrary: ApplicationLibrary
  )(success: String => Unit = _ => (), info: String => Unit = _ => (), warning: String => Unit = _ => (), err: String => Unit = _ => ()): Int = {
    info("Listing the apps...")
    val appNames = existingAppsLibrary.apps.map(app => s"'${app.name}'")
    if (appNames.isEmpty) warning("There are no installed applications!")
    else success(s"The installed apps are: ${appNames.mkString(", ")}")
    return SUCCESS_CODE
  }

  private def backupAppLibrary(destination: os.Path): Unit = {
    if (os.exists(destination)) os.remove.all(destination)
    os.copy.over(APP_LIBRARY_FOLDER_PATH, destination)
  }

  private def restoreAppLibraryFromBackup(source: os.Path): Unit = {
    if (os.exists(APP_LIBRARY_FOLDER_PATH)) os.remove.all(APP_LIBRARY_FOLDER_PATH)
    os.copy.over(source, APP_LIBRARY_FOLDER_PATH)
    os.remove.all(source)
  }

  private def compileAppsOperations(
      existingAppsLibrary: ApplicationLibrary,
      newAppsLibrary: ApplicationLibrary,
      newPhysicalStructure: PhysicalStructure
  ): (Boolean, List[VerifierMessage]) = {
    Try(compiler.Compiler.compile(newAppsLibrary, existingAppsLibrary, newPhysicalStructure)) match {
      case Failure(exception) => {
        exception match {
          case IncompatibleBindingsException() =>
            throw CompileErrorException(
              "The bindings are not compatible with the apps you want to install! Please run generateBindings again and fill them before compiling again."
            )
          case _ => throw CompileErrorException(s"The compiler produces an exception: $exception")
        }
        (false, Nil)
      }
      case Success((compiledNewApps, compiledExistingApps, gaAssignment)) => {
        val verifierMessages = verifier.Verifier.verify(compiledNewApps, compiledExistingApps, gaAssignment)
        if (validateProgram(verifierMessages)) {
          // Copy new app + all files in app_library
          val appLibraryPath = existingAppsLibrary.path
          FileUtils.moveAllFileToOtherDirectory(GENERATED_FOLDER_PATH, appLibraryPath)

          // Move verification_file.py, runtime_file.py and conditions.py in app_library
          val generatedFiles = List(GENERATED_VERIFICATION_FILE_PATH, GENERATED_RUNTIME_FILE_PATH, GENERATED_CONDITIONS_FILE_PATH)
          generatedFiles.foreach(p => os.move.into(p, appLibraryPath, replaceExisting = true))

          // Copy all installed applications, their bindings and their physical structure in installedFolder,
          // without verification_file.py, runtime_file.py, conditions.py and group_addresses.json
          if (!os.exists(INSTALLED_APPS_FOLDER_PATH)) os.makeDir(INSTALLED_APPS_FOLDER_PATH)
          os.copy(appLibraryPath, INSTALLED_APPS_FOLDER_PATH, mergeFolders = true, replaceExisting = true)
          generatedFiles.foreach(p => os.remove(INSTALLED_APPS_FOLDER_PATH / p.last))
          os.remove(INSTALLED_APPS_FOLDER_PATH / GROUP_ADDRESSES_LIST_FILE_NAME)

          // Call the KNX Programmer module
          val programmer = Programmer(gaAssignment)
          programmer.outputProgrammingFile()
          programmer.outputGroupAddressesCsv()

          (true, verifierMessages)
        } else {
          // Remove group_addresses.json
          os.remove(GENERATED_FOLDER_PATH / GROUP_ADDRESSES_LIST_FILE_NAME)

          (false, verifierMessages)
        }
      }
    }
  }

  @tailrec
  private def validateProgram(messages: List[VerifierMessage]): Boolean = {
    messages match {
      case head :: tl =>
        head match {
          case _: VerifierError   => false
          case _: VerifierWarning => validateProgram(tl)
          case _: VerifierInfo    => validateProgram(tl)
        }
      case Nil => true
    }
  }

  private def checkForAppDuplicates(
      existingAppsLibrary: ApplicationLibrary,
      newAppsLibrary: ApplicationLibrary,
      success: String => Unit,
      info: String => Unit,
      err: String => Unit
  ): Int = {
    newAppsLibrary.apps
      .map(a =>
        if (existingAppsLibrary.apps.exists(existingA => existingA.name == a.name)) {
          err(s"An application with the name '${a.name}' is already installed! You cannot install two apps with the same name!")
          ERROR_CODE
        } else {
          SUCCESS_CODE
        }
      )
      .fold(SUCCESS_CODE)((x1, x2) => if (x1 == ERROR_CODE || x2 == ERROR_CODE) ERROR_CODE else SUCCESS_CODE)
  }

  private def runPythonModule(module: String, args: Seq[String], errorMessageBuilder: Int => String, success: String => Unit, info: String => Unit, err: String => Unit): Unit = {
    val (exitCode, _) = ProcRunner.callPython(Some(info), Some(err), module, os.Path(SVSHI_SRC_FOLDER), args: _*)
    if (exitCode != SUCCESS_CODE) err(errorMessageBuilder(exitCode))
  }
  @tailrec
  private def outputTrace(
      messages: List[VerifierMessage]
  )(success: String => Unit = _ => (), info: String => Unit = _ => (), warning: String => Unit = _ => (), err: String => Unit = _ => ()): Unit = {
    messages match {
      case head :: tl => {
        head match {
          case verifierError: VerifierError     => err(verifierError.msg)
          case verifierWarning: VerifierWarning => warning(verifierWarning.msg)
          case verifierInfo: VerifierInfo       => info(verifierInfo.msg)
          case _                                => ()
        }
        outputTrace(tl)(success, info, warning, err)
      }
      case Nil => ()
    }
  }

  case class CompileErrorException(msg: String) extends Exception(msg)

  def setRuntimeModule(newRuntimeModule: String): Unit = runtimeModule = newRuntimeModule

  def setRuntimeModulePath(newRuntimeModulePath: os.Path): Unit = runtimeModulePath = newRuntimeModulePath
}
