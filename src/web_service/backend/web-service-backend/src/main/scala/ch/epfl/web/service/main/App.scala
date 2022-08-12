package ch.epfl.web.service.main

import ch.epfl.web.service.main.utils.Cli.Config
import ch.epfl.web.service.main.utils.Printer.{debug, error}
import ch.epfl.web.service.main.utils.style.{ColorsStyle, Style}
import ch.epfl.web.service.main.utils.{Cli, Utils}
import mainargs.ParserForClass

object App {
  private val CLI_TOTAL_WIDTH = 200
  private var systemExit: SystemExit = DefaultSystemExit
  private val ERROR_CODE = 1
  def main(args: Array[String]): Unit = {
    val config = ParserForClass[Config].constructOrExit(args, totalWidth = CLI_TOTAL_WIDTH)
    implicit val style: Style = ColorsStyle
    config.task match {
      case Cli.Run => {
        val dockerMode = config.docker.value
        val server: ApiServer = if (config.addressPort.isEmpty) {
          ApiServer(SvshiSimHttpInterfaceFactory(), debug = debug, dockerMode = dockerMode)
        } else {
          val addrPortStr = config.addressPort.get
          if (Utils.validAddressPortString(addrPortStr)) {
            val addrPortList = addrPortStr.split(":")
            val (ip, port) = (addrPortList.head, addrPortList.last.toInt)
            ApiServer(SvshiSimHttpInterfaceFactory(), host = ip, port = port, debug = debug, dockerMode = dockerMode)
          } else {
            printErrorAndExit(f"Malformed address:port args for the self address: $addrPortStr")
            ApiServer(SvshiSimHttpInterfaceFactory(), debug = debug, dockerMode = dockerMode)
          }
        }
        server.start()
      }
    }

  }

  private def printErrorAndExit(errorMessage: String)(implicit style: Style): Unit = {
    error(errorMessage)
    systemExit.exit(ERROR_CODE)
  }

  def setSystemExit(newSystemExit: SystemExit): Unit = systemExit = newSystemExit
}

trait SystemExit {
  def exit(errorCode: Int): Unit
}

object DefaultSystemExit extends SystemExit {
  override def exit(errorCode: Int): Unit = sys.exit(errorCode)
}
