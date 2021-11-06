package ch.epfl.core

import ch.epfl.core.compiler.parsers.ets.EtsParser

object Main extends App {

  val projectPathString = "/Users/samuel/EPFL/PdM/windows_VM/workflow_planifier_more_devices.knxproj"
  val listAddresses = EtsParser.explore0xmlFindListAddresses(projectPathString)
//  println(listAddresses)
  val parsedDevice = EtsParser.readDeviceFromEtsFile(projectPathString, ("1", "1", "1"))
//  println(parsedDevice)

  val physicalStructure = EtsParser.parseEtsProjectFile(projectPathString)
  println(physicalStructure)

}
