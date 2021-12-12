package ch.epfl.core.parser.json.prototype

import ch.epfl.core.model.application.{NotPrivileged, PermissionLevel, Privileged}
import ch.epfl.core.model.prototypical._
import ch.epfl.core.parser.json.JsonParsingException

import scala.io.Source
import scala.util.Using

/** Parser used to read JSON file containing the structure of applications (i.e., the prototypical devices they use)
  */
object AppInputJsonParser {
  val APP_PROTO_JSON_RELATIVE_PATH = "app_prototypical_structure.json"

  /** Parse the JSON file and produce an AppPrototypicalStructure instance
    * @param filePath
    * @return
    */
  def parse(filePath: String): AppPrototypicalStructure = {
    Using(Source.fromFile(filePath)) { fileBuff =>
      constructPrototypicalStructure(
        parseJson(fileBuff.getLines().toList.mkString)
      )
    }.get
  }

  /** Produce an instance of AppPrototypicalStructure from an instance of PrototypicalStructureJson produced by upickle
    * @param parsedStructure
    * @return
    */
  def constructPrototypicalStructure(parsedStructure: PrototypicalStructureJson): AppPrototypicalStructure = {
    def convertDeviceInstance(deviceInstanceJson: DeviceInstanceJson): AppPrototypicalDeviceInstance = {
      val deviceType = SupportedDevice.fromString(deviceInstanceJson.deviceType)
      AppPrototypicalDeviceInstance(deviceInstanceJson.name, deviceType)
    }
    val permissionLevel = PermissionLevel.fromString(parsedStructure.permissionLevel)
    if (permissionLevel.isEmpty)
      throw new JsonParsingException(
        s"The permission level '${parsedStructure.permissionLevel}' is unknown. PermissionLevel must be '${NotPrivileged.toString}' or '${Privileged.toString}'!"
      )
    AppPrototypicalStructure(permissionLevel = permissionLevel.get, deviceInstances = parsedStructure.devices.map(convertDeviceInstance))
  }

  /** Produce an instance of PrototypicalStructureJson from JSON file content using upickle
    * @param jsonContent
    * @return
    */
  def parseJson(jsonContent: String): PrototypicalStructureJson = try {
    upickle.default.read[PrototypicalStructureJson](jsonContent)
  } catch {
    case e: Exception =>
      throw new JsonParsingException("The given Json is not parsable, it has either a syntax error or the wrong structure.")
  }
}
