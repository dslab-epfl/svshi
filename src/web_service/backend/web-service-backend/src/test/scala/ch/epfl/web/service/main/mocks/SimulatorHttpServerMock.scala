package ch.epfl.web.service.main.mocks

import cask.endpoints.post
import cask.{Request, Response, get}
import ch.epfl.web.service.main.utils.Constants
import io.undertow.Undertow
import upickle.default._

case class SimulatorHttpServerMock(override val host: String = "127.0.0.1", override val port: Int = 4646) extends cask.MainRoutes {

  // Mock stuff begin
  var nextStartErrorCode: Option[Int] = None
  var nextConfigErrorCode: Option[Int] = None
  var nextStopErrorCode: Option[Int] = None
  var started = false
  var config: String = ""

  def resetMock() = {
    nextStartErrorCode = None
    nextStopErrorCode = None
    nextConfigErrorCode = None
    started = false
    config = ""
  }
  // Mock stuff end

  // Config of Cask
  override def verbose: Boolean = true

  private lazy val server = Undertow.builder.addHttpListener(port, host).setHandler(defaultHandler).build

  private val HEADERS_AJAX = Seq("Access-Control-Allow-Origin" -> "*", "Access-Control-Allow-Credentials" -> "true")

  private val BAD_REQUEST_CODE = 400

  private val resMocksPath = os.pwd / "res" / "mocks"

  private val privateTempZipFilePath = Constants.TEMP_FOLDER_PATH / "simulator_mock"

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

  @post("/simulator/start")
  def startSimulator() = {
    nextStartErrorCode match {
      case Some(value) => Response("ERROR MSG SIMULATOR START!", statusCode = value)
      case None => {
        started = true
        Response("Started")
      }
    }
  }

  @post("/simulator/stop")
  def stopSimulator() = {
    nextStopErrorCode match {
      case Some(value) => Response("ERROR MSG SIMULATOR STOP!", statusCode = value)
      case None => {
        started = false
        Response("Stopped!")
      }
    }
  }

  @post("/simulator/config")
  def setConfig(request: Request) = {
    nextConfigErrorCode match {
      case Some(value) => Response("ERROR MSG SIMULATOR CONFIG!", statusCode = value)
      case None => {
        config = request.text()
        Response("Config generated")
      }
    }
  }
  @get("/simulator/running")
  def getRunning(request: Request) = {
    write(Map(("isRunning" -> started)))
  }

  @get("/simulator/ipAddr")
  def getIpAddr(request: Request) = {
    write(Map(("address" -> "127.0.0.1")))
  }

  initialize()
}
