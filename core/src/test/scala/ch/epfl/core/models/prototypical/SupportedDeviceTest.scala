package ch.epfl.core.models.prototypical

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SupportedDeviceTest extends AnyFlatSpec with Matchers {
  "fromString" should "return BinarySensor on String binary" in {
    SupportedDevice.fromString("binary") shouldEqual BinarySensor
  }
  "fromString" should "return BinarySensor on String Binary" in {
    SupportedDevice.fromString("Binary") shouldEqual BinarySensor
  }
  "fromString" should "return BinarySensor on String BINARY" in {
    SupportedDevice.fromString("BINARY") shouldEqual BinarySensor
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

  "fromString" should "throw an UnsupportedDevice for switchhhh" in {
    an [UnsupportedDeviceException] should be thrownBy  SupportedDevice.fromString("switchhhh")
  }

}
