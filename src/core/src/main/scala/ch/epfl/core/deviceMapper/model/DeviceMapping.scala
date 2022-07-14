package ch.epfl.core.deviceMapper.model

import upickle.default.{ReadWriter, macroRW}

case class DeviceMapping(physicalAddress: String, supportedDeviceMappingNodes: List[SupportedDeviceMappingNode]) {}
object DeviceMapping {
  implicit val rw: ReadWriter[DeviceMapping] =
    macroRW[DeviceMapping]
}
