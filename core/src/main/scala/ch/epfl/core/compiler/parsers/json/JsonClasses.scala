package ch.epfl.core.compiler.parsers.json

import upickle.default.{ReadWriter, macroRW}

case class ChannelJsonParsed(name: String, datatype: String, @upickle.implicits.key("type") typee: String)
object ChannelJsonParsed {
  implicit val channel: ReadWriter[ChannelJsonParsed] = macroRW[ChannelJsonParsed]
}


case class DeviceTypeJsonParsed(@upickle.implicits.key("type") name: String, channels: List[ChannelJsonParsed])
object DeviceTypeJsonParsed {
  implicit val deviceType: ReadWriter[DeviceTypeJsonParsed] = macroRW[DeviceTypeJsonParsed]
}

case class DeviceInstanceJsonParsed(name: String, @upickle.implicits.key("type") typee: String)
object DeviceInstanceJsonParsed {
  implicit val deviceInstances: ReadWriter[DeviceInstanceJsonParsed] = macroRW[DeviceInstanceJsonParsed]
}

case class ParsedStructureJsonParsed(deviceTypes: List[DeviceTypeJsonParsed], deviceInstances: List[DeviceInstanceJsonParsed])
object ParsedStructureJsonParsed {
  implicit val parsedStructure: ReadWriter[ParsedStructureJsonParsed] = macroRW[ParsedStructureJsonParsed]
}
