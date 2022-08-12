package ch.epfl.core.model.prototypical

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SupportedDeviceTest extends AnyFlatSpec with Matchers {
  "binarySensorString" should "be 'binary'" in {
    SupportedDevice.binarySensorString shouldEqual "binarySensor"
  }

  "switchString" should "be 'switch'" in {
    SupportedDevice.switchString shouldEqual "switch"
  }

  "temperatureSensor" should "be 'temperature'" in {
    SupportedDevice.temperatureSensorString shouldEqual "temperatureSensor"
  }

  "humiditySensor" should "be 'humidity'" in {
    SupportedDevice.humiditySensorString shouldEqual "humiditySensor"
  }

  "co2Sensor" should "be 'co2'" in {
    SupportedDevice.co2SensorString shouldEqual "co2Sensor"
  }

  "fromString" should "return BinarySensor on String binarySensor" in {
    SupportedDevice.fromString("binarySensor") shouldEqual BinarySensor
  }
  "fromString" should "return BinarySensor on String Binarysensor" in {
    SupportedDevice.fromString("Binarysensor") shouldEqual BinarySensor
  }
  "fromString" should "return BinarySensor on String BINARYSENSOR" in {
    SupportedDevice.fromString("BINARYSENSOR") shouldEqual BinarySensor
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

  "fromString" should "return TemperatureSensor on String temperatureSensor" in {
    SupportedDevice.fromString("temperatureSensor") shouldEqual TemperatureSensor
  }
  "fromString" should "return TemperatureSensor on String TemperatureSensor" in {
    SupportedDevice.fromString("TemperatureSensor") shouldEqual TemperatureSensor
  }
  "fromString" should "return TemperatureSensor on String TEMPERATURESENSOR" in {
    SupportedDevice.fromString("TEMPERATURESENSOR") shouldEqual TemperatureSensor
  }
  "fromString" should "return TemperatureSensor on String val in SupportedDevice" in {
    SupportedDevice.fromString(SupportedDevice.temperatureSensorString) shouldEqual TemperatureSensor
  }

  "fromString" should "return HumiditySensor on String humiditySensor" in {
    SupportedDevice.fromString("humiditySensor") shouldEqual HumiditySensor
  }
  "fromString" should "return HumiditySensor on String HumiditySensor" in {
    SupportedDevice.fromString("HumiditySensor") shouldEqual HumiditySensor
  }
  "fromString" should "return HumiditySensor on String HUMIDITYSENSOR" in {
    SupportedDevice.fromString("HUMIDITYSENSOR") shouldEqual HumiditySensor
  }
  "fromString" should "return HumiditySensor on String val in SupportedDevice" in {
    SupportedDevice.fromString(SupportedDevice.humiditySensorString) shouldEqual HumiditySensor
  }

  "fromString" should "return CO2Sensor on String co2Sensor" in {
    SupportedDevice.fromString("co2Sensor") shouldEqual CO2Sensor
  }
  "fromString" should "return CO2Sensor on String Co2Sensor" in {
    SupportedDevice.fromString("Co2Sensor") shouldEqual CO2Sensor
  }
  "fromString" should "return CO2Sensor on String CO2SENSOR" in {
    SupportedDevice.fromString("CO2SENSOR") shouldEqual CO2Sensor
  }
  "fromString" should "return CO2Sensor on String val in SupportedDevice" in {
    SupportedDevice.fromString(SupportedDevice.co2SensorString) shouldEqual CO2Sensor
  }

  "fromString" should "return DimmerSensor on String dimmerSensor" in {
    SupportedDevice.fromString("dimmerSensor") shouldEqual DimmerSensor
  }
  "fromString" should "return DimmerSensor on String DimmerSensor" in {
    SupportedDevice.fromString("DimmerSensor") shouldEqual DimmerSensor
  }
  "fromString" should "return DimmerSensor on String DIMMERSENSOR" in {
    SupportedDevice.fromString("DIMMERSENSOR") shouldEqual DimmerSensor
  }
  "fromString" should "return DimmerSensor on String dimmersensor" in {
    SupportedDevice.fromString("dimmersensor") shouldEqual DimmerSensor
  }
  "fromString" should "return DimmerSensor on String val in SupportedDevice" in {
    SupportedDevice.fromString(SupportedDevice.dimmerSensorString) shouldEqual DimmerSensor
  }

  "fromString" should "return DimmerActuator on String dimmerActuator" in {
    SupportedDevice.fromString("dimmerActuator") shouldEqual DimmerActuator
  }
  "fromString" should "return DimmerActuator on String DimmerActuator" in {
    SupportedDevice.fromString("DimmerActuator") shouldEqual DimmerActuator
  }
  "fromString" should "return DimmerActuator on String DIMMERACTUATOR" in {
    SupportedDevice.fromString("DIMMERACTUATOR") shouldEqual DimmerActuator
  }
  "fromString" should "return DimmerActuator on String dimmeractuator" in {
    SupportedDevice.fromString("dimmeractuator") shouldEqual DimmerActuator
  }
  "fromString" should "return DimmerActuator on String val in SupportedDevice" in {
    SupportedDevice.fromString(SupportedDevice.dimmerActuatorString) shouldEqual DimmerActuator
  }

  "fromString" should "throw an UnsupportedDevice for switchhhh" in {
    an[UnsupportedDeviceException] should be thrownBy SupportedDevice.fromString("switchhhh")
  }

  "getDeviceBinding" should "return the correct binding for BinarySensor" in {
    SupportedDevice.getDeviceBinding(AppPrototypicalDeviceInstance("name", BinarySensor)) shouldEqual BinarySensorBinding(BinarySensor.toString, SupportedDevice.defaultPhysicalId)
  }
  "getDeviceBinding" should "return the correct binding for BinarySensor with prebinding" in {
    SupportedDevice.getDeviceBinding(AppPrototypicalDeviceInstance("name", BinarySensor, 42)) shouldEqual BinarySensorBinding(BinarySensor.toString, 42)
  }
  "getDeviceBinding" should "return the correct binding for HumiditySensor" in {
    SupportedDevice.getDeviceBinding(AppPrototypicalDeviceInstance("name", HumiditySensor)) shouldEqual HumiditySensorBinding(
      HumiditySensor.toString,
      SupportedDevice.defaultPhysicalId
    )
  }
  "getDeviceBinding" should "return the correct binding for HumiditySensor with prebindings" in {
    SupportedDevice.getDeviceBinding(AppPrototypicalDeviceInstance("name", HumiditySensor, 1234)) shouldEqual HumiditySensorBinding(
      HumiditySensor.toString,
      1234
    )
  }
  "getDeviceBinding" should "return the correct binding for TemperatureSensor" in {
    SupportedDevice.getDeviceBinding(AppPrototypicalDeviceInstance("name", TemperatureSensor)) shouldEqual TemperatureSensorBinding(
      TemperatureSensor.toString,
      SupportedDevice.defaultPhysicalId
    )
  }
  "getDeviceBinding" should "return the correct binding for TemperatureSensor with prebindings" in {
    SupportedDevice.getDeviceBinding(AppPrototypicalDeviceInstance("name", TemperatureSensor, 456)) shouldEqual TemperatureSensorBinding(
      TemperatureSensor.toString,
      456
    )
  }
  "getDeviceBinding" should "return the correct binding for Switch" in {
    SupportedDevice.getDeviceBinding(AppPrototypicalDeviceInstance("name", Switch)) shouldEqual SwitchBinding(Switch.toString, SupportedDevice.defaultPhysicalId)
  }
  "getDeviceBinding" should "return the correct binding for Switch with prebindings" in {
    SupportedDevice.getDeviceBinding(AppPrototypicalDeviceInstance("name", Switch, 123)) shouldEqual SwitchBinding(Switch.toString, 123)
  }
  "getDeviceBinding" should "return the correct binding for CO2Sensor" in {
    SupportedDevice.getDeviceBinding(AppPrototypicalDeviceInstance("name", CO2Sensor)) shouldEqual CO2SensorBinding(CO2Sensor.toString, SupportedDevice.defaultPhysicalId)
  }
  "getDeviceBinding" should "return the correct binding for CO2Sensor with prebindings" in {
    SupportedDevice.getDeviceBinding(AppPrototypicalDeviceInstance("name", CO2Sensor, -1234)) shouldEqual CO2SensorBinding(CO2Sensor.toString, -1234)
  }
  "getDeviceBinding" should "return the correct binding for DimmerSensor" in {
    SupportedDevice.getDeviceBinding(AppPrototypicalDeviceInstance("name", DimmerSensor)) shouldEqual DimmerSensorBinding(DimmerSensor.toString, SupportedDevice.defaultPhysicalId)
  }
  "getDeviceBinding" should "return the correct binding for DimmerSensor with prebindings" in {
    SupportedDevice.getDeviceBinding(AppPrototypicalDeviceInstance("name", DimmerSensor, -789)) shouldEqual DimmerSensorBinding(DimmerSensor.toString, -789)
  }

  "getDeviceBinding" should "return the correct binding for DimmerActuator" in {
    SupportedDevice.getDeviceBinding(AppPrototypicalDeviceInstance("name", DimmerActuator)) shouldEqual DimmerActuatorBinding(
      DimmerActuator.toString,
      SupportedDevice.defaultPhysicalId
    )
  }
  "getDeviceBinding" should "return the correct binding for DimmerActuator with prebindings" in {
    SupportedDevice.getDeviceBinding(AppPrototypicalDeviceInstance("name", DimmerActuator, -456)) shouldEqual DimmerActuatorBinding(
      DimmerActuator.toString,
      -456
    )
  }

}
