package ch.epfl.core

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SvshiTest extends AnyFlatSpec with Matchers {
  "getAvailableProtoDevices" should "return the correct list" in {
    Svshi.getAvailableProtoDevices() should contain theSameElementsAs List(
      "co2Sensor",
      "binarySensor",
      "temperatureSensor",
      "switch",
      "humiditySensor",
      "dimmerSensor",
      "dimmerActuator"
    )
  }
  "getAvailableDpts" should "return the correct list" in {
    Svshi.getAvailableDpts() should contain theSameElementsAs List(
      "DPT-1",
      "DPT-5",
      "DPT-6",
      "DPT-7",
      "DPT-9",
      "DPT-10",
      "DPT-11",
      "DPT-12",
      "DPT-13",
      "DPT-14",
      "DPT-16",
      "DPT-17",
      "DPT-18",
      "DPT-19",
      "DPT-20"
    )
  }
}
