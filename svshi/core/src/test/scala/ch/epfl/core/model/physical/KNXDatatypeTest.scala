package ch.epfl.core.model.physical

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class KNXDatatypeTest extends AnyFlatSpec with Matchers {
  "The datatypeRegex regex" should "matches correct String 1" in {
    KNXDatatype.datatypeRegex.matches("DPT-1") shouldEqual true
  }
  "The datatypeRegex regex" should "matches correct String 2" in {
    KNXDatatype.datatypeRegex.matches("DPT-0001") shouldEqual true
  }
  "The datatypeRegex regex" should "matches correct String 3" in {
    KNXDatatype.datatypeRegex.matches("DPT-1123") shouldEqual true
  }
  "The datatypeRegex regex" should "matches correct String 4" in {
    KNXDatatype.datatypeRegex.matches("DPT-12") shouldEqual true
  }
  "The datatypeRegex regex" should "matches correct String 5" in {
    KNXDatatype.datatypeRegex.matches("DPT-13") shouldEqual true
  }
  "The fromString method" should "return correct datatype instance with correct String 1" in {
    KNXDatatype.fromString("DPT-1") shouldEqual Some(DPT1)
  }
  "The fromString method" should "return correct datatype instance with correct String 2" in {
    KNXDatatype.fromString("DPT-001") shouldEqual Some(DPT1)
  }
  "The fromString method" should "return correct datatype instance with correct String 3" in {
    KNXDatatype.fromString("DPT-13") shouldEqual Some(DPT13)
  }
  "The fromString method" should "return correct datatype instance with correct String 4" in {
    KNXDatatype.fromString("DPT-013") shouldEqual Some(DPT13)
  }
//  "The fromString method" should "return correct datatype instance with correct String 5" in {
//    KNXDatatype.fromString("DPT-2") shouldEqual Some(DPT2)
//  }
  "The fromString method" should "return correct datatype instance with correct String 6" in {
    KNXDatatype.fromString("DPT-000000020") shouldEqual Some(DPT20)
  }
  "The fromString method" should "return None with incorrect String 1" in {
    KNXDatatype.fromString("DPT-") shouldEqual None
  }
  "The fromString method" should "return None with incorrect String 2" in {
    KNXDatatype.fromString("") shouldEqual None
  }
  "The fromString method" should "return None with incorrect String 3" in {
    KNXDatatype.fromString("DPT123") shouldEqual None
  }
  "The fromString method" should "return None with incorrect String 4" in {
    KNXDatatype.fromString("DPT-123") shouldEqual None
  }
  "The fromString method" should "return None with incorrect String 5" in {
    KNXDatatype.fromString("DPT-000101") shouldEqual None
  }

}
