package ch.epfl.core.compiler

import ch.epfl.core.compiler.jsonModels._
import ch.epfl.core.compiler.models.{DeviceChannel, DeviceInstance, DeviceType, IOType, KNXDatatype, PrototypicalStructure}
import upickle.default.{macroRW, ReadWriter => RW}

import java.nio.file.Path
import scala.io.Source

object JsonParser {
  def parse(filePath: String): PrototypicalStructure = {
    val fileBuff = Source.fromFile(filePath)
    val fileContent = fileBuff.getLines().toList.mkString
    fileBuff.close()
    constructPrototypicalStructure(parseJson(fileContent))
  }

  def constructPrototypicalStructure(parsedStructureJsonParsed: ParsedStructureJsonParsed): PrototypicalStructure = {
    def convertChannels(parsed: ChannelJsonParsed): DeviceChannel = {
      val datatypeString = KNXDatatype.datatypeRegex.findFirstIn(parsed.datatype)
      if(datatypeString.isEmpty) throw new SystemStructureException(s"The datatype ecoding is wrong for ${parsed.datatype} for channel = ${parsed.name}")
      val datatype = KNXDatatype.fromString(datatypeString.get)
      if(datatype.isEmpty) throw new SystemStructureException(s"The datatype encoding is wrong for ${parsed.datatype} or unsupported DPT for channel = ${parsed.name}")
      val ioType = IOType.fromString(parsed.typee)
      if(ioType.isEmpty) throw new SystemStructureException(s"Wrong IoType (type = ${parsed.typee}) for channel = ${parsed.name}")
      DeviceChannel(parsed.name, datatype.get, ioType.get)
    }
    def convertDeviceType(parsed: DeviceTypeJsonParsed): DeviceType = DeviceType(parsed.name, parsed.channels.map(convertChannels))
    def convertInstances(parsed: DeviceInstanceJsonParsed, deviceTypes: List[DeviceType]): DeviceInstance = {
      val deviceType = deviceTypes.find(t => t.name == parsed.typee)
      if(deviceType.isEmpty) throw new SystemStructureException(s"The deviceInstance $parsed has a type that has not been defined in this file.")
      DeviceInstance(parsed.name, deviceType.get)
    }
    val convertedTypes = parsedStructureJsonParsed.deviceTypes.map(convertDeviceType)
    val convertedInstances = parsedStructureJsonParsed.deviceInstances.map(convertInstances(_, convertedTypes))
    PrototypicalStructure(convertedTypes, convertedInstances)
  }
  def parseJson(jsonContent: String): ParsedStructureJsonParsed = try {
    upickle.default.read[ParsedStructureJsonParsed](jsonContent)
  } catch{
    case e:Exception => throw new JsonParsingException("The given Json is not parsable, it has either a syntax or the wrong structure.")
  }
}

class SystemStructureException(msg: String) extends Exception(msg)
class JsonParsingException(msg: String) extends Exception(msg)
