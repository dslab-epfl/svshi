package ch.epfl.web.service.main.mocks

import cask.{Request, Response, get, post}
import ch.epfl.web.service.main.svshi.ResponseBody
import ch.epfl.web.service.main.utils.{Constants, FileUtils}
import io.undertow.Undertow
import os.Path

case class SvshiHttpServerMock(
    override val host: String = "127.0.0.1",
    override val port: Int = 4242
) extends cask.MainRoutes {

  // Mock stuff begin
  var installedApps: List[String] = Nil
  var nextVerificationError: Option[String] = None
  var nextGenerationBindingError: Option[String] = None
  var generatedContent: List[String] = Nil
  var bindingsGenerated = false
  var nextGenBindingErrorCode: Option[Int] = None
  var nextCompileErrorCode: Option[Int] = None
  var runningIpPort = ""
  var running = false

  def resetMock() = {
    installedApps = Nil
    nextVerificationError = None
    nextGenerationBindingError = None
    generatedContent = Nil
    nextGenBindingErrorCode = None
    nextCompileErrorCode = None
    running = false
    runningIpPort = ""
  }
  // Mock stuff end

  // Config of Cask
  override def verbose: Boolean = true

  private lazy val server = Undertow.builder.addHttpListener(port, host).setHandler(defaultHandler).build

  private val HEADERS_AJAX = Seq("Access-Control-Allow-Origin" -> "*", "Access-Control-Allow-Credentials" -> "true")

  private val BAD_REQUEST_CODE = 400

  private val resMocksPath = os.pwd / "res" / "mocks"

  private val privateTempZipFilePath = Constants.TEMP_FOLDER_PATH / "svshi_mock"

  /** Samuel Chassot - 14.04.2022
    * I override the main method of the parent class to be able to kill the server later on
    * which is not possible with the library's main method
    *
    * I create the server as a private lazy val and then offer a start and and a stop method to the caller
    *
    * original method is:
    * def main(args: Array[String]): Unit = {
    * if (!verbose) Main.silenceJboss()
    * val server = Undertow.builder
    * .addHttpListener(port, host)
    * .setHandler(defaultHandler)
    * .build
    * server.start()
    * }
    *
    * @param args
    */
  override def main(args: Array[String]): Unit = {
    // Does nothing to not interfere
  }

  def start(): Unit = server.start()

  def stop(): Unit = server.stop()

  @post("/deviceMappings")
  def generateMappings(request: Request) = {
    unzipContentAndExec(request = request)(
      privateTempZipFilePath,
      _ => {
        val json = os.read(resMocksPath / "available_proto_devices.json")
        Response(json, headers = HEADERS_AJAX)
      }
    )
  }

  @post("/generateApp/:appName")
  def generateApp(appName: String, request: Request) = {
    Response(ResponseBody(status = true, output = List("Hello there!")).toString, statusCode = 200)
  }

  @get("/generated/:filename")
  def getGenerated(filename: String) = {
    val data = FileUtils.zipInMem(List(resMocksPath / "app_one_generated")).get
    Response(data = data, headers = HEADERS_AJAX)
  }

  @post("/deleteGenerated/:filename")
  def deleteAllGenerated(filename: String) = {
    generatedContent.filter(e => e != filename)
    val responseBody = ResponseBody(status = true, output = List(f"file '$filename' was deleted")).toString
    Response(responseBody, headers = HEADERS_AJAX)

  }
  @post("/deleteAllGenerated")
  def deleteAllGenerated() = {
    generatedContent = Nil
    val responseBody = ResponseBody(status = true, output = List("The generated folder was successfully emptied!"))
    bindingsGenerated = false
    Response(responseBody.toString, headers = HEADERS_AJAX)
  }

  @post("/generated")
  def postNewInGenerated(request: Request) = {
    unzipContentAndExec(request)(
      privateTempZipFilePath,
      path => {
        val newElements = os.list(privateTempZipFilePath).filter(p => os.isDir(p)).map(p => p.segments.toList.last).toList
        generatedContent ++= newElements
        Response(ResponseBody(status = true, output = newElements).toString, headers = HEADERS_AJAX)
      }
    )
  }
  @get("/listApps")
  def getListApps() = {
    val responseBody = ResponseBody(status = true, output = installedApps).toString
    Response(responseBody, headers = HEADERS_AJAX)
  }

  @post("/removeAllApps")
  def removeAllApps() = {
    val output = List("hello there!", "all apps uninstalled!")
    installedApps = Nil
    Response(ResponseBody(true, output).toString, headers = HEADERS_AJAX)
  }
  @post("/compile")
  def compile(request: Request) = {
    nextCompileErrorCode match {
      case Some(value) => Response("COMPILE ERROR MESSAGE FROM SVSHI", statusCode = value, headers = HEADERS_AJAX)
      case None =>
        nextVerificationError match {
          case Some(value) => Response(ResponseBody(status = false, List(value)).toString, headers = HEADERS_AJAX)
          case None => {
            if (bindingsGenerated) {
              installedApps ++= generatedContent
              Response(ResponseBody(status = true, List()).toString, headers = HEADERS_AJAX)
            } else {
              Response(ResponseBody(status = false, List("No bindings")).toString, headers = HEADERS_AJAX)
            }
          }
        }
    }

  }

  @post("/generateBindings")
  def generateBindings(request: Request) = {
    nextGenBindingErrorCode match {
      case Some(value) => Response("GEN BINDINGS ERROR MESSAGE FROM SVSHI", statusCode = value, headers = HEADERS_AJAX)
      case None =>
        nextGenerationBindingError match {
          case Some(value) => Response(ResponseBody(status = false, List(value)).toString, headers = HEADERS_AJAX)
          case None => {
            bindingsGenerated = true
            Response(ResponseBody(status = true, Nil).toString, headers = HEADERS_AJAX)
          }
        }
    }

  }

  @post("/physicalStructure/parsed")
  def parsePhysicalStructure(request: Request) = {
    unzipContentAndExec(request = request)(
      privateTempZipFilePath,
      _ => {
        val json = os.read(resMocksPath / "physical_structure.json")
        Response(json, headers = HEADERS_AJAX)
      }
    )
  }

  @get("/appLibrary/bindings")
  def getAppLibraryBindings(request: Request) = {
    val json = os.read(resMocksPath / "apps_bindings.json")
    Response(json, headers = HEADERS_AJAX)
  }

  @post("/stopRun")
  def stopRun() = {
    running = false
    runningIpPort = ""
    Response(ResponseBody(status = true, output = List("Stopped")).toString, headers = HEADERS_AJAX)
  }

  @post("/run/:ipPort")
  def run(ipPort: String) = {
    runningIpPort = ipPort
    running = true
    Response(ResponseBody(status = true, List("Started")).toString, headers = HEADERS_AJAX)
  }
  @get("/runStatus")
  def getRunStatus() = {
    Response(ResponseBody(status = running, output = Nil).toString, headers = HEADERS_AJAX)
  }

  @get("/logs/run")
  def getRunLogs() = {
    val logContentLines = List("run log line 1", "run log line 2")
    Response(ResponseBody(status = true, logContentLines).toString, headers = HEADERS_AJAX)
  }

  @get("/logs/receivedTelegrams")
  def getReceivedTelegramsLogs() = {
    val logContentLines = List("run receivedTelegrams line 1", "run receivedTelegrams line 2")
    Response(ResponseBody(status = true, logContentLines).toString, headers = HEADERS_AJAX)
  }

  @get("/logs/execution")
  def getExecutionLogs() = {
    val logContentLines = List("run execution line 1", "run execution line 2")
    Response(ResponseBody(status = true, logContentLines).toString, headers = HEADERS_AJAX)
  }

  private def unzipContentAndExec(request: Request)(outputPath: Path, withUnzipped: Path => Response[String]): Response[String] = {
    val receivedData = request.data.readAllBytes()
    if (os.exists(privateTempZipFilePath)) os.remove.all(privateTempZipFilePath)
    FileUtils.writeToFileOverwrite(privateTempZipFilePath, receivedData)
    val outputPathOpt = FileUtils.unzip(privateTempZipFilePath, outputPath)
    outputPathOpt match {
      case Some(path) => withUnzipped(path)
      case None       =>
        // an error occurred while unzipping, the zip file is probably malformed
        Response("Wrong Zip format malformed!", statusCode = BAD_REQUEST_CODE, headers = HEADERS_AJAX)
    }
  }

  initialize()

}
