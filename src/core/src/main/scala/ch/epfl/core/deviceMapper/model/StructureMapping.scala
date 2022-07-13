package ch.epfl.core.deviceMapper.model

import ch.epfl.core.parser.json.JsonParsingException
import ch.epfl.core.parser.json.physical.PhysicalStructureJson
import ch.epfl.core.utils.FileUtils
import upickle.default.{ReadWriter, macroRW, write}

import java.nio.charset.StandardCharsets

case class StructureMapping(physicalStructureJson: PhysicalStructureJson, deviceMappings: List[DeviceMapping]) {
  def writeToFile(outputFilePath: os.Path): Unit = {
    if (os.isDir(outputFilePath)) {
      throw new IllegalArgumentException("The output path must not be a dir!")
    }
    FileUtils.writeToFileOverwrite(outputFilePath, write(this, indent = 2) getBytes StandardCharsets.UTF_8)
  }

}
object StructureMapping {
  implicit val rw: ReadWriter[StructureMapping] =
    macroRW[StructureMapping]
  def parseFromFile(filePath: os.Path): StructureMapping = parseJson(FileUtils.readFileContentAsString(filePath))
  def parseJson(jsonStr: String): StructureMapping = {
    try {
      upickle.default.read[StructureMapping](jsonStr)
    } catch {
      case e: Exception =>
        throw new JsonParsingException(s"The given Json is not parsable, it has either a syntax error or the wrong structure.\n$e")
    }
  }
}
