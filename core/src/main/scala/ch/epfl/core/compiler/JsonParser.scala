package ch.epfl.core.compiler

import ch.epfl.core.compiler.jsonModels._
import ch.epfl.core.compiler.models.{DeviceChannel, DeviceType, IOType, KNXDatatype, PrototypicalStructure}
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
      
    }
    def convertDeviceType(parsed: DeviceTypeJsonParsed): DeviceType = DeviceType(parsed.name, parsed.channels.map(convertChannels))

    PrototypicalStructure()
  }
  def parseJson(jsonContent: String): ParsedStructureJsonParsed = upickle.default.read[ParsedStructureJsonParsed](jsonContent)
}
