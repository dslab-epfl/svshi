package ch.epfl.web.service.main

import cask.{Request, Response, get, post}
import ch.epfl.web.service.main.session.floors.FloorListJson
import ch.epfl.web.service.main.session.{Session, SessionManager}
import ch.epfl.web.service.main.simulator.SimulatorInterface
import ch.epfl.web.service.main.svshi.SvshiInterface
import ch.epfl.web.service.main.utils.{Constants, FileUtils, Version}
import io.undertow.Undertow
import os.Path
import upickle.default._

import scala.util.{Failure, Success, Try}

case class ApiServer(
    svshiSimFactory: SvshiSimInterfaceFactory,
    override val host: String = Constants.WEB_SERVICE_DEFAULT_ADDRESS,
    override val port: Int = Constants.WEB_SERVICE_DEFAULT_PORT,
    debug: String => Unit = s => (),
    dockerMode: Boolean = false
) extends cask.MainRoutes {
  // Config of Cask
  override def verbose: Boolean = true
  private lazy val server = Undertow.builder.addHttpListener(port, host).setHandler(defaultHandler).build

  private val HEADERS_AJAX = Seq("Access-Control-Allow-Origin" -> "*")
  private val MALFORMED_ZIP_INPUT_FILE_MESSAGE = "The given zip is not a valid zip!"
  private def FLOOR_ADDED_MESSAGE(floorNumber: Int, floorName: String) = s"The floor with number $floorNumber and name $floorName was correctly added!"
  private def ERROR_GETTING_MAPPINGS(msg: String) = s"An internal error occurred while getting the devices mappings! Message: \n$msg"
  private def ERROR_WRONG_APP_FILES_ZIP_FORMAT_MESSAGE(errMsg: String) =
    f"The zip is in the wrong format! Please provide a zip with at the root either a folder containing the app's files or directly the app's files!\n$errMsg"
  private def APP_ADDED_MESSAGE(appName: String) = f"The app '$appName' was correctly added!"
  private def APP_REMOVED_MESSAGE(appName: String) = f"The app '$appName' was correctly removed!"
  private val ONE_FILE_IN_ZIP_ERROR = "Only one file should be in the zip!"
  private val ETS_FILE_UPDATED_MESSAGE = "The ETS project file was updated!"
  private val NO_ETS_FILE_SET_MESSAGE = "No ETS project file set for this session!"
  private val NO_SESSION_MESSAGE = "No session created! Please first call GET /sessionId"
  private val SESSION_RESET_MESSAGE = "The current session has been successfully erased!"
  private val BAD_REQUEST_CODE = 400
  private val SUCCESS_REQUEST_CODE = 200
  private val NOT_FOUND_ERROR_CODE = 404
  private val INTERNAL_ERROR_CODE = 500
  private val LOCKED_ERROR_CODE = 423
  private val NO_SESSION_ERROR_CODE = 403
  private val FAILED_PROCONDITION_ERROR_CODE = 412

  private def SVSHI_APPS_UNINSTALLED_MSG(msg: String) = f"All apps uninstalled on SVSHI!\n$msg"
  private def SVSHI_APPS_NOT_UNINSTALLED_MSG(msg: String) = f"Cannot uninstall apps on SVSHI!\n$msg"
  private def SVSHI_INSTALL_APPS_APP_NOT_IN_SESSION_ERROR_MSG(appName: String) = f"The app '$appName' is not in your session!"
  private val SVSHI_INSTALL_APPS_APP_NOT_IN_SESSION_ERROR_MSG = f"No apps are given, cannot install!"
  private def SVSHI_INSTALL_APPS_APP_GEN_BINDINGS_ERROR_MSG(msg: String) = f"Cannot generate the bindings! See SVSHI error:\n$msg"
  private def SVSHI_INSTALL_APPS_APP_COMPILE_ERROR_MSG(msg: String) = f"Cannot compile the applications! See SVSHI error:\n$msg"
  private def SVSHI_INSTALL_APPS_APP_COMPILE_SUCCESS_MSG(msg: String) = f"Apps installation is successful! See SVSHI output:\n$msg"
  private val applicationsBasicsFilesAndFoldersNames = List("models", "main.py", "files", "app_prototypical_structure.json")

  private val privateTempZipFilePath = Constants.TEMP_FOLDER_PATH / "zip_file.zip"

  val READ_REQUEST_TIMEOUT = 20_000 // Milliseconds

  /** Samuel Chassot - 14.04.2022
    * I override the main method of the parent class to be able to kill the server later on
    * which is not possible with the library's main method
    *
    * I create the server as a private lazy val and then offer a start and and a stop method to the caller
    *
    * original method is:
    * def main(args: Array[String]): Unit = {
    *    if (!verbose) Main.silenceJboss()
    *    val server = Undertow.builder
    *      .addHttpListener(port, host)
    *      .setHandler(defaultHandler)
    *      .build
    *    server.start()
    *  }
    * @param args
    */
  override def main(args: Array[String]): Unit = {
    // Does nothing to not interfere
  }

  def start(): Unit = server.start()

  def stop(): Unit = server.stop()

  @get("/version")
  def getVersion() = {
    val version = Version.getVersion()
    Response(version, headers = HEADERS_AJAX)
  }

  @get("/sessionId")
  def getCurrentSession() = {
    SessionManager.loadFromFileIfSessionExists()
    val session = SessionManager.getCurrentSession match {
      case Some(session) => session
      case None          => SessionManager.createNewSession()
    }
    Response(session.id, headers = HEADERS_AJAX)
  }

  @post("/reset/session")
  def resetCurrentSession() = {
    SessionManager.deleteCurrentSession()
    Response(SESSION_RESET_MESSAGE, headers = HEADERS_AJAX)
  }

//  @get("/session")
//  def getSession(request: cask.Request) = {
//    def createNewSession(): Response[String] = {
//      val newSession = SessionManager.createNewSession()
//      val cookie = generateCookieFromSession(newSession)
//      cask.Response("New session!", cookies = Seq(cookie), headers = HEADERS_AJAX)
//    }
//    val cookieOpt = request.cookies.get(Constants.SESSION_ID_COOKIE_NAME)
//    cookieOpt match {
//      case Some(cookie) => {
//        // Cookie passed -> check if session exists:
//        // - if yes, renew the cookie with a new validity date
//        // - if no, the cookie is invalid -> create a new session and return the new cookie
//        val sessionId = cookie.value
//        val sessionOpt = SessionManager.getSession(sessionId)
//        sessionOpt match {
//          case Some(session) => {
//            session.renewLifespan()
//            val cookie = generateCookieFromSession(session)
//            cask.Response("Session renewed!", cookies = Seq(cookie), headers = HEADERS_AJAX)
//          }
//          case None => {
//            createNewSession()
//          }
//        }
//      }
//      case None => {
//        // No cookie passed -> create a new session and respond with the new cookie
//        createNewSession()
//      }
//    }
//  }

  @post("/addFloor/:floorName/:floorNumber")
  def postImportFloors(floorName: String, floorNumber: Int, request: Request) = {
    getSessionAndInterfacesAndExecute(
      request,
      (svshiInterface, simulatorInterface, session) => {
        val zipOutputFolder = s"${session.id}-$floorNumber-$floorName"
        unzipContentAndExec(request)(
          Constants.TEMP_FOLDER_PATH / zipOutputFolder,
          p => {
            val files = FileUtils.recursiveListFiles(p)
            if (files.length != 1) {
              Response(ONE_FILE_IN_ZIP_ERROR, statusCode = BAD_REQUEST_CODE, headers = HEADERS_AJAX)
            } else {
              val file = files.head
              session.removeFloor(floorNumber)
              session.addFloor(floorNumber, floorName, file)
              os.remove.all(p)
              Response(FLOOR_ADDED_MESSAGE(floorNumber, floorName), headers = HEADERS_AJAX)
            }
          }
        )
      },
      NO_SESSION_MESSAGE
    )
  }

  @get("/floors")
  def getFloors(request: Request) = {
    getSessionAndInterfacesAndExecute(
      request,
      (svshiInterface, simulatorInterface, session) => {
        val floors = FloorListJson(session.getFloors().map(_.toJsonClass))
        Response(floors.toString, statusCode = SUCCESS_REQUEST_CODE, headers = HEADERS_AJAX)
      },
      NO_SESSION_MESSAGE
    )
  }

  @get("/floorFile/:number")
  def getFloorFile(number: Int, request: Request) = {
    getSessionAndInterfacesAndExecute(
      request,
      (svshiInterface, simulatorInterface, session) => {
        session.getFloor(number) match {
          case Some(floor) => Response(data = os.read.bytes(floor.blueprintFilePath), statusCode = SUCCESS_REQUEST_CODE, headers = HEADERS_AJAX)
          case None        => Response(data = Array[Byte](), statusCode = NOT_FOUND_ERROR_CODE, headers = HEADERS_AJAX)
        }

      },
      Array[Byte]()
    )
  }
  @post("/etsFile")
  def setEtsFile(request: Request) = {
    getSessionAndInterfacesAndExecute(
      request,
      (svshiInterface, simulatorInterface, session) => {
        val zipOutputFolder = s"etsFile-$session.id"
        unzipContentAndExec(request)(
          Constants.TEMP_FOLDER_PATH / zipOutputFolder,
          p => {
            val files = FileUtils.recursiveListFiles(p)
            if (files.length != 1) {
              Response(ONE_FILE_IN_ZIP_ERROR, statusCode = BAD_REQUEST_CODE, headers = HEADERS_AJAX)
            } else {
              val file = files.head
              session.setEtsFile(file)
              os.remove.all(p)
              Response(ETS_FILE_UPDATED_MESSAGE, headers = HEADERS_AJAX)
            }
          }
        )
      },
      NO_SESSION_MESSAGE
    )
  }

  @get("/etsFile")
  def getETSFile(request: Request) = {
    getSessionAndInterfacesAndExecute(
      request,
      (svshiInterface, simulatorInterface, session) => {
        if (session.hasEtsFile) {
          Response(data = os.read.bytes(session.etsFilePath), statusCode = SUCCESS_REQUEST_CODE, headers = HEADERS_AJAX)
        } else {
          Response(data = Array[Byte](), statusCode = NOT_FOUND_ERROR_CODE, headers = HEADERS_AJAX)
        }

      },
      Array[Byte]()
    )
  }

  @get("/getDeviceMappings")
  def getDeviceMappings(request: Request) = {
    getSessionAndInterfacesAndExecute(
      request,
      (svshiInterface, simulatorInterface, session) => {
        if (!session.hasEtsFile) {
          Response(NO_ETS_FILE_SET_MESSAGE, FAILED_PROCONDITION_ERROR_CODE, headers = HEADERS_AJAX)
        } else {
          Try(session.getOrComputeDeviceMappings(svshiInterface)) match {
            case Failure(exception) => Response(ERROR_GETTING_MAPPINGS(exception.getMessage), statusCode = INTERNAL_ERROR_CODE, headers = HEADERS_AJAX)
            case Success(mappings)  => Response(mappings, statusCode = SUCCESS_REQUEST_CODE, headers = HEADERS_AJAX)
          }

        }
      },
      NO_SESSION_MESSAGE
    )
  }

  @post("/generateApp/:appName")
  def generateApp(appName: String, request: Request) = {
    getSessionAndInterfacesAndExecute(
      request,
      (svshiInterface, simulatorInterface, session) => {
        val protoJsonString = request.text()
        Try(svshiInterface.generateApp(appName, protoJsonString)) match {
          case Failure(exception) => {
            debug(exception.getLocalizedMessage)
            Response(data = Array[Byte](), statusCode = INTERNAL_ERROR_CODE, headers = HEADERS_AJAX)
          }
          case Success(newAppZip) => Response(data = newAppZip, statusCode = SUCCESS_REQUEST_CODE, headers = HEADERS_AJAX)
        }
      },
      Array[Byte]()
    )
  }

  @get("/applications/names")
  def getApplicationsList(request: Request) = {
    getSessionAndInterfacesAndExecute(
      request,
      (svshiInterface, simulatorInterface, session) => {
        val appNames = session.appNamesList
        Response(write(appNames), statusCode = SUCCESS_REQUEST_CODE, headers = HEADERS_AJAX)
      },
      NO_SESSION_MESSAGE
    )
  }

  @get("/applications/files/:appName")
  def getAppFiles(appName: String, request: Request) = {
    getSessionAndInterfacesAndExecute(
      request,
      (svshiInterface, simulatorInterface, session) => {
        session.getZipForAppFiles(appName) match {
          case Some(value) => Response(data = value, statusCode = SUCCESS_REQUEST_CODE, headers = HEADERS_AJAX)
          case None        => Response(data = Array[Byte](), statusCode = NOT_FOUND_ERROR_CODE, headers = HEADERS_AJAX)
        }
      },
      Array[Byte]()
    )
  }

  @post("/applications/add/:appName")
  def postNewApp(appName: String, request: Request) = {
    getSessionAndInterfacesAndExecute(
      request,
      (svshiInterface, simulatorInterface, session) => {
        val zipOutputFolderName = s"app_${appName}_temp_unzip"
        unzipContentAndExec(request)(
          Constants.TEMP_FOLDER_PATH / zipOutputFolderName,
          p => {
            // Here we support 2 cases:
            // 1) the user compressed the app with a folder containing the app at the root
            // 2) the user compressed the app with its files at the root
            val rootFilesFolders = os.list(p)

            if (rootFilesFolders.length == 1 && os.isDir(rootFilesFolders.head)) {
              // case 1:
              val appFolder = rootFilesFolders.head
              val appFiles = FileUtils.recursiveListFiles(appFolder)
              val valididyTuple = checkAppFilesValidity(appFiles)
              if (!valididyTuple._1) {
                Response(data = ERROR_WRONG_APP_FILES_ZIP_FORMAT_MESSAGE(valididyTuple._2), statusCode = BAD_REQUEST_CODE, headers = HEADERS_AJAX)
              } else {
                session.addApp(appName, appFolder)
                Response(data = APP_ADDED_MESSAGE(appName), statusCode = SUCCESS_REQUEST_CODE, headers = HEADERS_AJAX)
              }
            } else {
              // case 2:
              val files = FileUtils.recursiveListFiles(p)
              val valididyTuple = checkAppFilesValidity(files)
              if (!valididyTuple._1) {
                Response(data = ERROR_WRONG_APP_FILES_ZIP_FORMAT_MESSAGE(valididyTuple._2), statusCode = BAD_REQUEST_CODE, headers = HEADERS_AJAX)
              } else {
                session.addApp(appName, p)
                Response(data = APP_ADDED_MESSAGE(appName), statusCode = SUCCESS_REQUEST_CODE, headers = HEADERS_AJAX)
              }
            }
          }
        )
      },
      NO_SESSION_MESSAGE
    )
  }

  @post("/applications/delete/:appName")
  def postDeleteApp(appName: String, request: Request) = {
    getSessionAndInterfacesAndExecute(
      request,
      (svshiInterface, simulatorInterface, session) => {
        session.removeApp(appName)
        Response(APP_REMOVED_MESSAGE(appName), statusCode = SUCCESS_REQUEST_CODE, headers = HEADERS_AJAX)
      },
      NO_SESSION_MESSAGE
    )
  }

  @get("/svshi/applications/installed/names")
  def getSvshiInstalledAppNames(request: Request) = {
    getSessionAndInterfacesAndExecute(
      request,
      (svshiInterface, simulatorInterface, session) => {
        Try(write(svshiInterface.getInstalledAppNames())) match {
          case Failure(exception) => Response(exception.getLocalizedMessage, statusCode = INTERNAL_ERROR_CODE, headers = HEADERS_AJAX)
          case Success(value)     => Response(data = value, statusCode = SUCCESS_REQUEST_CODE, headers = HEADERS_AJAX)
        }

      },
      NO_SESSION_MESSAGE
    )
  }

  @post("/svshi/applications/uninstall")
  def svshiUninstallApps(request: Request) = {
    getSessionAndInterfacesAndExecute(
      request,
      (svshiInterface, simulatorInterface, session) => {
        val (res, output) = svshiInterface.uninstallAllApps()
        if (res) {
          Response(SVSHI_APPS_UNINSTALLED_MSG(output.mkString("\n")), statusCode = SUCCESS_REQUEST_CODE, headers = HEADERS_AJAX)

        } else {
          Response(SVSHI_APPS_NOT_UNINSTALLED_MSG(output.mkString("\n")), statusCode = INTERNAL_ERROR_CODE, headers = HEADERS_AJAX)
        }
      },
      NO_SESSION_MESSAGE
    )
  }

  @post("/svshi/applications/install")
  def svshiInstallApps(request: Request): Response[String] = {
    getSessionAndInterfacesAndExecute[String](
      request,
      (svshiInterface, simulatorInterface, session) => {
        val appList = read[List[String]](request.text())
        if (appList.isEmpty) {
          return Response(SVSHI_INSTALL_APPS_APP_NOT_IN_SESSION_ERROR_MSG, statusCode = BAD_REQUEST_CODE, headers = HEADERS_AJAX)
        }
        val optNotPresent = appList.find(n => !session.appNamesList.contains(n))
        if (optNotPresent.isDefined) {
          return Response(SVSHI_INSTALL_APPS_APP_NOT_IN_SESSION_ERROR_MSG(optNotPresent.get), statusCode = BAD_REQUEST_CODE, headers = HEADERS_AJAX)
        }

        // 1) Remove everything
        val (uninstallSuccessful, uninstallOutput) = svshiInterface.uninstallAllApps()
        if (!uninstallSuccessful) {
          return Response(uninstallOutput.mkString("\n"), statusCode = INTERNAL_ERROR_CODE, headers = HEADERS_AJAX)
        }
        svshiInterface.removeAllFromGenerated()

        // 2) Put all apps files in generated
        appList.foreach(appName => svshiInterface.addToGenerated(session.getZipForAppFiles(appName).get))

        // 3) Generate bindings
        val tryGenB = Try(svshiInterface.generateBindings(session.getEtsFileZip().get))
        tryGenB match {
          case Failure(exception) =>
            return Response(SVSHI_INSTALL_APPS_APP_GEN_BINDINGS_ERROR_MSG(exception.getLocalizedMessage), statusCode = INTERNAL_ERROR_CODE, headers = HEADERS_AJAX)
          case Success(_) => ()
        }

        val (genBindingsSuccessful, genBindingsOutput) = tryGenB.get
        if (!genBindingsSuccessful) {
          return Response(SVSHI_INSTALL_APPS_APP_GEN_BINDINGS_ERROR_MSG(genBindingsOutput.mkString("\n")), statusCode = INTERNAL_ERROR_CODE, headers = HEADERS_AJAX)
        }

        // 4) Compile
        val tryCompile = Try(svshiInterface.compile(session.getEtsFileZip().get))
        tryCompile match {
          case Failure(exception) =>
            return Response(SVSHI_INSTALL_APPS_APP_COMPILE_ERROR_MSG(exception.getLocalizedMessage), statusCode = INTERNAL_ERROR_CODE, headers = HEADERS_AJAX)
          case Success(_) => ()
        }
        val (compilationSuccessful, compilationOutput) = tryCompile.get
        val compilationOutStr = compilationOutput.mkString("\n")
        if (!compilationSuccessful) {
          return Response(SVSHI_INSTALL_APPS_APP_COMPILE_ERROR_MSG(compilationOutStr), statusCode = INTERNAL_ERROR_CODE, headers = HEADERS_AJAX)
        }
        return Response(SVSHI_INSTALL_APPS_APP_COMPILE_SUCCESS_MSG(compilationOutStr), statusCode = SUCCESS_REQUEST_CODE, headers = HEADERS_AJAX)
      },
      NO_SESSION_MESSAGE
    )
  }

  @post("/simulation/start")
  def startSimulation(request: Request) = {
    getSessionAndInterfacesAndExecute(
      request,
      (svshiInterface, simulatorInterface, session) => {
        debug("received post /simulation/start")
        if (simulatorInterface.isRunning()) {
          simulatorInterface.stop()()
        }
        if (svshiInterface.getRunStatus()) {
          Response(
            s"Cannot start simulation, the simulator or svshi is running! See:\nsvshi is running = ${svshiInterface.getRunStatus()}\nsimulator is running = ${simulatorInterface.isRunning()}",
            statusCode = BAD_REQUEST_CODE,
            headers = HEADERS_AJAX
          )
        } else {
          Try({
            val physicalStructureJson = session.getOrComputePhysicalStructureJson(svshiInterface)
            val appBindings = svshiInterface.getAppLibraryBindings()
            val gaToPhysId = svshiInterface.getAssignmentGaToPhysId()
            simulatorInterface.startSimulate(physicalStructureJson = physicalStructureJson, appBindingsJson = appBindings, groupAddressToPhysId = gaToPhysId)(debug)
            svshiInterface.run(simulatorInterface.getIpAddr(), session.SIMULATOR_KNX_PORT)(debug)

          }) match {
            case Failure(exception) =>
              Response(s"Cannot start simulation, an error occurred! See:\n${exception.getLocalizedMessage}", statusCode = INTERNAL_ERROR_CODE, headers = HEADERS_AJAX)
            case Success(_) => Response("Simulation started!", statusCode = SUCCESS_REQUEST_CODE, headers = HEADERS_AJAX)
          }

        }
      },
      NO_SESSION_MESSAGE
    )
  }

  @post("/simulation/stop")
  def stopSimulation(request: Request) = {
    getSessionAndInterfacesAndExecute(
      request,
      (svshiInterface, simulatorInterface, session) => {
        val svshiStopMsg = Try(svshiInterface.stop()) match {
          case Failure(exception) => exception.getLocalizedMessage
          case Success(_)         => ""
        }
        val simulatorStopMsg = Try(simulatorInterface.stop()(debug)) match {
          case Failure(exception) => exception.getLocalizedMessage
          case Success(_)         => ""
        }
        val svshiStopped = svshiStopMsg == ""
        val simulatorStopped = simulatorStopMsg == ""
        val mapResult: Map[String, String] =
          Map(("svshiStopped" -> s"$svshiStopped"), ("simulatorStopped" -> s"$simulatorStopped"), ("svshiMessage" -> svshiStopMsg), ("simulatorMessage" -> simulatorStopMsg))
        Response(write(mapResult, indent = 2), statusCode = SUCCESS_REQUEST_CODE, headers = HEADERS_AJAX)
      },
      NO_SESSION_MESSAGE
    )
  }

  @get("/svshi/runStatus/")
  def getSvshiRunStatus(request: Request): Response[String] = {
    getSessionAndInterfacesAndExecute(
      request,
      (svshiInterface, simulatorInterface, session) => {
        Response(write(Map("status" -> svshiInterface.getRunStatus()), indent = 2), statusCode = SUCCESS_REQUEST_CODE, headers = HEADERS_AJAX)
      },
      NO_SESSION_MESSAGE
    )
  }

  @get("/svshi/logs/run")
  def getSvshiLogsRun(request: Request) = {
    getSessionAndInterfacesAndExecute(
      request,
      (svshiInterface, simulatorInterface, session) => {
        Response(write(Map("lines" -> svshiInterface.getRunLogs()), indent = 2), statusCode = SUCCESS_REQUEST_CODE, headers = HEADERS_AJAX)
      },
      NO_SESSION_MESSAGE
    )
  }

  @get("/svshi/logs/receivedTelegrams")
  def getSvshiLogsReceivedTelegrams(request: Request) = {
    getSessionAndInterfacesAndExecute(
      request,
      (svshiInterface, simulatorInterface, session) => {
        Response(write(Map("lines" -> svshiInterface.getReceivedTelegramsLogs()), indent = 2), statusCode = SUCCESS_REQUEST_CODE, headers = HEADERS_AJAX)
      },
      NO_SESSION_MESSAGE
    )
  }

  @get("/svshi/logs/execution")
  def getSvshiLogsExecution(request: Request) = {
    getSessionAndInterfacesAndExecute(
      request,
      (svshiInterface, simulatorInterface, session) => {
        Response(write(Map("lines" -> svshiInterface.getExecutionLogs()), indent = 2), statusCode = SUCCESS_REQUEST_CODE, headers = HEADERS_AJAX)
      },
      NO_SESSION_MESSAGE
    )
  }

  @get("/simulator/runStatus")
  def getSimulatorRunStatus(request: Request) = {
    getSessionAndInterfacesAndExecute(
      request,
      (svshiInterface, simulatorInterface, session) => {
        Response(write(Map("status" -> simulatorInterface.isRunning()), indent = 2), statusCode = SUCCESS_REQUEST_CODE, headers = HEADERS_AJAX)
      },
      NO_SESSION_MESSAGE
    )
  }

  private def checkAppFilesValidity(files: List[os.Path]): (Boolean, String) = {
    this.applicationsBasicsFilesAndFoldersNames.foreach(fName => {
      if (!files.exists(p => p.segments.toList.last == fName)) {
        return (false, f"'$fName' is missing from the application files!\nContent was: $files")
      }
    })

    (true, "")
  }

  private def unzipContentAndExec(request: Request)(outputPath: Path, withUnzipped: Path => Response[String]): Response[String] = {
    val receivedData = request.data.readAllBytes()
    FileUtils.writeToFileOverwrite(privateTempZipFilePath, receivedData)
    val outputPathOpt = FileUtils.unzip(privateTempZipFilePath, outputPath)
    outputPathOpt match {
      case Some(path) => withUnzipped(path)
      case None       =>
        // an error occurred while unzipping, the zip file is probably malformed
        Response(MALFORMED_ZIP_INPUT_FILE_MESSAGE, statusCode = BAD_REQUEST_CODE, headers = HEADERS_AJAX)
    }
  }

  private def getSessionAndInterfacesAndExecute[T](request: Request, operation: (SvshiInterface, SimulatorInterface, Session) => Response[T], errorContent: T): Response[T] = {
    SessionManager.getCurrentSession match {
      case Some(session) => {
        val svshiInterface = svshiSimFactory.getSvshiHttpInterface(session.getSvshiAddr(dockerMode), session.SVSHI_PORT)
        val simulatorInterface = svshiSimFactory.getSimulatorHttpInterface(session.getSimulatorAddr(dockerMode), session.SIMULATOR_PORT)
        operation(svshiInterface, simulatorInterface, session)
      }
      case None => {
        Response(errorContent, statusCode = NO_SESSION_ERROR_CODE, headers = HEADERS_AJAX)
      }
    }
  }

  initialize()

}
