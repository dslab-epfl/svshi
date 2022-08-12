package ch.epfl.core.parser

import ch.epfl.core.model.application.{NotPrivileged, Privileged}
import ch.epfl.core.model.prototypical._
import ch.epfl.core.parser.json.JsonParsingException
import ch.epfl.core.parser.json.prototype.{AppInputJsonParser, DeviceInstanceJson, PrototypicalStructureJson}
import ch.epfl.core.utils.{Constants, FileUtils}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import os.Path

class AppInputJsonParserTest extends AnyFlatSpec with Matchers with BeforeAndAfterEach {
  private val protoJsonResDirectoryPath: Path = Constants.SVSHI_SRC_FOLDER_PATH / "core" / "res" / "proto_json"

  private val tempFilePath: Path = protoJsonResDirectoryPath / "temp_file.json"

  override def beforeEach(): Unit = {
    if (os.exists(tempFilePath)) os.remove(tempFilePath)
  }
  override def afterEach(): Unit = {
    if (os.exists(tempFilePath)) os.remove(tempFilePath)
  }
  "parseJson" should "return the correct ParsedStructure with correct input NotPrivileged" in {
    val json = os.read(protoJsonResDirectoryPath / "proto1.json")

    val device1 = DeviceInstanceJson("device1", "binarySensor")
    val device2 = DeviceInstanceJson("device2", "binarySensor")
    val device3 = DeviceInstanceJson("device3", "switch")
    val app = PrototypicalStructureJson(permissionLevel = "notPrivileged", timer = 0, devices = List(device1, device2, device3))

    AppInputJsonParser.parseJson(json) shouldEqual app
  }

  "parseJson" should "return the correct ParsedStructure with correct input Privileged" in {
    val json = os.read(protoJsonResDirectoryPath / "proto2.json")

    val device1 = DeviceInstanceJson("device1", "binarySensor")
    val device2 = DeviceInstanceJson("device2", "binarySensor")
    val device3 = DeviceInstanceJson("device3", "switch")
    val app = PrototypicalStructureJson(permissionLevel = "privileged", timer = 60, devices = List(device1, device2, device3))

    AppInputJsonParser.parseJson(json) shouldEqual app
  }

  "parseJson" should "return the correct ParsedStructure with correct input with preBindings" in {
    val json = os.read(protoJsonResDirectoryPath / "proto7-prebindings.json")

    val device1 = DeviceInstanceJson("device1", "binarySensor", 42)
    val device2 = DeviceInstanceJson("device2", "binarySensor", 46)
    val device3 = DeviceInstanceJson("device3", "switch")
    val app = PrototypicalStructureJson(permissionLevel = "notPrivileged", timer = 60, devices = List(device1, device2, device3))

    AppInputJsonParser.parseJson(json) shouldEqual app
  }

  "constructPrototypicalStructure(parseJson(...))" should "throw UnsupportedDeviceException on unsupported device type" in {
    val json = os.read(protoJsonResDirectoryPath / "proto3.json")

    an[UnsupportedDeviceException] should be thrownBy AppInputJsonParser
      .constructPrototypicalStructure(AppInputJsonParser.parseJson(json))
  }

  "constructPrototypicalStructure(parseJson(...))" should "return the correct converted structure with preBindings" in {
    val json = os.read(protoJsonResDirectoryPath / "proto7-prebindings.json")

    val device1 = AppPrototypicalDeviceInstance("device1", BinarySensor, 42)
    val device2 = AppPrototypicalDeviceInstance("device2", BinarySensor, 46)
    val device3 = AppPrototypicalDeviceInstance("device3", Switch)
    val app = AppPrototypicalStructure(deviceInstances = List(device1, device2, device3), timer = 60, permissionLevel = NotPrivileged)

    AppInputJsonParser.constructPrototypicalStructure(
      AppInputJsonParser.parseJson(json)
    ) shouldEqual app
  }

