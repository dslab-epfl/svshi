package ch.epfl.core.parser.json.prototype

import upickle.default.{ReadWriter, macroRW}

/**
 * Classes used by upickle in teh AppInputJsonParser
 */


case class DeviceInstanceJson(name: String, deviceType: String)
object DeviceInstanceJson {
  implicit val deviceInstances: ReadWriter[DeviceInstanceJson] = macroRW[DeviceInstanceJson]
}

case class PrototypicalStructureJson(permissionLevel: String, devices: List[DeviceInstanceJson])
object PrototypicalStructureJson {
  implicit val parsedStructure: ReadWriter[PrototypicalStructureJson] = macroRW[PrototypicalStructureJson]
}
