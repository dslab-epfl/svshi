package ch.epfl.web.service.main
import ch.epfl.web.service.main.simulator.{SimulatorHttp, SimulatorInterface}
import ch.epfl.web.service.main.svshi.{SvshiHttp, SvshiInterface}
import ch.epfl.web.service.main.utils.Utils

/** DO NOT CHECK FOR IP VALIDITY!
  * So that container names can be passed when using docker compose
  */
case class SvshiSimHttpInterfaceFactory() extends SvshiSimInterfaceFactory {
  override def getSvshiHttpInterface(ip: String, port: Int): SvshiInterface = {
//    if (!Utils.validAddressPortString(s"$ip:$port")) throw new IllegalArgumentException("Given ip and port are invalid!")
    SvshiHttp(ip, port)
  }

  override def getSimulatorHttpInterface(ip: String, port: Int): SimulatorInterface = {
//    if (!Utils.validAddressPortString(s"$ip:$port")) throw new IllegalArgumentException("Given ip and port are invalid!")
    SimulatorHttp(ip, port)
  }
}