  "constructPrototypicalStructure(parseJson(...))" should "return the correct converted structure notPrivileged" in {
    val json = os.read(protoJsonResDirectoryPath / "proto4.json")

    val device1 = AppPrototypicalDeviceInstance("device1", BinarySensor)
    val device2 = AppPrototypicalDeviceInstance("device2", BinarySensor)
    val device3 = AppPrototypicalDeviceInstance("device3", Switch)
    val app = AppPrototypicalStructure(deviceInstances = List(device1, device2, device3), timer = 60, permissionLevel = NotPrivileged)

    AppInputJsonParser.constructPrototypicalStructure(
      AppInputJsonParser.parseJson(json)
    ) shouldEqual app
  }
  "constructPrototypicalStructure(parseJson(...))" should "return the correct converted structure privileged" in {
    val json = os.read(protoJsonResDirectoryPath / "proto5.json")

    val device1 = AppPrototypicalDeviceInstance("device1", BinarySensor)
    val device2 = AppPrototypicalDeviceInstance("device2", BinarySensor)
    val device3 = AppPrototypicalDeviceInstance("device3", Switch)
    val app = AppPrototypicalStructure(deviceInstances = List(device1, device2, device3), timer = 60, permissionLevel = Privileged)

    AppInputJsonParser.constructPrototypicalStructure(
      AppInputJsonParser.parseJson(json)
    ) shouldEqual app
  }
  "constructPrototypicalStructure(parseJson(...))" should "throw for unknown PermissionLevel" in {
    val json = os.read(protoJsonResDirectoryPath / "proto6.json")

    val device1 = AppPrototypicalDeviceInstance("device1", BinarySensor)
    val device2 = AppPrototypicalDeviceInstance("device2", BinarySensor)
    val device3 = AppPrototypicalDeviceInstance("device3", Switch)
    val app = AppPrototypicalStructure(deviceInstances = List(device1, device2, device3), timer = 60, permissionLevel = Privileged)

    an[JsonParsingException] should be thrownBy AppInputJsonParser.constructPrototypicalStructure(
      AppInputJsonParser.parseJson(json)
    )
  }

  "writeToFile" should "create a file that gives the same structure when parsed again" in {
    val device1 = DeviceInstanceJson("device1", "binarySensor")
    val device2 = DeviceInstanceJson("device2", "binarySensor")
    val device3 = DeviceInstanceJson("device3", "switch")
    val app = PrototypicalStructureJson(permissionLevel = "privileged", timer = 60, devices = List(device1, device2, device3))

    AppInputJsonParser.writeToFile(tempFilePath, app)

    val json = os.read(tempFilePath)
    AppInputJsonParser.parseJson(json) shouldEqual app
  }

  "writeToFile" should "create a file that gives the same structure when parsed again if file already exists" in {
    val device1 = DeviceInstanceJson("device1", "binarySensor")
    val device2 = DeviceInstanceJson("device2", "binarySensor")
    val device3 = DeviceInstanceJson("device3", "switch")
    val app = PrototypicalStructureJson(permissionLevel = "privileged", timer = 60, devices = List(device1, device2, device3))

    FileUtils.writeToFileOverwrite(tempFilePath, "Content".getBytes)

    AppInputJsonParser.writeToFile(tempFilePath, app)

    val json = os.read(tempFilePath)
    AppInputJsonParser.parseJson(json) shouldEqual app
  }

  "writeToFile" should "create a file that gives the same structure when parsed again with preBindings" in {
    val device1 = DeviceInstanceJson("device1", "binarySensor", preBindingPhysId = 42)
    val device2 = DeviceInstanceJson("device2", "binarySensor", preBindingPhysId = 43)
    val device3 = DeviceInstanceJson("device3", "switch", preBindingPhysId = 44)
    val app = PrototypicalStructureJson(permissionLevel = "privileged", timer = 60, devices = List(device1, device2, device3))

    AppInputJsonParser.writeToFile(tempFilePath, app)

    val json = os.read(tempFilePath)
    AppInputJsonParser.parseJson(json) shouldEqual app
  }

}
