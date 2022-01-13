package ch.epfl.core

import ch.epfl.core.compiler.IncompatibleBindingsException
import ch.epfl.core.compiler.knxProgramming.Programmer
import ch.epfl.core.model.application.ApplicationLibrary
import ch.epfl.core.model.physical.PhysicalStructure
import ch.epfl.core.parser.ets.EtsParser
import ch.epfl.core.parser.json.physical.PhysicalStructureJsonParser
import ch.epfl.core.utils.Cli._
import ch.epfl.core.utils.Constants._
import ch.epfl.core.utils.Printer._
import ch.epfl.core.utils.Utils.loadApplicationsLibrary
import ch.epfl.core.utils.style.{ColorsStyle, NoColorsStyle}
import ch.epfl.core.utils.{Constants, FileUtils, Style}
import ch.epfl.core.verifier.exceptions.{VerifierError, VerifierInfo, VerifierMessage, VerifierWarning}
import ch.epfl.core.verifier.static.python.ProcRunner
import mainargs.ParserForClass

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}
import java.io.File

object Main {

  private val SUCCESS_CODE = 0
  private val ERROR_CODE = 1

  private val CLI_TOTAL_WIDTH = 200

  private var systemExit: SystemExit = DefaultSystemExit
  private var runtimeModule: String = RUNTIME_PYTHON_MODULE
  private var runtimeModulePath: os.Path = RUNTIME_PYTHON_MODULE_PATH

  def main(args: Array[String]): Unit = {
    val config = ParserForClass[Config].constructOrExit(args, totalWidth = CLI_TOTAL_WIDTH)
    implicit val style: Style = if (config.noColors.value) NoColorsStyle else ColorsStyle

    val existingAppsLibrary = loadApplicationsLibrary(APP_LIBRARY_FOLDER_PATH)
    val newAppsLibrary = loadApplicationsLibrary(GENERATED_FOLDER_PATH)

    // Check if app_library folder exists and if not, create it
    if (!os.exists(APP_LIBRARY_FOLDER_PATH)) os.makeDir.all(APP_LIBRARY_FOLDER_PATH)

    val existingPhysStructPath = existingAppsLibrary.path / PHYSICAL_STRUCTURE_JSON_FILE_NAME
    val existingPhysicalStructure = if (os.exists(existingPhysStructPath)) PhysicalStructureJsonParser.parse(existingPhysStructPath) else PhysicalStructure(Nil)

    def extractPhysicalStructure(etsProjPathString: String): PhysicalStructure = {
      val etsProjPath = Try(os.Path(etsProjPathString)) match {
        case Failure(exception) => {
          printErrorAndExit(exception.getLocalizedMessage)
          return PhysicalStructure(Nil) // Does not matter as the previous call will exit the program
        }
        case Success(value) => value
      }
      val newPhysicalStructure = EtsParser.parseEtsProjectFile(etsProjPath)
      newPhysicalStructure
    }

    config.task match {
      case GetVersion =>
        val version = getClass.getPackage.getImplementationVersion
        success(s"svshi v$version")
      case Run =>
        run(config.knxAddress, existingAppsLibrary)
      case Compile | GenerateBindings if config.etsProjectFile.isEmpty =>
        printErrorAndExit("The ETS project file needs to be specified to compile or to generate the bindings")
      case Compile =>
        val etsProjectFile = config.etsProjectFile.get
        if (!new File(etsProjectFile).isAbsolute()) printErrorAndExit("The ETS project file name has to be absolute")
        else compileApps(existingAppsLibrary, newAppsLibrary, extractPhysicalStructure(etsProjectFile))
      case GenerateBindings =>
        val etsProjectFile = config.etsProjectFile.get
        if (!new File(etsProjectFile).isAbsolute()) printErrorAndExit("The ETS project file name has to be absolute")
        else generateBindings(existingAppsLibrary, newAppsLibrary, existingPhysicalStructure, extractPhysicalStructure(etsProjectFile))
      case GenerateApp =>
        generateApp(config.appName, config.devicesPrototypicalStructureFile)
      case RemoveApp =>
        removeApps(config.all.value, config.appName, existingAppsLibrary)
      case ListApps =>
        listApps(existingAppsLibrary)
    }
  }

