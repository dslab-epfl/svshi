package ch.epfl.core

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SvshiTest extends AnyFlatSpec with Matchers {
  "getAvailableProtoDevices" should "return the correct list" in {
    Svshi.getAvailableProtoDevices() should contain theSameElementsAs List("co2", "binary", "temperature", "switch", "humidity")
  }
}
