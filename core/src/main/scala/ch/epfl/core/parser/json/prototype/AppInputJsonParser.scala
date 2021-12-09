package ch.epfl.core.parser.json.prototype

import ch.epfl.core.model.application.{NotPrivileged, PermissionLevel, Privileged}
import ch.epfl.core.parser.json.JsonParsingException
import ch.epfl.core.model.prototypical._

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
    val permissionLevel = PermissionLevel.fromString(parsedStructure.permissionLevel)
    if(permissionLevel.isEmpty) throw new JsonParsingException(s"The permission level '${parsedStructure.permissionLevel}' is unknown. PermissionLevel must be '${NotPrivileged.toString}' or '${Privileged.toString}'!")
    AppPrototypicalStructure(permissionLevel = permissionLevel.get, deviceInstances = parsedStructure.devices.map(convertDeviceInstance))
  }
  def parseJson(jsonContent: String): PrototypicalStructureJson = try {
    upickle.default.read[PrototypicalStructureJson](jsonContent)
  } catch {
    case e: Exception =>
      throw new JsonParsingException("The given Json is not parsable, it has either a syntax error or the wrong structure.")
  }
}
