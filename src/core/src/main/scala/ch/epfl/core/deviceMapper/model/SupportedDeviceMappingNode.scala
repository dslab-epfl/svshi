package ch.epfl.core.deviceMapper.model

import upickle.default.{ReadWriter, macroRW}

case class SupportedDeviceMappingNode(name: String, supportedDeviceMappings: List[SupportedDeviceMapping])
object SupportedDeviceMappingNode {
  implicit val rw: ReadWriter[SupportedDeviceMappingNode] =
    macroRW[SupportedDeviceMappingNode]
}
