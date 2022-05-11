package ch.epfl.core

import ch.epfl.core.Svshi.{ERROR_CODE, SUCCESS_CODE}
import ch.epfl.core.api.server.CoreApiServer
import ch.epfl.core.model.physical.PhysicalStructure
import ch.epfl.core.parser.ets.EtsParser
import ch.epfl.core.parser.json.physical.PhysicalStructureJsonParser
import ch.epfl.core.utils.Cli._
import ch.epfl.core.utils.Constants._
import ch.epfl.core.utils.Printer._
import ch.epfl.core.utils.Utils.loadApplicationsLibrary
import ch.epfl.core.utils.style.{ColorsStyle, NoColorsStyle}
import ch.epfl.core.utils.{Constants, Style, Utils}
import mainargs.ParserForClass

import java.io.File
import scala.util.{Failure, Success, Try}

object Main {

  private val CLI_TOTAL_WIDTH = 200

  private var systemExit: SystemExit = DefaultSystemExit

  var coreApiServer: Option[CoreApiServer] = None

  def main(args: Array[String]): Unit = {
    val config = ParserForClass[Config].constructOrExit(args, totalWidth = CLI_TOTAL_WIDTH)
    implicit val style: Style = if (config.noColors.value) NoColorsStyle else ColorsStyle

    // Check if app_library folder exists and if not, create it
    if (!os.exists(APP_LIBRARY_FOLDER_PATH)) os.makeDir.all(APP_LIBRARY_FOLDER_PATH)

    // Check if generated folder exists and if not, create it
    if (!os.exists(GENERATED_FOLDER_PATH)) os.makeDir.all(GENERATED_FOLDER_PATH)

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
      if (!os.exists(etsProjPath)) {
        printErrorAndExit("The ETS Project file does not exist!")
      }
      val newPhysicalStructure = EtsParser.parseEtsProjectFile(etsProjPath)
      newPhysicalStructure
    }

    val appNameOpt = config.appName
    config.task match {
      case Gui =>
        val addrPort = config.knxAddress
        val (address, port) =
          if (addrPort.isDefined) {
            val addrPortStr = addrPort.get
            if (Utils.validAddressPortString(addrPortStr)) {
              val addrPortList = addrPort.get.split(":")
              if (addrPortList.length != 2) {
                printErrorAndExit(f"Malformed address:port args: $addrPortStr")
                // The printErrorAndExit call exists anyway ...
              }
              (addrPortList.head, addrPortList.last.toInt)
            } else {
              printErrorAndExit(f"Malformed address:port args: $addrPortStr")
              ("", 0)
            }
          } else {
            (Constants.SVSHI_GUI_SERVER_DEFAULT_HOST, Constants.SVSHI_GUI_SERVER_DEFAULT_PORT)
          }
        coreApiServer = Some(CoreApiServer(debug = debug, host = address, port = port))
        coreApiServer.get.start()
      case GetVersion =>
        Svshi.getVersion(success = info)
      case Run =>
        if (
          Svshi
            .run(knxAddress = config.knxAddress, existingAppsLibrary = existingAppsLibrary, blocking = true)(success = success, info = info, err = error)
            .exitCode
            .get != SUCCESS_CODE
        ) {
          printErrorAndExit("Exiting...")
        }
      case Compile | GenerateBindings if config.etsProjectFile.isEmpty =>
        printErrorAndExit("The ETS project file needs to be specified to compile or to generate the bindings")
      case Compile =>
        val etsProjectFile = config.etsProjectFile.get
        if (!new File(etsProjectFile).isAbsolute) printErrorAndExit("The ETS project file name has to be absolute")
        else {
          if (
            Svshi.compileApps(existingAppsLibrary, newAppsLibrary, extractPhysicalStructure(etsProjectFile))(
              success = success,
              info = info,
              warning = warning,
              err = error
            ) != SUCCESS_CODE
          ) {
            printErrorAndExit("Exiting...")
          }
        }
      case GenerateBindings =>
        val etsProjectFile = config.etsProjectFile.get
        if (!new File(etsProjectFile).isAbsolute) printErrorAndExit("The ETS project file name has to be absolute")
        else {
          if (
            Svshi.generateBindings(existingAppsLibrary, newAppsLibrary, existingPhysicalStructure, extractPhysicalStructure(etsProjectFile))(
              success = success,
              info = info,
              warning = warning,
              err = error
            ) != SUCCESS_CODE
          ) {
            printErrorAndExit("Exiting...")
          }
        }
      case GenerateApp =>
        if (Svshi.generateApp(appNameOpt, config.devicesPrototypicalStructureFile)(success = success, info = info, err = error) != SUCCESS_CODE) {
          printErrorAndExit("Exiting...")
        }

      case UpdateApp =>
        if (appNameOpt.isEmpty) {
          printErrorAndExit("The app name has to be provided to update an app")
        }
        val appName = appNameOpt.get
        if (
          Svshi.updateApp(existingAppsLibrary, newAppsLibrary, appName, existingPhysicalStructure)(success = success, info = info, warning = warning, err = error) != SUCCESS_CODE
        ) {
          printErrorAndExit("Exiting...")
        }
      case RemoveApp =>
        val allAppsFlag = config.all.value
        val answer = if (allAppsFlag) {
          scala.io.StdIn.readLine("All the apps will be removed. Are you sure [y/n]? ")
        } else {
          if (appNameOpt.isEmpty) {
            printErrorAndExit("The app name has to be provided to remove an app")
          }
          scala.io.StdIn.readLine(s"The app '${appNameOpt.get}' will be removed. Are you sure [y/n]? ")
        }

        if (!(answer != null && (answer.toLowerCase == "y" || answer.toLowerCase == "yes"))) {
          info("Exiting...")
        } else if (Svshi.removeApps(allAppsFlag, appNameOpt, existingAppsLibrary)(success = success, info = info, warning = warning, err = error) != SUCCESS_CODE) {
          printErrorAndExit("Exiting...")
        }
      case ListApps => {
        info("Listing the apps...")
        val installedAppNames = Svshi.listApps(existingAppsLibrary)
        if (installedAppNames.isEmpty) warning("There are no installed applications!")
        else {
          val namesString = installedAppNames.map(s => f"'$s'").mkString(", ")
          success(s"The installed apps are: $namesString")
        }
      }

    }
  }

  def setSystemExit(newSystemExit: SystemExit): Unit = systemExit = newSystemExit

  private def printErrorAndExit(errorMessage: String)(implicit style: Style): Unit = {
    error(errorMessage)
    systemExit.exit(ERROR_CODE)
  }

}

trait SystemExit {
  def exit(errorCode: Int): Unit
}

object DefaultSystemExit extends SystemExit {
  override def exit(errorCode: Int): Unit = sys.exit(errorCode)
}
