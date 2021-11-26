package ch.epfl.core.parsers


import ch.epfl.core.parsers.json.bindings.BindingsJsonParser
import ch.epfl.core.models.prototypical._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class BindingsJsonParserTest extends AnyFlatSpec with Matchers {
  "parse" should "return the right structure given a valid file" in {
    val res = BindingsJsonParser.parse("res/test_bindings_json_parser.json")

    val expected = AppLibraryBindings(List(
      AppPrototypeBindings("app1", List(
        DeviceInstanceBinding("device1", BinarySensorBinding(BinarySensor.toString, 42)),
        DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 41)),
        DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 39)),
        DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 38))
      ))
    ))
    res shouldEqual expected
  }

  "writeToFile" should "write the correct json file on correct input" in {
    val expected = AppLibraryBindings(List(
      AppPrototypeBindings("app1", List(
        DeviceInstanceBinding("device1", BinarySensorBinding(BinarySensor.toString, 42)),
        DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 41)),
        DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 39)),
        DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 38))
      ))
    ))
    BindingsJsonParser.writeToFile("res/test_bindings_json_parser_write.json", expected)
    val res = BindingsJsonParser.parse("res/test_bindings_json_parser_write.json")
    res shouldEqual expected
  }
}
