package ch.epfl.core.parser

import ch.epfl.core.model.prototypical._
import ch.epfl.core.parser.json.bindings.BindingsJsonParser
import ch.epfl.core.utils.Constants
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class BindingsJsonParserTest extends AnyFlatSpec with Matchers {
  private val coreResDirPath = Constants.SVSHI_SRC_FOLDER_PATH / "core" / "res"
  "parse" should "return the right structure given a valid file" in {
    val res = BindingsJsonParser.parse(coreResDirPath / "test_bindings_json_parser.json")

    val expected = AppLibraryBindings(
      List(
        AppPrototypeBindings(
          "app1",
          List(
            DeviceInstanceBinding("device1", BinarySensorBinding(BinarySensor.toString, 42)),
            DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 41)),
            DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 39)),
            DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 38))
          )
        )
      )
    )
    res shouldEqual expected
  }

  "writeToFile" should "write the correct json file on correct input" in {
    val expected = AppLibraryBindings(
      List(
        AppPrototypeBindings(
          "app1",
          List(
            DeviceInstanceBinding("device1", BinarySensorBinding(BinarySensor.toString, 42)),
            DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 41)),
            DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 39)),
            DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 38))
          )
        )
      )
    )
    BindingsJsonParser.writeToFile(coreResDirPath / "test_bindings_json_parser_write.json", expected)
    val res = BindingsJsonParser.parse(coreResDirPath / "test_bindings_json_parser_write.json")
    res shouldEqual expected
  }
}
