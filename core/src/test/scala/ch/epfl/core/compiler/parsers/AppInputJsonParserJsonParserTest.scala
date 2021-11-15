package ch.epfl.core.compiler.parsers

import ch.epfl.core.compiler.parsers.json.prototype.{AppInputJsonParser, DeviceInstanceJson, PrototypicalStructureJson}
import ch.epfl.core.models.prototypical._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class AppInputJsonParserJsonParserTest extends AnyFlatSpec with Matchers {
  "parseJson" should "return the correct ParsedStructure with correct input" in {
    val json =
      """
        |{
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
    val app = PrototypicalStructureJson(List(device1, device2, device3))

    AppInputJsonParser.parseJson(json) shouldEqual app
  }

  "constructPrototypicalStructure(parseJson(...))" should "throw UnsupportedDeviceException on unsupported device type" in {
    val json =
      """
        |{
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

    an[UnsupportedDeviceException] should be thrownBy AppInputJsonParser
      .constructPrototypicalStructure(AppInputJsonParser.parseJson(json))
  }

  "constructPrototypicalStructure(parseJson(...))" should "return the correct converted structure" in {
    val json =
      """
        |{
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
    val app = AppPrototypicalStructure(List(device1, device2, device3))

    AppInputJsonParser.constructPrototypicalStructure(
      AppInputJsonParser.parseJson(json)
    ) shouldEqual app
  }

}
