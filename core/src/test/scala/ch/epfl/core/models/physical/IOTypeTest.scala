package ch.epfl.core.models.physical

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class IOTypeTest extends AnyFlatSpec with Matchers {
  "The fromString method" should "return In with valid String" in {
    IOType.fromString("in") shouldEqual Some(In)
  }
  "The fromString method" should "return Out with valid String" in {
    IOType.fromString("out") shouldEqual Some(Out)
  }
  "The fromString method" should "return InOut with valid String" in {
    IOType.fromString("in/out") shouldEqual Some(InOut)
  }
  "The fromString method" should "return Unknown with valid String" in {
    IOType.fromString("unknown") shouldEqual Some(Unknown)
  }
  "The fromString method" should "return In with valid capitalized String" in {
    IOType.fromString("In") shouldEqual Some(In)
  }
  "The fromString method" should "return Out with valid capitalized String" in {
    IOType.fromString("Out") shouldEqual Some(Out)
  }
  "The fromString method" should "return InOut with valid capitalized String" in {
    IOType.fromString("In/Out") shouldEqual Some(InOut)
  }
  "The fromString method" should "return Unknown with valid capitalized String" in {
    IOType.fromString("Unknown") shouldEqual Some(Unknown)
  }
  "The fromString method" should "return None for invalid String 1" in {
    IOType.fromString("fdas") shouldEqual None
  }
  "The fromString method" should "return None for invalid String 2" in {
    IOType.fromString("") shouldEqual None
  }
  "The fromString method" should "return None for invalid String 4" in {
    IOType.fromString("Inn") shouldEqual None
  }
}