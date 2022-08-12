package ch.epfl.core.parser.json.prototype

import ch.epfl.core.model.prototypical.SupportedDevice
import upickle.default.{ReadWriter, macroRW}

/** Classes used by upickle in the AppInputJsonParser
  */

case class DeviceInstanceJson(name: String, deviceType: String, preBindingPhysId: Int = SupportedDevice.defaultPhysicalId)
object DeviceInstanceJson {
  implicit val deviceInstance: ReadWriter[DeviceInstanceJson] = macroRW[DeviceInstanceJson]
}

case class PrototypicalStructureJson(permissionLevel: String, timer: Int, devices: List[DeviceInstanceJson])
object PrototypicalStructureJson {
  implicit val parsedStructure: ReadWriter[PrototypicalStructureJson] = macroRW[PrototypicalStructureJson]
}