  private def removeApps(allFlag: Boolean, appName: Option[String], existingAppsLibrary: ApplicationLibrary)(implicit style: Style): Unit = {
    def removeAllApps(verbose: Boolean = true): Unit = {
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

      // Delete 'addresses.json' in all apps we are moving
      FileUtils.getListOfFolders(existingAppsLibrary.path).foreach(p => os.remove(p / APP_PYTHON_ADDR_BINDINGS_FILE_NAME))

      FileUtils.moveAllFileToOtherDirectory(existingAppsLibrary.path, DELETED_APPS_FOLDER_PATH)

      if (verbose) success("All the installed apps have been removed!")
    }

    def removeApp(name: String): Unit = {
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
      def backupAppLibrary(): Unit = {
        os.remove.all(APP_LIBRARY_TEMP_FOLDER_DURING_REMOVING_PATH)
        os.copy(APP_LIBRARY_FOLDER_PATH, APP_LIBRARY_TEMP_FOLDER_DURING_REMOVING_PATH)
      }
      def restoreAppLibraryFromBackup(): Unit = {
        os.remove.all(APP_LIBRARY_FOLDER_PATH)
        os.copy(APP_LIBRARY_TEMP_FOLDER_DURING_REMOVING_PATH, APP_LIBRARY_FOLDER_PATH)
        os.remove.all(APP_LIBRARY_TEMP_FOLDER_DURING_REMOVING_PATH)
      }
      def deleteFromDeletedApps(name: String): Unit = {
        os.remove.all(DELETED_APPS_FOLDER_PATH / name)
      }
      if (!os.exists(DELETED_APPS_FOLDER_PATH)) os.makeDir.all(DELETED_APPS_FOLDER_PATH)
      if (!existingAppsLibrary.apps.exists(a => a.name == name)) printErrorAndExit(s"The app '$name' is not installed!")

      val answer = scala.io.StdIn.readLine(s"The app '$name' will be removed. Are you sure [y/n]? ")
      if (!(answer != null && (answer.toLowerCase == "y" || answer.toLowerCase == "yes"))) {
        info("Exiting...")
        return
      }

      // First check if it is the last app of the library, if yes, call removeAllApps
      if (existingAppsLibrary.apps.forall(a => a.name == name)) {
        info(s"Removing application '$name'...")
        removeAllApps(verbose = false)
        success(s"The app '$name' has been successfully removed!")
        return
      }

      info(s"Removing application '$name'...")
      // To remove an app, we recompile the existing appLibrary without the new application library after removing the
      // app to uninstall from the existing appLibrary
      // IMPORTANT: First compile and verify remaining apps to see if everything still passes (it should). Only then remove the app from the folder

      // IMPORTANT: As the pipeline uses the GENERATED folder for doing its things, we need to first empty it by moving everything it contains in a temp folder
      // and we will copy everything back when the removing is done
      backupGeneratedFolder()
      backupAppLibrary()

      val emptyApplibrary = ApplicationLibrary(Nil, GENERATED_FOLDER_PATH)
      val existingPhysicalStructure = PhysicalStructureJsonParser.parse(existingAppsLibrary.path / Constants.PHYSICAL_STRUCTURE_JSON_FILE_NAME)

      // Remove the app from the library
      val newExistingLibrary = ApplicationLibrary(existingAppsLibrary.apps.filterNot(a => a.name == name), existingAppsLibrary.path)

      // Generate new bindings to remove the application
      // They will go in generated
      generateBindings(newExistingLibrary, emptyApplibrary, existingPhysicalStructure, existingPhysicalStructure)

      // Remove the app from the folder
      os.makeDir.all(DELETED_APPS_FOLDER_PATH / name)
      FileUtils.moveAllFileToOtherDirectory(existingAppsLibrary.path / name, DELETED_APPS_FOLDER_PATH / name)
      os.remove.all(existingAppsLibrary.path / name)

      Try(compileAppsOperations(newExistingLibrary, emptyApplibrary, existingPhysicalStructure)) match {
        case Failure(exception) => {
          restoreGeneratedFolder()
          restoreAppLibraryFromBackup()
          deleteFromDeletedApps(name)
          printErrorAndExit(
            s"Removing the application '$name' causes the verification of the remaining applications to fail. Compiler threw the exception: ${exception.getLocalizedMessage}\n\n The app '$name' has not been removed.'"
          )
        }
        case Success((ok, verifierMessages)) => {
          restoreGeneratedFolder()
          if (ok) {
            // Delete 'addresses.json' in all app we are moving
            os.remove(DELETED_APPS_FOLDER_PATH / name / APP_PYTHON_ADDR_BINDINGS_FILE_NAME)

            // Delete temp generated folder + backup app library
            os.remove.all(GENERATED_TEMP_FOLDER_DURING_REMOVING_PATH)
            os.remove.all(APP_LIBRARY_TEMP_FOLDER_DURING_REMOVING_PATH)
            success(s"The app '$name' has been successfully removed!")
          } else {
            // Move the app back to revert
            restoreAppLibraryFromBackup()
            deleteFromDeletedApps(name)
            printTrace(verifierMessages)
            printErrorAndExit(
              s"Removing the application '$name' causes the verification of the remaining applications to fail. Please see trace above for more information. The app '$name' has not been removed.'"
            )
          }
        }
      }
    }

    if (allFlag) {
      val answer = scala.io.StdIn.readLine("All the apps will be removed. Are you sure [y/n]? ")
      if (answer != null && (answer.toLowerCase == "y" || answer.toLowerCase == "yes")) removeAllApps()
      else info("Exiting...")
    } else {
      appName match {
        case Some(name) => removeApp(name)
        case None       => printErrorAndExit("The app name has to be provided to remove an app")
      }
    }
  }

