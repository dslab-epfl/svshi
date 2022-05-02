package ch.epfl.core.model.prototypical

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SupportedDeviceTest extends AnyFlatSpec with Matchers {
  "binarySensorString" should "be 'binary'" in {
    SupportedDevice.binarySensorString shouldEqual "binary"
  }

  "switchString" should "be 'switch'" in {
    SupportedDevice.switchString shouldEqual "switch"
  }

  "temperatureSensor" should "be 'temperature'" in {
    SupportedDevice.temperatureSensorString shouldEqual "temperature"
  }

  "humiditySensor" should "be 'humidity'" in {
    SupportedDevice.humiditySensorString shouldEqual "humidity"
  }

  "co2Sensor" should "be 'co2'" in {
    SupportedDevice.co2SensorString shouldEqual "co2"
  }

  "fromString" should "return BinarySensor on String binary" in {
    SupportedDevice.fromString("binary") shouldEqual BinarySensor
  }
  "fromString" should "return BinarySensor on String Binary" in {
    SupportedDevice.fromString("Binary") shouldEqual BinarySensor
  }
  "fromString" should "return BinarySensor on String BINARY" in {
    SupportedDevice.fromString("BINARY") shouldEqual BinarySensor
  }
  "fromString" should "return BinarySensor on String val in SupportedDevice" in {
    SupportedDevice.fromString(SupportedDevice.binarySensorString) shouldEqual BinarySensor
  }

  "fromString" should "return Switch on String switch" in {
    SupportedDevice.fromString("switch") shouldEqual Switch
  }
  "fromString" should "return Switch on String Switch" in {
    SupportedDevice.fromString("Switch") shouldEqual Switch
  }
  "fromString" should "return Switch on String SWITCH" in {
    SupportedDevice.fromString("SWITCH") shouldEqual Switch
  }
  "fromString" should "return Switch on String val in SupportedDevice" in {
    SupportedDevice.fromString(SupportedDevice.switchString) shouldEqual Switch
  }

  "fromString" should "return TemperatureSensor on String temperature" in {
    SupportedDevice.fromString("temperature") shouldEqual TemperatureSensor
  }
  "fromString" should "return TemperatureSensor on String Temperature" in {
    SupportedDevice.fromString("Temperature") shouldEqual TemperatureSensor
  }
  "fromString" should "return TemperatureSensor on String TEMPERATURE" in {
    SupportedDevice.fromString("TEMPERATURE") shouldEqual TemperatureSensor
  }
  "fromString" should "return TemperatureSensor on String val in SupportedDevice" in {
    SupportedDevice.fromString(SupportedDevice.temperatureSensorString) shouldEqual TemperatureSensor
  }

  "fromString" should "return HumiditySensor on String humidity" in {
    SupportedDevice.fromString("humidity") shouldEqual HumiditySensor
  }
  "fromString" should "return HumiditySensor on String Humidity" in {
    SupportedDevice.fromString("Humidity") shouldEqual HumiditySensor
  }
  "fromString" should "return HumiditySensor on String HUMIDITY" in {
    SupportedDevice.fromString("HUMIDITY") shouldEqual HumiditySensor
  }
  "fromString" should "return HumiditySensor on String val in SupportedDevice" in {
    SupportedDevice.fromString(SupportedDevice.humiditySensorString) shouldEqual HumiditySensor
  }

  "fromString" should "return CO2Sensor on String co2" in {
    SupportedDevice.fromString("co2") shouldEqual CO2Sensor
  }
  "fromString" should "return CO2Sensor on String Co2" in {
    SupportedDevice.fromString("Co2") shouldEqual CO2Sensor
  }
  "fromString" should "return CO2Sensor on String CO2" in {
    SupportedDevice.fromString("CO2") shouldEqual CO2Sensor
  }
  "fromString" should "return CO2Sensor on String val in SupportedDevice" in {
    SupportedDevice.fromString(SupportedDevice.co2SensorString) shouldEqual CO2Sensor
  }

  "fromString" should "throw an UnsupportedDevice for switchhhh" in {
    an[MatchError] should be thrownBy SupportedDevice.fromString("switchhhh")
  }

  "getDeviceBinding" should "return the correct binding for BinarySensor" in {
    SupportedDevice.getDeviceBinding(BinarySensor) shouldEqual BinarySensorBinding(BinarySensor.toString, SupportedDevice.defaultPhysicalId)
  }
  "getDeviceBinding" should "return the correct binding for HumiditySensor" in {
    SupportedDevice.getDeviceBinding(HumiditySensor) shouldEqual HumiditySensorBinding(HumiditySensor.toString, SupportedDevice.defaultPhysicalId)
  }
  "getDeviceBinding" should "return the correct binding for TemperatureSensor" in {
    SupportedDevice.getDeviceBinding(TemperatureSensor) shouldEqual TemperatureSensorBinding(TemperatureSensor.toString, SupportedDevice.defaultPhysicalId)
  }
  "getDeviceBinding" should "return the correct binding for Switch" in {
    SupportedDevice.getDeviceBinding(Switch) shouldEqual SwitchBinding(Switch.toString, SupportedDevice.defaultPhysicalId)
  }
  "getDeviceBinding" should "return the correct binding for CO2Sensor" in {
    SupportedDevice.getDeviceBinding(CO2Sensor) shouldEqual CO2SensorBinding(CO2Sensor.toString, SupportedDevice.defaultPhysicalId)
  }

}
