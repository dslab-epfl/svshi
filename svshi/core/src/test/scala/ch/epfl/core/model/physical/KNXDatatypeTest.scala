package ch.epfl.core.model.physical

import ch.epfl.core.model.python._
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

  "The fromDPTSize method" should "return DPT1 if the size is one bit" in {
    KNXDatatype.fromDPTSize("1 bit") shouldEqual Some(DPT1)
    KNXDatatype.fromDPTSize("one bit") shouldEqual Some(DPT1)
  }

  "The fromDPTSize method" should "return None if the size contains bit but is not one" in {
    KNXDatatype.fromDPTSize("2 bit") shouldEqual None
    KNXDatatype.fromDPTSize("bit") shouldEqual None
  }

  "The fromDPTSize method" should "return None if the size does not contain bit" in {
    KNXDatatype.fromDPTSize("2 bytes") shouldEqual None
    KNXDatatype.fromDPTSize("String") shouldEqual None
  }

  "DPT1 toPythonType" should "return PythonBool" in {
    DPT1.toPythonType shouldEqual PythonBool
  }

  "DPT5 toPythonType" should "return PythonInt" in {
    DPT5.toPythonType shouldEqual PythonInt
  }

  "DPT6 toPythonType" should "return PythonInt" in {
    DPT6.toPythonType shouldEqual PythonInt
  }

  "DPT7 toPythonType" should "return PythonInt" in {
    DPT7.toPythonType shouldEqual PythonInt
  }

  "DPT9 toPythonType" should "return PythonFloat" in {
    DPT9.toPythonType shouldEqual PythonFloat
  }

  "DPT10 toPythonType" should "return PythonInt" in {
    DPT10.toPythonType shouldEqual PythonInt
  }

  "DPT11 toPythonType" should "return PythonInt" in {
    DPT11.toPythonType shouldEqual PythonInt
  }

  "DPT12 toPythonType" should "return PythonInt" in {
    DPT12.toPythonType shouldEqual PythonInt
  }

  "DPT13 toPythonType" should "return PythonInt" in {
    DPT13.toPythonType shouldEqual PythonInt
  }

  "DPT14 toPythonType" should "return PythonFloat" in {
    DPT14.toPythonType shouldEqual PythonFloat
  }

  "DPT16 toPythonType" should "return PythonInt" in {
    DPT16.toPythonType shouldEqual PythonInt
  }

  "DPT17 toPythonType" should "return PythonInt" in {
    DPT17.toPythonType shouldEqual PythonInt
  }

  "DPT18 toPythonType" should "return PythonInt" in {
    DPT18.toPythonType shouldEqual PythonInt
  }

  "DPT19 toPythonType" should "return PythonInt" in {
    DPT19.toPythonType shouldEqual PythonInt
  }

  "DPT20 toPythonType" should "return PythonInt" in {
    DPT20.toPythonType shouldEqual PythonInt
  }

  "DPTUnknown toPythonType" should "return PythonInt" in {
    DPTUnknown.toPythonType shouldEqual PythonInt
  }
}