  private def run(knxAddress: Option[String], existingAppsLibrary: ApplicationLibrary)(implicit style: Style): Unit = {
    knxAddress match {
      case Some(address) =>
        val addressRegex = """^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3}):(\d)+$""".r
        if (addressRegex.matches(address)) {
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
              FileUtils.copyFiles(appFiles.map(fName => appLibraryPath / appName / fName), appFilesPath)
            }
          }

          // Run the runtime module
          runPythonModule(runtimeModule, address.split(":"), exitCode => s"The runtime module failed with exit code $exitCode and above stdout")

          // Clear all files used by apps
          os.remove.all(filesDirPath)
        } else printErrorAndExit("The KNX address and port need to have the format 'address:port' where address is a valid IPv4 address and port a valid port")
      case None => printErrorAndExit("The KNX address and port need to be specified to run the apps")
    }
  }

  private def listApps(existingAppsLibrary: ApplicationLibrary)(implicit style: Style): Unit = {
    info("Listing the apps...")
    val appNames = existingAppsLibrary.apps.map(app => s"'${app.name}'")
    if (appNames.isEmpty) warning("WARNING: There are no installed applications!")
    else success(s"The installed apps are: ${appNames.mkString(", ")}")
  }

  private def generateApp(appName: Option[String], devicesPrototypicalStructureFile: Option[String])(implicit style: Style): Unit = {
    info("Generating the app...")
    (appName, devicesPrototypicalStructureFile) match {
      case (Some(name), Some(devicesJson)) =>
        val nameRegex = "^_*[a-z]+[a-z_]*_*$".r
        if (nameRegex.matches(name)) {
          if (!new File(devicesJson).isAbsolute()) printErrorAndExit("The devices prototypical structure JSON file name has to be absolute")
          else runPythonModule(APP_GENERATOR_PYTHON_MODULE, Seq(name, devicesJson), exitCode => s"The app generator failed with exit code $exitCode and above stdout")
        } else printErrorAndExit("The app name has to contain only lowercase letters and underscores")
        success(s"The app '$name' has been successfully created!")
      case (None, Some(_)) =>
        printErrorAndExit("The app name has to be provided to generate a new app")
      case (Some(_), None) =>
        printErrorAndExit("The devices prototypical structure JSON file has to be provided to generate a new app")
      case _ =>
        printErrorAndExit("The devices prototypical structure JSON file and the app name have to be provided to generate a new app")
    }
  }

  private def generateBindings(
      existingAppsLibrary: ApplicationLibrary,
      newAppsLibrary: ApplicationLibrary,
      existingPhysicalStructure: PhysicalStructure,
      newPhysicalStructure: PhysicalStructure
  )(implicit style: Style): Unit = {
    // First check that no app with the same name as any new apps is already installed
    checkForAppDuplicates(existingAppsLibrary = existingAppsLibrary, newAppsLibrary = newAppsLibrary)

    info("Generating the bindings...")
    compiler.Compiler.generateBindingsFiles(newAppsLibrary, existingAppsLibrary, newPhysicalStructure, existingPhysicalStructure)
    success(s"The bindings have been successfully created!")
  }

  private def compileApps(existingAppsLibrary: ApplicationLibrary, newAppsLibrary: ApplicationLibrary, newPhysicalStructure: PhysicalStructure)(implicit style: Style): Unit = {
    // First check that no app with the same name as any new apps is already installed
    checkForAppDuplicates(existingAppsLibrary = existingAppsLibrary, newAppsLibrary = newAppsLibrary)

    info("Compiling and verifying the apps...")

    Try(compileAppsOperations(existingAppsLibrary, newAppsLibrary, newPhysicalStructure)) match {
      case Failure(exception: CompileErrorException) => printErrorAndExit(exception.msg)
      case Failure(exception)                        => printErrorAndExit(s"$exception\nCompilation/verification failed, see messages above")
      case Success((ok, verifierMessages)) => {
        printTrace(verifierMessages)
        if (ok) {
          success(s"The apps have been successfully compiled and verified!")
        } else {
          printErrorAndExit("Compilation/verification failed, see messages above")
        }
      }
    }
  }

  private def checkForAppDuplicates(existingAppsLibrary: ApplicationLibrary, newAppsLibrary: ApplicationLibrary)(implicit style: Style): Unit = {
    newAppsLibrary.apps.foreach(a =>
      if (existingAppsLibrary.apps.exists(existingA => existingA.name == a.name))
        printErrorAndExit(s"An application with the name '${a.name}' is already installed! You cannot install two apps with the same name!")
    )
  }

  private def compileAppsOperations(
      existingAppsLibrary: ApplicationLibrary,
      newAppsLibrary: ApplicationLibrary,
      newPhysicalStructure: PhysicalStructure
  )(implicit style: Style): (Boolean, List[VerifierMessage]) = {
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
          List(GENERATED_VERIFICATION_FILE_PATH, GENERATED_RUNTIME_FILE_PATH, GENERATED_CONDITIONS_FILE_PATH).foreach(p => os.move.into(p, appLibraryPath, replaceExisting = true))

          // Call the KNX Programmer module
          val programmer = Programmer(gaAssignment)
          programmer.outputProgrammingFile()
          programmer.outputGroupAddressesCsv()

          (true, verifierMessages)
        } else {
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

  @tailrec
  private def printTrace(messages: List[VerifierMessage])(implicit style: Style): Unit = {
    messages match {
      case head :: tl => {
        head match {
          case verifierError: VerifierError     => error(verifierError.msg)
          case verifierWarning: VerifierWarning => warning(s"WARNING: ${verifierWarning.msg}")
          case verifierInfo: VerifierInfo       => info(s"INFO: ${verifierInfo.msg}")
          case _                                => ()
        }
        printTrace(tl)
      }
      case Nil => ()
    }
  }

  case class CompileErrorException(msg: String) extends Exception(msg)
  def setSystemExit(newSystemExit: SystemExit): Unit = systemExit = newSystemExit

  def setRuntimeModule(newRuntimeModule: String): Unit = runtimeModule = newRuntimeModule

  def setRuntimeModulePath(newRuntimeModulePath: os.Path): Unit = runtimeModulePath = newRuntimeModulePath

  private def printErrorAndExit(errorMessage: String)(implicit style: Style): Unit = {
    error(s"ERROR: $errorMessage")
    systemExit.exit(ERROR_CODE)
  }

  private def runPythonModule(module: String, args: Seq[String], errorMessageBuilder: Int => String)(implicit style: Style): Unit = {
    val (exitCode, _) = ProcRunner.callPython(module, os.Path(SVSHI_FOLDER), args: _*)
    if (exitCode != SUCCESS_CODE) printErrorAndExit(errorMessageBuilder(exitCode))
  }
}

trait SystemExit {
  def exit(errorCode: Int): Unit
}

object DefaultSystemExit extends SystemExit {
  override def exit(errorCode: Int): Unit = sys.exit(errorCode)
}
