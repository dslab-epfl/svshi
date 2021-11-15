package ch.epfl.core.compiler.parsers.json.prototype

import ch.epfl.core.compiler.parsers.json.JsonParsingException
import ch.epfl.core.models.prototypical._

import scala.io.Source
import scala.util.Using

object AppInputJsonParser {
  val APP_PROTO_JSON_RELATIVE_PATH = "app_prototypical_structure.json"

  def parse(filePath: String): AppPrototypicalStructure = {
    Using(Source.fromFile(filePath)) { fileBuff =>
      constructPrototypicalStructure(
        parseJson(fileBuff.getLines().toList.mkString)
      )
    }.get
  }

  def constructPrototypicalStructure(parsedStructure: PrototypicalStructureJson): AppPrototypicalStructure = {
    def convertDeviceInstance(deviceInstanceJson: DeviceInstanceJson): AppPrototypicalDeviceInstance = {
      val deviceType = SupportedDevice.fromString(deviceInstanceJson.deviceType)
      AppPrototypicalDeviceInstance(deviceInstanceJson.name, deviceType)
    }
    AppPrototypicalStructure(parsedStructure.devices.map(convertDeviceInstance))
  }
  def parseJson(jsonContent: String): PrototypicalStructureJson = try {
    upickle.default.read[PrototypicalStructureJson](jsonContent)
  } catch {
    case e: Exception =>
      throw new JsonParsingException("The given Json is not parsable, it has either a syntax error or the wrong structure.")
  }
}
