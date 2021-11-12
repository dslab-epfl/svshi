package ch.epfl.core.compiler.parsers.json

import ch.epfl.core.models.physical.{IOType, KNXDatatype}
import ch.epfl.core.models.prototypical
import ch.epfl.core.models.prototypical.{AppProtoDeviceChannel, AppProtoDeviceInstance, AppProtoDeviceType, AppPrototypicalStructure}

import scala.io.Source
import scala.util.Using

object AppInputJsonParser {
  def parse(filePath: String): AppPrototypicalStructure = {
    Using(Source.fromFile(filePath)) { fileBuff =>
      constructPrototypicalStructure(parseJson(fileBuff.getLines().toList.mkString))
    }.get
  }

  def constructPrototypicalStructure(parsedStructureJsonParsed: ParsedStructureJsonParsed): AppPrototypicalStructure = {
    def convertChannels(parsed: ChannelJsonParsed): AppProtoDeviceChannel = {
      val datatypeString = KNXDatatype.datatypeRegex.findFirstIn(parsed.datatype)
      if(datatypeString.isEmpty) throw new SystemStructureException(s"The datatype ecoding is wrong for ${parsed.datatype} for channel = ${parsed.name}")
      val datatype = KNXDatatype.fromString(datatypeString.get)
      if(datatype.isEmpty) throw new SystemStructureException(s"The datatype encoding is wrong for ${parsed.datatype} or unsupported DPT for channel = ${parsed.name}")
      val ioType = IOType.fromString(parsed.typee)
      if(ioType.isEmpty) throw new SystemStructureException(s"Wrong IoType (type = ${parsed.typee}) for channel = ${parsed.name}")
      AppProtoDeviceChannel(parsed.name, datatype.get, ioType.get)
    }
    def convertDeviceType(parsed: DeviceTypeJsonParsed): AppProtoDeviceType = prototypical.AppProtoDeviceType(parsed.name, parsed.channels.map(convertChannels))
    def convertInstances(parsed: DeviceInstanceJsonParsed, deviceTypes: List[AppProtoDeviceType]): AppProtoDeviceInstance = {
      val deviceType = deviceTypes.find(t => t.name == parsed.typee)
      if(deviceType.isEmpty) throw new SystemStructureException(s"The deviceInstance $parsed has a type that has not been defined in this file.")
      AppProtoDeviceInstance(parsed.name, deviceType.get)
    }
    val convertedTypes = parsedStructureJsonParsed.deviceTypes.map(convertDeviceType)
    val convertedInstances = parsedStructureJsonParsed.deviceInstances.map(convertInstances(_, convertedTypes))
    prototypical.AppPrototypicalStructure(convertedTypes, convertedInstances)
  }
  def parseJson(jsonContent: String): ParsedStructureJsonParsed = try {
    upickle.default.read[ParsedStructureJsonParsed](jsonContent)
  } catch{
    case e:Exception => throw new JsonParsingException("The given Json is not parsable, it has either a syntax or the wrong structure.")
  }
}

class SystemStructureException(msg: String) extends Exception(msg)
class JsonParsingException(msg: String) extends Exception(msg)
