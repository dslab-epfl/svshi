package ch.epfl.core.parser

import ch.epfl.core.model.application.{NotPrivileged, Privileged}
import ch.epfl.core.model.prototypical._
import ch.epfl.core.parser.json.JsonParsingException
import ch.epfl.core.parser.json.prototype.{AppInputJsonParser, DeviceInstanceJson, PrototypicalStructureJson}
import ch.epfl.core.utils.Constants
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class AppInputJsonParserJsonParserTest extends AnyFlatSpec with Matchers {
  val protoJsonResDirectoryPath = Constants.SVSHI_SRC_FOLDER_PATH / "core" / "res" / "proto_json"

  "parseJson" should "return the correct ParsedStructure with correct input NotPrivileged" in {
    val json = os.read(protoJsonResDirectoryPath / "proto1.json")

    val device1 = DeviceInstanceJson("device1", "binary")
    val device2 = DeviceInstanceJson("device2", "binary")
    val device3 = DeviceInstanceJson("device3", "switch")
    val file1 = "file1.txt"
    val file2 = "file2.png"
    val app = PrototypicalStructureJson(permissionLevel = "notPrivileged", timer = 0, files = List(file1, file2), devices = List(device1, device2, device3))

    AppInputJsonParser.parseJson(json) shouldEqual app
  }

  "parseJson" should "return the correct ParsedStructure with correct input Privileged" in {
    val json = os.read(protoJsonResDirectoryPath / "proto2.json")

    val device1 = DeviceInstanceJson("device1", "binary")
    val device2 = DeviceInstanceJson("device2", "binary")
    val device3 = DeviceInstanceJson("device3", "switch")
    val file1 = "file1.txt"
    val file2 = "file2.png"
    val app = PrototypicalStructureJson(permissionLevel = "privileged", timer = 60, files = List(file1, file2), devices = List(device1, device2, device3))

    AppInputJsonParser.parseJson(json) shouldEqual app
  }

  "constructPrototypicalStructure(parseJson(...))" should "throw UnsupportedDeviceException on unsupported device type" in {
    val json = os.read(protoJsonResDirectoryPath / "proto3.json")

    an[MatchError] should be thrownBy AppInputJsonParser
      .constructPrototypicalStructure(AppInputJsonParser.parseJson(json))
  }

  "constructPrototypicalStructure(parseJson(...))" should "return the correct converted structure notPrivileged" in {
    val json = os.read(protoJsonResDirectoryPath / "proto4.json")

    val device1 = AppPrototypicalDeviceInstance("device1", BinarySensor)
    val device2 = AppPrototypicalDeviceInstance("device2", BinarySensor)
    val device3 = AppPrototypicalDeviceInstance("device3", Switch)
    val app = AppPrototypicalStructure(deviceInstances = List(device1, device2, device3), timer = 60, files = List("file1.txt", "file2.png"), permissionLevel = NotPrivileged)

    AppInputJsonParser.constructPrototypicalStructure(
      AppInputJsonParser.parseJson(json)
    ) shouldEqual app
  }
  "constructPrototypicalStructure(parseJson(...))" should "return the correct converted structure privileged" in {
    val json = os.read(protoJsonResDirectoryPath / "proto5.json")

    val device1 = AppPrototypicalDeviceInstance("device1", BinarySensor)
    val device2 = AppPrototypicalDeviceInstance("device2", BinarySensor)
    val device3 = AppPrototypicalDeviceInstance("device3", Switch)
    val app = AppPrototypicalStructure(deviceInstances = List(device1, device2, device3), timer = 60, files = List("file1.txt", "file2.png"), permissionLevel = Privileged)

    AppInputJsonParser.constructPrototypicalStructure(
      AppInputJsonParser.parseJson(json)
    ) shouldEqual app
  }
  "constructPrototypicalStructure(parseJson(...))" should "throw for unknown PermissionLevel" in {
    val json = os.read(protoJsonResDirectoryPath / "proto6.json")

    val device1 = AppPrototypicalDeviceInstance("device1", BinarySensor)
    val device2 = AppPrototypicalDeviceInstance("device2", BinarySensor)
    val device3 = AppPrototypicalDeviceInstance("device3", Switch)
    val app = AppPrototypicalStructure(deviceInstances = List(device1, device2, device3), timer = 60, files = List("file1.txt", "file2.png"), permissionLevel = Privileged)

    an[JsonParsingException] should be thrownBy AppInputJsonParser.constructPrototypicalStructure(
      AppInputJsonParser.parseJson(json)
    )
  }

}
