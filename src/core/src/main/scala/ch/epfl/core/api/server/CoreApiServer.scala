package ch.epfl.core.api.server

import cask.{Request, Response, get, post}
import ch.epfl.core.Svshi.runtimeModuleApplicationFilesPath
import ch.epfl.core.api.server.json.{BindingsResponse, ResponseBody}
import ch.epfl.core.model.application.ApplicationLibrary
import ch.epfl.core.model.physical.PhysicalStructure
import ch.epfl.core.parser.ets.EtsParser
import ch.epfl.core.parser.json.bindings.BindingsJsonParser
import ch.epfl.core.parser.json.physical.PhysicalStructureJsonParser
import ch.epfl.core.parser.json.prototype.AppInputJsonParser
import ch.epfl.core.utils.Constants._
import ch.epfl.core.utils.Utils.loadApplicationsLibrary
import ch.epfl.core.utils.{Constants, FileUtils}
import ch.epfl.core.{Svshi, SvshiRunResult, SvshiTr}
import io.undertow.Undertow
import os.Path

import java.util.concurrent.locks.{Lock, ReentrantLock}
import scala.util.{Failure, Success, Try}

case class CoreApiServer(
    svshiSystem: SvshiTr = Svshi,
    debug: String => Unit = _ => (),
    override val host: String = "localhost",
    override val port: Int = Constants.SVSHI_GUI_SERVER_DEFAULT_PORT
) extends cask.MainRoutes {
  // Config of Cask
  override def verbose: Boolean = true
  private lazy val server = Undertow.builder.addHttpListener(port, host).setHandler(defaultHandler).build

  private val BAD_REQUEST_CODE = 400
  private val SUCCESS_REQUEST_CODE = 200
  private val NOT_FOUND_ERROR_CODE = 404
  private val INTERNAL_ERROR_CODE = 500
  private val LOCKED_ERROR_CODE = 423

  private val localRunLogFileName = "private_run_logs.log"
  private val localRunLogFilePath = Constants.PRIVATE_SERVER_LOG_FOLDER_PATH / localRunLogFileName

  private val knxprojPrivateFolderName = "private_ets_project"
  private val privateTempZipFileName = "temp_file.zip"
  private val privateTempFolderName = "temp_api_server"
  private val knxprojPrivateFolderPath = Constants.PRIVATE_INPUT_FOLDER_PATH / knxprojPrivateFolderName
  private val privateTempZipFilePath = Constants.PRIVATE_INPUT_FOLDER_PATH / privateTempZipFileName
  private val privateTempFolderPath = Constants.PRIVATE_INPUT_FOLDER_PATH / privateTempFolderName

  private val appsBindingsGeneratedFilePath = Constants.GENERATED_FOLDER_PATH / Constants.APP_PROTO_BINDINGS_JSON_FILE_NAME
  private val physicalStructureJsonGeneratedFilePath = Constants.GENERATED_FOLDER_PATH / Constants.PHYSICAL_STRUCTURE_JSON_FILE_NAME

  private val infoPrefix = "INFO"
  private val warningPrefix = "WARNING"
  private val errorPrefix = "ERROR"

  private val RUN_CALLED_MESSAGE = "SVSHI run was initiated!"
  private val RUN_STOPPED_MESSAGE = "SVSHI run was stopped!"
  private val MALFORMED_ZIP_INPUT_FILE_MESSAGE = "An error occurred when unzipping the file provided in the request. Please check the file is well formed and in zip format."
  private val BINDINGS_NOT_FOUND_MESSAGE = "The bindings are not generated yet, please generate them before trying to get!"
  private val GENERATED_FOLDER_DELETED_MESSAGE = "The generated folder was successfully emptied!"
  private val REQUESTED_APP_NOT_INSTALLED_MESSAGE = "The requested app is not installed!"
  private val ZIPPING_ERROR_MESSAGE = "An error occurred while zipping the content!"
  private val LOCKED_ERROR_MESSAGE = "An operation is already running on SVSHI, please retry later!"
  private val knxprojExtension = "knxproj"
  private val jsonExtension = "json"
  private val WRONG_EXTENSION_FILE_ERROR_MESSAGE = "The file for the physical structure must be either a " + jsonExtension + " or a " + knxprojExtension + " file!"

  private val HEADERS_AJAX = Seq("Access-Control-Allow-Origin" -> "*")

  private var svshiRunResult: Option[SvshiRunResult] = None
  private def runStateRunning: Boolean = svshiRunResult.isDefined && svshiRunResult.get.isAlive

  val MAX_LOG_LINES_SENT = 10000

  private val lock: Lock = new ReentrantLock()

  private val defaultResponseStringIfLocked = Response(data = LOCKED_ERROR_MESSAGE, statusCode = LOCKED_ERROR_CODE, headers = HEADERS_AJAX)
  private val defaultResponseBytesIfLocked: Response[Array[Byte]] = Response(data = Array(), statusCode = LOCKED_ERROR_CODE, headers = HEADERS_AJAX)

  /** Load the existingAppLibrary, newAppsLibrary and existingPhysicalStructure from file system
    * @return (existingAppLibrary, newAppsLibrary, existingPhysicalStructure)
    */
  private def loadCurrentSVSHIState: (ApplicationLibrary, ApplicationLibrary, PhysicalStructure) = {
    val existingAppsLibrary = loadApplicationsLibrary(APP_LIBRARY_FOLDER_PATH)
    val newAppsLibrary = loadApplicationsLibrary(GENERATED_FOLDER_PATH)
    val existingPhysStructPath = existingAppsLibrary.path / PHYSICAL_STRUCTURE_JSON_FILE_NAME
    val existingPhysicalStructure = if (os.exists(existingPhysStructPath)) PhysicalStructureJsonParser.parse(existingPhysStructPath) else PhysicalStructure(Nil)
    (existingAppsLibrary, newAppsLibrary, existingPhysicalStructure)
  }

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

  def forceKillSvshiRun(): Unit = if (svshiRunResult.isDefined) {
    svshiRunResult.get.forceStop()
    svshiRunResult = None
    // Clear all files used by apps
    os.remove.all(runtimeModuleApplicationFilesPath)
  }

  @get("/")
  def welcomeRoot() = Response(data = "API server for SVSHI interface\n", headers = HEADERS_AJAX)

  @get("/version")
  def getVersion() = {
    var output: List[String] = List()
    val resCode = svshiSystem.getVersion(s => output = output ::: List(s))
    Response(data = ResponseBody(resCode == Svshi.SUCCESS_CODE, output).toString, headers = HEADERS_AJAX)
  }

  @get("/listApps")
  def listApps() = {
    val (existingAppsLibrary, _, _) = loadCurrentSVSHIState
    val installedApps = svshiSystem.listApps(existingAppsLibrary)
    Response(ResponseBody(status = true, installedApps).toString, headers = HEADERS_AJAX)
  }

  @get("/availableProtoDevices")
  def getAvailableProtoDevices() = {
    val availableDevices = svshiSystem.getAvailableProtoDevices()
    Response(ResponseBody(status = true, availableDevices).toString, headers = HEADERS_AJAX)
  }

  @get("/availableDpts")
  def getAvailableDpts() = {
    val availableDpts = svshiSystem.getAvailableDpts()
    Response(ResponseBody(status = true, availableDpts).toString, headers = HEADERS_AJAX)
  }

  @post("/generateApp/:appName")
  def generateApp(appName: String, request: Request) = acquireLockAndExecute(
    () => {
      cleanTempFolders()
      val protoJsonString = request.text()
      debug(f"Received generateNewApp for $appName with " + jsonExtension + f" = $protoJsonString")
      Try(AppInputJsonParser.parseJson(protoJsonString)) match {
        case Failure(exception) => Response(exception.toString, statusCode = BAD_REQUEST_CODE)
        case Success(protoJson) => {
          var output: List[String] = List()
          val protoJsonFile = Constants.PRIVATE_INPUT_FOLDER_PATH / Constants.APP_PROTO_STRUCT_FILE_NAME
          AppInputJsonParser.writeToFile(protoJsonFile, protoJson)
          def outputFunc(s: String, prefix: String): Unit = {
            debug(createStrMessagePrefix(s, prefix))
            output = output ::: List(createStrMessagePrefix(s, prefix))
          }
          val resCode = svshiSystem.generateApp(appName = Some(appName), Some(protoJsonFile.toString()))(
            success = s => outputFunc(s, ""),
            info = s => outputFunc(s, infoPrefix),
            warning = s => outputFunc(s, warningPrefix),
            err = s => outputFunc(s, errorPrefix)
          )
          Response(ResponseBody(resCode == Svshi.SUCCESS_CODE, output).toString, headers = HEADERS_AJAX)
        }
      }
    },
    defaultResponseStringIfLocked
  )

  @post("/compile")
  def compile(request: Request) = acquireLockAndExecute(
    () => {
      debug("Received a compile request")
      cleanTempFolders()
      val (existingAppsLibrary, newAppsLibrary, _) = loadCurrentSVSHIState
      extractKnxprojOrJsonFileAndExec(request)(newPhysicalStructure => {
        var output: List[String] = List()
        def outputFunc(s: String, prefix: String): Unit = {
          debug(createStrMessagePrefix(s, prefix))
          output = output ::: List(createStrMessagePrefix(s, prefix))
        }
        val resCode = svshiSystem.compileApps(existingAppsLibrary = existingAppsLibrary, newAppsLibrary = newAppsLibrary, newPhysicalStructure = newPhysicalStructure)(
          success = s => outputFunc(s, ""),
          info = s => outputFunc(s, infoPrefix),
          warning = s => outputFunc(s, warningPrefix),
          err = s => outputFunc(s, errorPrefix)
        )
        Response(ResponseBody(resCode == Svshi.SUCCESS_CODE, output).toString, headers = HEADERS_AJAX)
      })
    },
    defaultResponseStringIfLocked
  )

  @post("/updateApp/:appName")
  def updateApp(appName: String) = acquireLockAndExecute(
    () => {
      debug(f"Received an update app request for app '$appName'")
      cleanTempFolders()
      val (existingAppsLibrary, newAppsLibrary, existingPhysStruct) = loadCurrentSVSHIState
      if (existingAppsLibrary.apps.exists(a => a.name == appName)) {
        var output: List[String] = List()
        def outputFunc(s: String, prefix: String): Unit = {
          debug(createStrMessagePrefix(s, prefix))
          output = output ::: List(createStrMessagePrefix(s, prefix))
        }
        val resCode =
          svshiSystem.updateApp(
            existingAppsLibrary = existingAppsLibrary,
            newAppsLibrary = newAppsLibrary,
            appToUpdateName = appName,
            existingPhysicalStructure = existingPhysStruct
          )(
            success = s => outputFunc(s, ""),
            info = s => outputFunc(s, infoPrefix),
            warning = s => outputFunc(s, warningPrefix),
            err = s => outputFunc(s, errorPrefix)
          )
        Response(ResponseBody(status = resCode == Svshi.SUCCESS_CODE, output).toString, headers = HEADERS_AJAX)
      } else {
        Response(REQUESTED_APP_NOT_INSTALLED_MESSAGE, statusCode = NOT_FOUND_ERROR_CODE, headers = HEADERS_AJAX)
      }
    },
    defaultResponseStringIfLocked
  )

  @post("/generateBindings")
  def generateBindings(request: Request) = acquireLockAndExecute(
    () => {
      cleanTempFolders()
      val (existingAppsLibrary, newAppsLibrary, existingPhysicalStructure) = loadCurrentSVSHIState
      extractKnxprojOrJsonFileAndExec(request)(newPhysicalStructure => {
        var output: List[String] = List()
        def outputFunc(s: String, prefix: String): Unit = {
          debug(createStrMessagePrefix(s, prefix))
          output = output ::: List(createStrMessagePrefix(s, prefix))
        }
        val resCode = svshiSystem.generateBindings(
          existingAppsLibrary = existingAppsLibrary,
          newAppsLibrary = newAppsLibrary,
          existingPhysicalStructure = existingPhysicalStructure,
          newPhysicalStructure = newPhysicalStructure
        )(
          success = s => outputFunc(s, ""),
          info = s => outputFunc(s, infoPrefix),
          warning = s => outputFunc(s, warningPrefix),
          err = s => outputFunc(s, errorPrefix)
        )
        Response(ResponseBody(resCode == Svshi.SUCCESS_CODE, output).toString, headers = HEADERS_AJAX)
      })
    },
    defaultResponseStringIfLocked
  )

  @post("/removeApp/:appName")
  def removeApp(appName: String) = acquireLockAndExecute(
    () => {
      cleanTempFolders()
      val (existingAppsLibrary, _, _) = loadCurrentSVSHIState
      var output: List[String] = List()
      def outputFunc(s: String, prefix: String): Unit = {
        debug(createStrMessagePrefix(s, prefix))
        output = output ::: List(createStrMessagePrefix(s, prefix))
      }
      val resCode = svshiSystem.removeApps(allFlag = false, appName = Some(appName), existingAppsLibrary = existingAppsLibrary)(
        success = s => outputFunc(s, ""),
        info = s => outputFunc(s, infoPrefix),
        warning = s => outputFunc(s, warningPrefix),
        err = s => outputFunc(s, errorPrefix)
      )
      Response(ResponseBody(resCode == Svshi.SUCCESS_CODE, output).toString, headers = HEADERS_AJAX)
    },
    defaultResponseStringIfLocked
  )

  @post("/removeAllApps")
  def removeAllApps() = acquireLockAndExecute(
    () => {
      cleanTempFolders()
      val (existingAppsLibrary, _, _) = loadCurrentSVSHIState
      var output: List[String] = List()
      def outputFunc(s: String, prefix: String): Unit = {
        debug(createStrMessagePrefix(s, prefix))
        output = output ::: List(createStrMessagePrefix(s, prefix))
      }
      val resCode = svshiSystem.removeApps(allFlag = true, appName = None, existingAppsLibrary = existingAppsLibrary)(
        success = s => outputFunc(s, ""),
        info = s => outputFunc(s, infoPrefix),
        warning = s => outputFunc(s, warningPrefix),
        err = s => outputFunc(s, errorPrefix)
      )
      Response(ResponseBody(resCode == Svshi.SUCCESS_CODE, output).toString, headers = HEADERS_AJAX)
    },
    defaultResponseStringIfLocked
  )

  @post("/run/:ipPort")
  def run(ipPort: String) = acquireLockAndExecute(
    () => {
      cleanTempFolders()
      val (existingAppsLibrary, _, _) = loadCurrentSVSHIState

      def successLog(s: String): Unit = Logger.log(file = localRunLogFilePath, prefix = "", message = s)
      def infoLog(s: String): Unit = Logger.log(file = localRunLogFilePath, prefix = infoPrefix, message = s)
      def warningLog(s: String): Unit = Logger.log(file = localRunLogFilePath, prefix = warningPrefix, message = s)
      def errorLog(s: String): Unit = Logger.log(file = localRunLogFilePath, prefix = errorPrefix, message = s)

      // Reset the log file
      FileUtils.deleteIfExists(localRunLogFilePath)

      svshiRunResult = Some(
        svshiSystem.run(knxAddress = Some(ipPort), existingAppsLibrary = existingAppsLibrary, blocking = false)(
          success = successLog,
          info = infoLog,
          warning = warningLog,
          err = errorLog
        )
      )

      Response(ResponseBody(status = true, List(RUN_CALLED_MESSAGE)).toString, headers = HEADERS_AJAX)
    },
    defaultResponseStringIfLocked
  )

  @get("/runStatus")
  def getRunStatus() = {
    Response(ResponseBody(status = runStateRunning, output = Nil).toString, headers = HEADERS_AJAX)
  }

  @get("/logs/run")
  def getRunLogs() = {
    if (os.exists(localRunLogFilePath)) {
      val logContentLines = os.read.lines(localRunLogFilePath).toList
      Response(ResponseBody(status = true, logContentLines).toString, headers = HEADERS_AJAX)
    } else {
      Response(ResponseBody(status = false, Nil).toString, headers = HEADERS_AJAX)
    }
  }

  @get("/logs/receivedTelegrams")
  def getReceivedTelegramsLogs() = {
    if (os.exists(PYTHON_RUNTIME_LOGS_FOLDER_PATH)) {
      val logsFolders = os.list(PYTHON_RUNTIME_LOGS_FOLDER_PATH)
      if (logsFolders.nonEmpty) {
        // We take the latest
        val latestFolder = logsFolders.max
        val logFilePath = latestFolder / PYTHON_RUNTIME_LOGS_RECEIVED_TELEGRAMS_LOG_FILE_NAME
        if (os.exists(logFilePath)) {
          val content = os.read.lines(logFilePath).toList
          Response(ResponseBody(status = true, content).toString, headers = HEADERS_AJAX)
        } else {
          Response(ResponseBody(status = false, Nil).toString, headers = HEADERS_AJAX)
        }
      } else {
        Response(ResponseBody(status = false, Nil).toString, headers = HEADERS_AJAX)
      }
    } else {
      Response(ResponseBody(status = false, Nil).toString, headers = HEADERS_AJAX)
    }
  }

  @get("/logs/execution")
  def getExecutionLogs() = {
    if (os.exists(PYTHON_RUNTIME_LOGS_FOLDER_PATH)) {
      val logsFolders = os.list(PYTHON_RUNTIME_LOGS_FOLDER_PATH)
      if (logsFolders.nonEmpty) {
        // We take the latest
        val latestFolder = logsFolders.max
        val logFilePath = latestFolder / PYTHON_RUNTIME_LOGS_EXECUTION_LOG_FILE_NAME
        if (os.exists(logFilePath)) {
          val content = os.read.lines(logFilePath).toList
          Response(ResponseBody(status = true, content).toString, headers = HEADERS_AJAX)
        } else {
          Response(ResponseBody(status = false, Nil).toString, headers = HEADERS_AJAX)
        }
      } else {
        Response(ResponseBody(status = false, Nil).toString, headers = HEADERS_AJAX)
      }
    } else {
      Response(ResponseBody(status = false, Nil).toString, headers = HEADERS_AJAX)
    }
  }
  @get("/logs/physicalState")
  def getPhysicalStateLog() = {
    val physicalStateLogPath = Constants.PYTHON_RUNTIME_LOGS_PHYSICAL_STATE_LOG_FILE_PATH
    if (os.exists(physicalStateLogPath)) {
      val physicalStateContent = FileUtils.readFileContentAsString(physicalStateLogPath)
      Response(physicalStateContent, headers = HEADERS_AJAX)
    } else {
      Response("", statusCode = NOT_FOUND_ERROR_CODE, headers = HEADERS_AJAX)
    }

  }

  @post("/stopRun")
  def stopRun() = acquireLockAndExecute(
    () => {
      cleanTempFolders()
      forceKillSvshiRun()
      Response(ResponseBody(status = true, output = List(RUN_STOPPED_MESSAGE)).toString, headers = HEADERS_AJAX)
    },
    defaultResponseStringIfLocked
  )

  @get("/newApps")
  def getNewApps() = {
    val foldersList = FileUtils.getListOfFolders(Constants.GENERATED_FOLDER_PATH)
    val appsList = foldersList.map(p => p.segments.toList.last)
    Response(ResponseBody(status = true, output = appsList).toString, headers = HEADERS_AJAX)
  }

  @get("/bindings")
  def getGeneratedBindings() = {
    if (os.exists(appsBindingsGeneratedFilePath) && os.exists(physicalStructureJsonGeneratedFilePath)) {
      val bindings = BindingsJsonParser.parse(appsBindingsGeneratedFilePath)
      val physStruct = PhysicalStructureJsonParser.parseJson(os.read(physicalStructureJsonGeneratedFilePath))
      Response(BindingsResponse(bindings = bindings, physicalStructure = physStruct).toString, headers = HEADERS_AJAX)
    } else {
      Response(BINDINGS_NOT_FOUND_MESSAGE, statusCode = NOT_FOUND_ERROR_CODE, headers = HEADERS_AJAX)
    }
  }

  @post("/generated")
  def postNewInGenerated(request: Request) = acquireLockAndExecute(
    () => {
      cleanTempFolders()
      if (os.exists(privateTempFolderPath)) os.remove.all(privateTempFolderPath)
      os.makeDir.all(privateTempFolderPath)
      unzipContentAndExec(request)(
        privateTempFolderPath,
        path => {
          val moved = FileUtils.recursiveListFiles(path).map(p => p.toString())
          FileUtils.moveAllFileToOtherDirectory(path, Constants.GENERATED_FOLDER_PATH)
          Response(ResponseBody(status = true, output = moved).toString, headers = HEADERS_AJAX)
        }
      )
    },
    defaultResponseStringIfLocked
  )

  @get("/generated")
  def getGenerated() = {
    val toZip = os.list(Constants.GENERATED_FOLDER_PATH).toList
    FileUtils.zip(toZip, privateTempZipFilePath) match {
      case Some(zippedPath) => {
        val data = os.read.bytes(zippedPath)
        Response(data = data, headers = HEADERS_AJAX)
      }
      case None => Response(data = Array[Byte](), statusCode = INTERNAL_ERROR_CODE, headers = HEADERS_AJAX)
    }
  }

  @post("/deleteGenerated")
  def deleteGenerated() = acquireLockAndExecute(
    () => {
      cleanTempFolders()
      Try({
        if (os.exists(Constants.GENERATED_FOLDER_PATH)) os.remove.all(Constants.GENERATED_FOLDER_PATH)
        os.makeDir.all(Constants.GENERATED_FOLDER_PATH)
      }) match {
        case Failure(exception) =>
          val responseBody = ResponseBody(status = false, output = exception.getLocalizedMessage.split("\n").toList)
          Response(responseBody.toString, headers = HEADERS_AJAX)
        case Success(_) =>
          val responseBody = ResponseBody(status = true, output = List(GENERATED_FOLDER_DELETED_MESSAGE))
          Response(responseBody.toString, headers = HEADERS_AJAX)
      }

    },
    defaultResponseStringIfLocked
  )

  @get("/installedApp/:appName")
  def getInstalledApp(appName: String) = {
    val appPath = Constants.INSTALLED_APPS_FOLDER_PATH / appName
    if (os.exists(appPath)) {
      FileUtils.zip(List(appPath), privateTempZipFilePath) match {
        case Some(zippedPath) => {
          val data = os.read.bytes(zippedPath)
          Response(data = data, headers = HEADERS_AJAX)
        }
        case None => Response(data = Array[Byte](), statusCode = INTERNAL_ERROR_CODE, headers = HEADERS_AJAX)
      }
    } else {
      Response(data = Array[Byte](), statusCode = NOT_FOUND_ERROR_CODE, headers = HEADERS_AJAX)
    }
  }

  @get("/allInstalledApps")
  def getInstalledApp() = {
    val folderPath = Constants.INSTALLED_APPS_FOLDER_PATH
    if (os.exists(folderPath)) {
      FileUtils.zip(List(folderPath), privateTempZipFilePath) match {
        case Some(zippedPath) => {
          val data = os.read.bytes(zippedPath)
          Response(data = data, headers = HEADERS_AJAX)
        }
        case None => Response(data = Array[Byte](), statusCode = INTERNAL_ERROR_CODE, headers = HEADERS_AJAX)
      }
    } else {
      Response(data = Array[Byte](), statusCode = NOT_FOUND_ERROR_CODE, headers = HEADERS_AJAX)
    }
  }

  @get("/assignments")
  def getAssignments() = {
    val assignmentsPath = Constants.ASSIGNMENTS_DIRECTORY_PATH
    if (os.exists(assignmentsPath) && os.list(assignmentsPath).nonEmpty) {
      FileUtils.zip(List(assignmentsPath), privateTempZipFilePath) match {
        case Some(zippedPath) => {
          val data = os.read.bytes(zippedPath)
          Response(data = data, headers = HEADERS_AJAX)
        }
        case None => Response(data = Array[Byte](), statusCode = INTERNAL_ERROR_CODE, headers = HEADERS_AJAX)
      }
    } else {
      Response(data = Array[Byte](), statusCode = NOT_FOUND_ERROR_CODE, headers = HEADERS_AJAX)
    }
  }

  private def cleanTempFolders() = {
    if (os.exists(knxprojPrivateFolderPath)) os.remove.all(knxprojPrivateFolderPath)
    if (os.exists(privateTempZipFilePath)) os.remove.all(privateTempZipFilePath)
    if (os.exists(privateTempFolderPath)) os.remove.all(privateTempFolderPath)
  }
  private def createStrMessagePrefix(s: String, prefix: String) = if (prefix.isBlank) s else f"$prefix: $s"

  private def acquireLockAndExecute[B](exec: () => B, otherwise: B): B = {
    if (lock.tryLock()) {
      try {
        exec()
      } finally {
        lock.unlock()
      }
    } else { otherwise }
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
  private def extractKnxprojOrJsonFileAndExec(request: Request)(withPhysStruct: PhysicalStructure => Response[String]): Response[String] = {
    if (os.exists(knxprojPrivateFolderPath)) os.remove(knxprojPrivateFolderPath)
    unzipContentAndExec(request)(
      knxprojPrivateFolderPath,
      path => {
        val knxprojOrJsonFile = FileUtils.recursiveListFiles(path).head
        val extension = FileUtils.getFileExtension(knxprojOrJsonFile)
        if (extension == knxprojExtension) {
          Try(EtsParser.parseEtsProjectFile(knxprojOrJsonFile)) match {
            case Failure(exception) => Response(exception.getLocalizedMessage, statusCode = BAD_REQUEST_CODE, headers = HEADERS_AJAX)
            case Success(newPhysicalStructure) =>
              withPhysStruct(newPhysicalStructure)
          }
        } else if (extension == jsonExtension) {
          Try(PhysicalStructureJsonParser.parse(knxprojOrJsonFile)) match {
            case Failure(exception) => Response(exception.getLocalizedMessage, statusCode = BAD_REQUEST_CODE, headers = HEADERS_AJAX)
            case Success(newPhysicalStructure) =>
              withPhysStruct(newPhysicalStructure)
          }
        } else {
          Response(WRONG_EXTENSION_FILE_ERROR_MESSAGE, statusCode = BAD_REQUEST_CODE, headers = HEADERS_AJAX)
        }
      }
    )
  }

  initialize()
}
