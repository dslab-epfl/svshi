package ch.epfl.web.service.main

import ch.epfl.web.service.main.simulator.SimulatorInterface
import ch.epfl.web.service.main.svshi.SvshiInterface

trait SvshiSimInterfaceFactory {
  def getSvshiHttpInterface(ip: String, port: Int): SvshiInterface
  def getSimulatorHttpInterface(ip: String, port: Int): SimulatorInterface
}
