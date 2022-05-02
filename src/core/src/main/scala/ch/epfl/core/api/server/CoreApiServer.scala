package ch.epfl.core.api.server

import cask.{Request, Response, get, post}
import ch.epfl.core.api.server.json.{BindingsResponse, ResponseBody}
import ch.epfl.core.model.application.ApplicationLibrary
import ch.epfl.core.model.physical.PhysicalStructure
import ch.epfl.core.parser.ets.EtsParser
import ch.epfl.core.parser.json.bindings.BindingsJsonParser
import ch.epfl.core.parser.json.physical.PhysicalStructureJsonParser
import ch.epfl.core.parser.json.prototype.AppInputJsonParser
import ch.epfl.core.utils.Constants.{APP_LIBRARY_FOLDER_PATH, GENERATED_FOLDER_PATH, PHYSICAL_STRUCTURE_JSON_FILE_NAME}
import ch.epfl.core.utils.Utils.loadApplicationsLibrary
import ch.epfl.core.utils.{Constants, FileUtils}
import ch.epfl.core.{Svshi, SvshiRunResult, SvshiTr}
import io.undertow.Undertow
import os.Path

import scala.util.{Failure, Success, Try}

case class CoreApiServer(svshiSystem: SvshiTr = Svshi, debug: String => Unit = _ => ()) extends cask.MainRoutes {
  // Config of Cask
  override def port: Int = Constants.SVSHI_GUI_SERVER_PORT

  private lazy val server = Undertow.builder.addHttpListener(port, host).setHandler(defaultHandler).build
  private val BAD_REQUEST_CODE = 400
  private val SUCCESS_REQUEST_CODE = 200
  private val NOT_FOUND_ERROR_CODE = 404
  private val INTERNAL_ERROR_CODE = 500

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

  private val HEADERS_AJAX = Seq("Access-Control-Allow-Origin" -> "*")

  private var svshiRunResult: Option[SvshiRunResult] = None
  private def runStateRunning: Boolean = svshiRunResult.isDefined && svshiRunResult.get.isAlive

  val MAX_LOG_LINES_SENT = 10000

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
  }

  @get("/")
  def welcomeRoot() = "API server for SVSHI interface"

  @get("/version")
  def getVersion() = {
    cleanTempFolders()
    var output: List[String] = List()
    val resCode = svshiSystem.getVersion(s => output = output ::: List(s))
    Response(data = ResponseBody(resCode == Svshi.SUCCESS_CODE, output).toString, headers = HEADERS_AJAX)
  }

  @get("/listApps")
  def listApps() = {
    cleanTempFolders()
    val (existingAppsLibrary, _, _) = loadCurrentSVSHIState
    val installedApps = svshiSystem.listApps(existingAppsLibrary)
    Response(ResponseBody(status = true, installedApps).toString, headers = HEADERS_AJAX)
  }

  @get("/availableProtoDevices")
  def getAvailableProtoDevices() = {
    val availableDevices = svshiSystem.getAvailableProtoDevices()
    Response(ResponseBody(status = true, availableDevices).toString, headers = HEADERS_AJAX)
  }

  @post("/generateApp/:appName")
  def generateApp(appName: String, request: Request) = {
    cleanTempFolders()
    val protoJsonString = request.text()
    debug(f"Received generateNewApp for $appName with json = $protoJsonString")
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
  }

  @post("/compile")
  def compile(request: Request) = {
    debug("Received a compile request")
    cleanTempFolders()
    val (existingAppsLibrary, newAppsLibrary, _) = loadCurrentSVSHIState
    extractKnxprojFileAndExec(request)(newPhysicalStructure => {
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
  }

  @post("/updateApp/:appName")
  def updateApp(appName: String) = {
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
  }

  @post("/generateBindings")
  def generateBindings(request: Request) = {
    cleanTempFolders()
    val (existingAppsLibrary, newAppsLibrary, existingPhysicalStructure) = loadCurrentSVSHIState
    extractKnxprojFileAndExec(request)(newPhysicalStructure => {
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
  }

  @post("/removeApp/:appName")
  def removeApp(appName: String) = {
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
  }

  @post("/removeAllApps")
  def removeAllApps() = {
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
  }

  @post("/run/:ipPort")
  def run(ipPort: String) = {
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
  }

  @get("/runStatus")
  def getRunStatus() = {
    cleanTempFolders()
    Response(ResponseBody(status = runStateRunning, output = Nil).toString, headers = HEADERS_AJAX)
  }

  @get("/runLogs")
  def getRunLogs() = {
    cleanTempFolders()
    if (os.exists(localRunLogFilePath)) {
      val logContentLines = os.read.lines(localRunLogFilePath).toList
      Response(ResponseBody(status = true, logContentLines).toString, headers = HEADERS_AJAX)
    } else {
      Response(ResponseBody(status = false, Nil).toString, headers = HEADERS_AJAX)
    }

  }

  @post("/stopRun")
  def stopRun() = {
    cleanTempFolders()
    forceKillSvshiRun()
    Response(ResponseBody(status = true, output = List(RUN_STOPPED_MESSAGE)).toString, headers = HEADERS_AJAX)
  }

  @get("/newApps")
  def getNewApps() = {
    cleanTempFolders()
    val foldersList = FileUtils.getListOfFolders(Constants.GENERATED_FOLDER_PATH)
    val appsList = foldersList.map(p => p.segments.toList.last)
    Response(ResponseBody(status = true, output = appsList).toString, headers = HEADERS_AJAX)
  }

  @get("/bindings")
  def getGeneratedBindings() = {
    cleanTempFolders()
    if (os.exists(appsBindingsGeneratedFilePath) && os.exists(physicalStructureJsonGeneratedFilePath)) {
      val bindings = BindingsJsonParser.parse(appsBindingsGeneratedFilePath)
      val physStruct = PhysicalStructureJsonParser.parseJson(os.read(physicalStructureJsonGeneratedFilePath))
      Response(BindingsResponse(bindings = bindings, physicalStructure = physStruct).toString, headers = HEADERS_AJAX)
    } else {
      Response(BINDINGS_NOT_FOUND_MESSAGE, statusCode = NOT_FOUND_ERROR_CODE, headers = HEADERS_AJAX)
    }
  }

  @post("/generated")
  def postNewInGenerated(request: Request) = {
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
  }

  @get("/generated")
  def getGenerated() = {
    cleanTempFolders()
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
  def deleteGenerated() = {
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

  }

  @get("/installedApp/:appName")
  def getInstalledApp(appName: String) = {
    cleanTempFolders()
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

  private def cleanTempFolders() = {
    if (os.exists(knxprojPrivateFolderPath)) os.remove.all(knxprojPrivateFolderPath)
    if (os.exists(privateTempZipFilePath)) os.remove.all(privateTempZipFilePath)
    if (os.exists(privateTempFolderPath)) os.remove.all(privateTempFolderPath)
  }
  private def createStrMessagePrefix(s: String, prefix: String) = if (prefix.isBlank) s else f"$prefix: $s"

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
  private def extractKnxprojFileAndExec(request: Request)(withPhysStruct: PhysicalStructure => Response[String]): Response[String] = {
    if (os.exists(knxprojPrivateFolderPath)) os.remove(knxprojPrivateFolderPath)
    unzipContentAndExec(request)(
      knxprojPrivateFolderPath,
      path => {
        val knxprofFile = FileUtils.recursiveListFiles(path).head
        Try(EtsParser.parseEtsProjectFile(knxprofFile)) match {
          case Failure(exception) => Response(exception.getLocalizedMessage, statusCode = BAD_REQUEST_CODE, headers = HEADERS_AJAX)
          case Success(newPhysicalStructure) =>
            withPhysStruct(newPhysicalStructure)
        }
      }
    )
  }

  initialize()
}
