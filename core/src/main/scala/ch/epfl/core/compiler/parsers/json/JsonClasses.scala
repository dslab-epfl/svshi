package ch.epfl.core.compiler.parsers.json

import upickle.default.{ReadWriter, macroRW}

case class DeviceInstanceJson(name: String, deviceType: String)
object DeviceInstanceJson {
  implicit val deviceInstances: ReadWriter[DeviceInstanceJson] = macroRW[DeviceInstanceJson]
}

case class PrototypicalStructureJson(devices: List[DeviceInstanceJson])
object PrototypicalStructureJson {
  implicit val parsedStructure: ReadWriter[PrototypicalStructureJson] = macroRW[PrototypicalStructureJson]
}
