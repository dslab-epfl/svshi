package ch.epfl.core

import ch.epfl.core.model.physical.PhysicalStructure
import ch.epfl.core.parser.ets.EtsParser
import ch.epfl.core.parser.json.physical.PhysicalStructureJsonParser
import ch.epfl.core.utils.Cli._
import ch.epfl.core.utils.Constants._
import ch.epfl.core.utils.Printer._
import ch.epfl.core.utils.Utils.loadApplicationsLibrary
import ch.epfl.core.utils.style.{ColorsStyle, NoColorsStyle}
import ch.epfl.core.utils.{FileUtils, Style}
import ch.epfl.core.verifier.exceptions.{VerifierError, VerifierInfo, VerifierMessage, VerifierWarning}
import ch.epfl.core.verifier.static.python.ProcRunner
import mainargs.ParserForClass

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

object Main {

  private val SUCCESS_CODE = 0
  private val ERROR_CODE = 1

  def main(args: Array[String]): Unit = {
    val config = ParserForClass[Config].constructOrExit(args, totalWidth = 200)
    implicit val style = if (config.noColors.value) NoColorsStyle else ColorsStyle

    val existingAppsLibrary = loadApplicationsLibrary(APP_LIBRARY_FOLDER_PATH)
    val newAppsLibrary = loadApplicationsLibrary(GENERATED_FOLDER_PATH)

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
        config.knxAddress match {
          case Some(address) =>
            val addressRegex = """^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3}):(\d)+$""".r
            if (addressRegex.matches(address)) {
              info("Running the apps...")
              runPythonModule(RUNTIME_PYTHON_MODULE, address.split(":"), exitCode => s"The runtime module failed with exit code $exitCode and above stdout")
            } else printErrorAndExit("The KNX address and port need to have the format 'address:port' where address is a valid IPv4 address and port a valid port")
          case None => printErrorAndExit("The KNX address and port need to be specified to run the apps")
        }
      case Compile | GenerateBindings if config.etsProjectFile.isEmpty =>
        printErrorAndExit("The ETS project file needs to be specified to compile or to generate the bindings")
      case Compile =>
        info("Compiling the apps...")
        val etsProjPathString = config.etsProjectFile.get
        val newPhysicalStructure: PhysicalStructure = extractPhysicalStructure(etsProjPathString)
        val (compiledNewApps, compiledExistingApps, gaAssignment) = compiler.Compiler.compile(newAppsLibrary, existingAppsLibrary, newPhysicalStructure)
        val verifierMessages = verifier.Verifier.verify(compiledNewApps, compiledExistingApps, gaAssignment)
        if (validateProgram(verifierMessages)) {
          // Copy new app + all files in app_library
          FileUtils.moveAllFileToOtherDirectory(GENERATED_FOLDER_PATH, existingAppsLibrary.path)
          printTrace(verifierMessages)
          success(s"The apps have been successfully compiled!")
        } else {
          // New app is rejected
          printTrace(verifierMessages)
          printErrorAndExit("Compilation failed, see messages above")
        }
      case GenerateBindings =>
        info("Generating the bindings...")
        val etsProjPathString = config.etsProjectFile.get
        val newPhysicalStructure: PhysicalStructure = extractPhysicalStructure(etsProjPathString)
        compiler.Compiler.generateBindingsFiles(newAppsLibrary, existingAppsLibrary, newPhysicalStructure, existingPhysicalStructure)
        success(s"The bindings have been successfully created!")
      case GenerateApp =>
        info("Generating the app...")
        (config.appName, config.devicesPrototypicalStructureFile) match {
          case (Some(name), Some(devicesJson)) =>
            val nameRegex = "^_*[a-z]+[a-z_]*_*$".r
            if (nameRegex.matches(name))
              runPythonModule(APP_GENERATOR_PYTHON_MODULE, Seq(name, devicesJson), exitCode => s"The app generator failed with exit code $exitCode and above stdout")
            else printErrorAndExit("The app name has to contain only lowercase letters and underscores")
            success(s"The app '$name' has been successfully created!")
          case (None, Some(_)) =>
            printErrorAndExit("The app name has to be provided to generate a new app")
          case (Some(_), None) =>
            printErrorAndExit("The devices prototypical structure JSON file has to be provided to generate a new app")
          case _ =>
            printErrorAndExit("The devices prototypical structure JSON file and the app name have to be provided to generate a new app")
        }
      case ListApps =>
        info("Listing the apps...")
        val appNames = existingAppsLibrary.apps.map(_.name)
        if (appNames.isEmpty) warning("WARNING: There are no installed applications!")
        else success(s"The installed apps are: ${appNames.mkString(",")}")
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
          case verifierError: VerifierError     => printErrorAndExit(verifierError.msg)
          case verifierWarning: VerifierWarning => warning(s"WARNING: ${verifierWarning.msg}")
          case verifierInfo: VerifierInfo       => info(s"INFO: ${verifierInfo.msg}")
          case _                                => ()
        }
        printTrace(tl)
      }
      case Nil => ()
    }
  }

  private def printErrorAndExit(errorMessage: String)(implicit style: Style): Unit = {
    error(s"ERROR: $errorMessage")
    sys.exit(ERROR_CODE)
  }

  private def runPythonModule(module: String, args: Seq[String], errorMessageBuilder: Int => String)(implicit style: Style): Unit = {
    val (exitCode, _) = ProcRunner.callPython(module, os.Path(SVSHI_FOLDER), args: _*)
    if (exitCode != SUCCESS_CODE) printErrorAndExit(errorMessageBuilder(exitCode))
  }
}
