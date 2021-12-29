package ch.epfl.core.parser

import ch.epfl.core.model.application.{NotPrivileged, Privileged}
import ch.epfl.core.model.prototypical._
import ch.epfl.core.parser.json.JsonParsingException
import ch.epfl.core.parser.json.prototype.{AppInputJsonParser, DeviceInstanceJson, PrototypicalStructureJson}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class AppInputJsonParserJsonParserTest extends AnyFlatSpec with Matchers {
  "parseJson" should "return the correct ParsedStructure with correct input NotPrivileged" in {
    val json =
      """
        |{
        |    "permissionLevel": "notPrivileged",
        |    "devices":
        |    [
        |       {
        |         "name": "device1",
        |         "deviceType": "binary"
        |       },
        |       {
        |         "name": "device2",
        |         "deviceType": "binary"
        |       },
        |       {
        |         "name": "device3",
        |         "deviceType": "switch"
        |       }
        |    ]
        |}
        |
        |""".stripMargin

    val device1 = DeviceInstanceJson("device1", "binary")
    val device2 = DeviceInstanceJson("device2", "binary")
    val device3 = DeviceInstanceJson("device3", "switch")
    val app = PrototypicalStructureJson("notPrivileged", List(device1, device2, device3))

    AppInputJsonParser.parseJson(json) shouldEqual app
  }

  "parseJson" should "return the correct ParsedStructure with correct input NotPrivileged even with timer entry" in {
    val json =
      """
        |{
        |    "permissionLevel": "notPrivileged",
        |    "timer": 60,
        |    "devices":
        |    [
        |       {
        |         "name": "device1",
        |         "deviceType": "binary"
        |       },
        |       {
        |         "name": "device2",
        |         "deviceType": "binary"
        |       },
        |       {
        |         "name": "device3",
        |         "deviceType": "switch"
        |       }
        |    ]
        |}
        |
        |""".stripMargin

    val device1 = DeviceInstanceJson("device1", "binary")
    val device2 = DeviceInstanceJson("device2", "binary")
    val device3 = DeviceInstanceJson("device3", "switch")
    val app = PrototypicalStructureJson("notPrivileged", List(device1, device2, device3))

    AppInputJsonParser.parseJson(json) shouldEqual app
  }
  "parseJson" should "return the correct ParsedStructure with correct input Privileged" in {
    val json =
      """
        |{
        |    "permissionLevel": "privileged",
        |    "devices":
        |    [
        |       {
        |         "name": "device1",
        |         "deviceType": "binary"
        |       },
        |       {
        |         "name": "device2",
        |         "deviceType": "binary"
        |       },
        |       {
        |         "name": "device3",
        |         "deviceType": "switch"
        |       }
        |    ]
        |}
        |
        |""".stripMargin

    val device1 = DeviceInstanceJson("device1", "binary")
    val device2 = DeviceInstanceJson("device2", "binary")
    val device3 = DeviceInstanceJson("device3", "switch")
    val app = PrototypicalStructureJson("privileged", List(device1, device2, device3))

    AppInputJsonParser.parseJson(json) shouldEqual app
  }

  "constructPrototypicalStructure(parseJson(...))" should "throw UnsupportedDeviceException on unsupported device type" in {
    val json =
      """
        |{
        |    "permissionLevel": "notPrivileged",
        |    "devices":
        |    [
        |       {
        |         "name": "device1",
        |         "deviceType": "binarySensorHell"
        |       },
        |       {
        |         "name": "device2",
        |         "deviceType": "binary"
        |       },
        |       {
        |         "name": "device3",
        |         "deviceType": "switch"
        |       }
        |    ]
        |}
        |
        |""".stripMargin

    an[MatchError] should be thrownBy AppInputJsonParser
      .constructPrototypicalStructure(AppInputJsonParser.parseJson(json))
  }

  "constructPrototypicalStructure(parseJson(...))" should "return the correct converted structure notPrivileged" in {
    val json =
      """
        |{
        |    "permissionLevel": "notPrivileged",
        |    "devices":
        |    [
        |       {
        |         "name": "device1",
        |         "deviceType": "binary"
        |       },
        |       {
        |         "name": "device2",
        |         "deviceType": "binary"
        |       },
        |       {
        |         "name": "device3",
        |         "deviceType": "switch"
        |       }
        |    ]
        |}
        |
        |""".stripMargin

    val device1 = AppPrototypicalDeviceInstance("device1", BinarySensor)
    val device2 = AppPrototypicalDeviceInstance("device2", BinarySensor)
    val device3 = AppPrototypicalDeviceInstance("device3", Switch)
    val app = AppPrototypicalStructure(deviceInstances = List(device1, device2, device3), permissionLevel = NotPrivileged)

    AppInputJsonParser.constructPrototypicalStructure(
      AppInputJsonParser.parseJson(json)
    ) shouldEqual app
  }
  "constructPrototypicalStructure(parseJson(...))" should "return the correct converted structure privileged" in {
    val json =
      """
        |{
        |    "permissionLevel": "privileged",
        |    "devices":
        |    [
        |       {
        |         "name": "device1",
        |         "deviceType": "binary"
        |       },
        |       {
        |         "name": "device2",
        |         "deviceType": "binary"
        |       },
        |       {
        |         "name": "device3",
        |         "deviceType": "switch"
        |       }
        |    ]
        |}
        |
        |""".stripMargin

    val device1 = AppPrototypicalDeviceInstance("device1", BinarySensor)
    val device2 = AppPrototypicalDeviceInstance("device2", BinarySensor)
    val device3 = AppPrototypicalDeviceInstance("device3", Switch)
    val app = AppPrototypicalStructure(deviceInstances = List(device1, device2, device3), permissionLevel = Privileged)

    AppInputJsonParser.constructPrototypicalStructure(
      AppInputJsonParser.parseJson(json)
    ) shouldEqual app
  }
  "constructPrototypicalStructure(parseJson(...))" should "throw for unknown PermissionLevel" in {
    val json =
      """
        |{
        |    "permissionLevel": "unknown",
        |    "devices":
        |    [
        |       {
        |         "name": "device1",
        |         "deviceType": "binary"
        |       },
        |       {
        |         "name": "device2",
        |         "deviceType": "binary"
        |       },
        |       {
        |         "name": "device3",
        |         "deviceType": "switch"
        |       }
        |    ]
        |}
        |
        |""".stripMargin

    val device1 = AppPrototypicalDeviceInstance("device1", BinarySensor)
    val device2 = AppPrototypicalDeviceInstance("device2", BinarySensor)
    val device3 = AppPrototypicalDeviceInstance("device3", Switch)
    val app = AppPrototypicalStructure(deviceInstances = List(device1, device2, device3), permissionLevel = Privileged)

    an[JsonParsingException] should be thrownBy AppInputJsonParser.constructPrototypicalStructure(
      AppInputJsonParser.parseJson(json)
    )
  }

}
