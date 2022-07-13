package ch.epfl.core.deviceMapper.model

import upickle.default.{ReadWriter, macroRW}

case class SupportedDeviceMapping(name: String, supportedDeviceName: String, physicalCommObjectId: Int)
object SupportedDeviceMapping {
  implicit val rw: ReadWriter[SupportedDeviceMapping] =
    macroRW[SupportedDeviceMapping]
}
