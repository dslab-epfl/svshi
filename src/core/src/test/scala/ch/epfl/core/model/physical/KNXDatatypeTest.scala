package ch.epfl.core.model.physical

import ch.epfl.core.model.python._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class KNXDatatypeTest extends AnyFlatSpec with Matchers {
  "The datatypeRegex regex" should "matches correct String 1" in {
    KNXDatatype.simpleDatatypeRegex.matches("DPT-1") shouldEqual true
  }

  "The datatypeRegex regex" should "matches correct String 2" in {
    KNXDatatype.simpleDatatypeRegex.matches("DPT-0001") shouldEqual true
  }

  "The datatypeRegex regex" should "matches correct String 3" in {
    KNXDatatype.simpleDatatypeRegex.matches("DPT-1123") shouldEqual true
  }

  "The datatypeRegex regex" should "matches correct String 4" in {
    KNXDatatype.simpleDatatypeRegex.matches("DPT-12") shouldEqual true
  }

  "The datatypeRegex regex" should "matches correct String 5" in {
    KNXDatatype.simpleDatatypeRegex.matches("DPT-13") shouldEqual true
  }
  "The datatypeRegex regex" should "matches correct String 6" in {
    KNXDatatype.simpleDatatypeRegex.matches("DPST-1") shouldEqual true
  }
  "The datatypeRegex regex" should "matches correct String 7" in {
    KNXDatatype.simpleDatatypeRegex.matches("DPST-13") shouldEqual true
  }
  "The datatypeRegex regex" should "matches correct String 8" in {
    KNXDatatype.simpleDatatypeRegex.matches("DPST-12") shouldEqual true
  }
  "The datatypeRegex regex" should "matches correct String 9" in {
    KNXDatatype.simpleDatatypeRegex.matches("DPST-0012") shouldEqual true
  }
  "The datatypeRegex regex" should "matches correct String 10" in {
    KNXDatatype.simpleDatatypeRegex.matches("DPST-14") shouldEqual true
  }

  "The fromString method" should "return correct datatype instance with correct String DPT-Unknown" in {
    KNXDatatype.fromString("DPT-Unknown") shouldEqual Some(DPTUnknown(-1))
  }

  "The fromString method" should "return correct datatype instance with correct String 1" in {
    KNXDatatype.fromString("DPT-1") shouldEqual Some(DPT1(-1))
  }

  "The fromString method" should "return correct datatype instance with correct String 2" in {
    KNXDatatype.fromString("DPT-001") shouldEqual Some(DPT1(-1))
  }

  "The fromString method" should "return correct datatype instance with correct String 3" in {
    KNXDatatype.fromString("DPT-13") shouldEqual Some(DPT13(-1))
  }

  "The fromString method" should "return correct datatype instance with correct String 4" in {
    KNXDatatype.fromString("DPT-013") shouldEqual Some(DPT13(-1))
  }

  "The fromString method" should "return correct datatype instance with correct String 6" in {
    KNXDatatype.fromString("DPT-000000020") shouldEqual Some(DPT20(-1))
  }

  "The fromString method" should "return None with incorrect String 1" in {
    KNXDatatype.fromString("DPT-") shouldEqual None
  }

  "The fromString method" should "return DPTUnknown with empty String" in {
    KNXDatatype.fromString("") shouldEqual Some(DPTUnknown(-1))
  }

  "The fromString method" should "return None with incorrect String 3" in {
    KNXDatatype.fromString("DPT123") shouldEqual None
  }

  "The fromString method" should "return DPTUnknown(-1) with unknown dpt number 1" in {
    KNXDatatype.fromString("DPT-123") shouldEqual Some(DPTUnknown(-1))
  }

  "The fromString method" should "return DPTUnknown(-1) with unknown dpt number 2" in {
    KNXDatatype.fromString("DPT-000101") shouldEqual Some(DPTUnknown(-1))
  }

  "The fromString method" should "return correct datatype instance with correct String 7" in {
    KNXDatatype.fromString("DPST-1") shouldEqual Some(DPT1(-1))
  }

  "The fromString method" should "return correct datatype instance with correct String 8" in {
    KNXDatatype.fromString("DPST-001") shouldEqual Some(DPT1(-1))
  }

  "The fromString method" should "return correct datatype instance with correct String 9" in {
    KNXDatatype.fromString("DPST-13") shouldEqual Some(DPT13(-1))
  }

  "The fromString method" should "return correct datatype instance with correct String 10" in {
    KNXDatatype.fromString("DPST-013") shouldEqual Some(DPT13(-1))
  }

  "The fromString method" should "return correct datatype instance with correct String 11" in {
    KNXDatatype.fromString("DPST-000000020") shouldEqual Some(DPT20(-1))
  }

  "The fromString method" should "return None with incorrect String 6" in {
    KNXDatatype.fromString("DPST-") shouldEqual None
  }

  "The fromString method" should "return None with incorrect String 8" in {
    KNXDatatype.fromString("DPST123") shouldEqual None
  }

  "The fromString method" should "return DPTUnknown(-1) with unknown dpt number 3" in {
    KNXDatatype.fromString("DPST-123") shouldEqual Some(DPTUnknown(-1))
  }

  "The fromString method" should "return DPTUnknown(-1) with unknown dpt number 4" in {
    KNXDatatype.fromString("DPST-000101") shouldEqual Some(DPTUnknown(-1))
  }

  "The fromString method" should "return correct datatype instance with correct String 12" in {
    KNXDatatype.fromString("DPST-1-1") shouldEqual Some(DPT1(1))
  }

  "The fromString method" should "return correct datatype instance with correct String 13" in {
    KNXDatatype.fromString("DPST-001-004") shouldEqual Some(DPT1(4))
  }

  "The fromString method" should "return correct datatype instance with correct String 14" in {
    KNXDatatype.fromString("DPST-13-005") shouldEqual Some(DPT13(5))
  }

  "The fromString method" should "return correct datatype instance with correct String 15" in {
    KNXDatatype.fromString("DPST-013-4") shouldEqual Some(DPT13(4))
  }

  "The fromString method" should "return correct datatype instance with correct String 16" in {
    KNXDatatype.fromString("DPST-000000020-9") shouldEqual Some(DPT20(9))
  }

  "The fromString method" should "return correct datatype instance with correct String 17" in {
    KNXDatatype.fromString("DPT-1-1") shouldEqual Some(DPT1(1))
  }

  "The fromString method" should "return correct datatype instance with correct String 18" in {
    KNXDatatype.fromString("DPT-001-004") shouldEqual Some(DPT1(4))
  }

  "The fromString method" should "return correct datatype instance with correct String 19" in {
    KNXDatatype.fromString("DPT-13-005") shouldEqual Some(DPT13(5))
  }

  "The fromString method" should "return correct datatype instance with correct String 20" in {
    KNXDatatype.fromString("DPT-013-4") shouldEqual Some(DPT13(4))
  }

  "The fromString method" should "return correct datatype instance with correct String 21" in {
    KNXDatatype.fromString("DPT-000000020-9") shouldEqual Some(DPT20(9))
  }

  "The fromString method" should "return None with incorrect String 11" in {
    KNXDatatype.fromString("DPST--") shouldEqual None
  }

  "The fromString method" should "return None with incorrect String 13" in {
    KNXDatatype.fromString("DPST123-1") shouldEqual None
  }

  "The fromString method" should "return DPTUnknown(-1) with unknown dpt number 6" in {
    KNXDatatype.fromString("DPST-123-2") shouldEqual Some(DPTUnknown(-1))
  }

  "The fromString method" should "return DPTUnknown(-1) with unknown dpt number 7" in {
    KNXDatatype.fromString("DPST-000101-4") shouldEqual Some(DPTUnknown(-1))
  }

  "The fromDPTSize method" should "return DPT1 if the size is one bit" in {
    KNXDatatype.fromDPTSize("1 bit") shouldEqual Some(DPT1(-1))
    KNXDatatype.fromDPTSize("one bit") shouldEqual Some(DPT1(-1))
  }

  "The fromDPTSize method" should "return None if the size contains bit but is not one" in {
    KNXDatatype.fromDPTSize("2 bit") shouldEqual None
    KNXDatatype.fromDPTSize("bit") shouldEqual None
  }

  "The fromDPTSize method" should "return None if the size does not contain bit" in {
    KNXDatatype.fromDPTSize("2 bytes") shouldEqual None
    KNXDatatype.fromDPTSize("String") shouldEqual None
  }

  "DPT1(-1).toString" should "return DPT-1" in {
    DPT1(-1).toString shouldEqual "DPT-1"
  }
  "DPT1(3).toString" should "return DPT-1-3" in {
    DPT1(3).toString shouldEqual "DPT-1-3"
  }
  "DPT5(2).toString" should "return DPT-5-2" in {
    DPT5(2).toString shouldEqual "DPT-5-2"
  }
  "DPT6(9).toString" should "return DPT-6-2" in {
    DPT6(9).toString shouldEqual "DPT-6-9"
  }
  "DPT20(2).toString" should "return DPT-20-2" in {
    DPT20(2).toString shouldEqual "DPT-20-2"
  }

  "DPT1 toPythonType" should "return PythonBool" in {
    DPT1(-1).toPythonType shouldEqual PythonBool
  }

  "DPT5 toPythonType" should "return PythonInt" in {
    DPT5(-1).toPythonType shouldEqual PythonInt
  }

  "DPT6 toPythonType" should "return PythonInt" in {
    DPT6(-1).toPythonType shouldEqual PythonInt
  }

  "DPT7 toPythonType" should "return PythonInt" in {
    DPT7(-1).toPythonType shouldEqual PythonInt
  }

  "DPT9 toPythonType" should "return PythonFloat" in {
    DPT9(-1).toPythonType shouldEqual PythonFloat
  }

  "DPT10 toPythonType" should "return PythonInt" in {
    DPT10(-1).toPythonType shouldEqual PythonInt
  }

  "DPT11 toPythonType" should "return PythonInt" in {
    DPT11(-1).toPythonType shouldEqual PythonInt
  }

  "DPT12 toPythonType" should "return PythonInt" in {
    DPT12(-1).toPythonType shouldEqual PythonInt
  }

  "DPT13 toPythonType" should "return PythonInt" in {
    DPT13(-1).toPythonType shouldEqual PythonInt
  }

  "DPT14 toPythonType" should "return PythonFloat" in {
    DPT14(-1).toPythonType shouldEqual PythonFloat
  }

  "DPT16 toPythonType" should "return PythonInt" in {
    DPT16(-1).toPythonType shouldEqual PythonInt
  }

  "DPT17 toPythonType" should "return PythonInt" in {
    DPT17(-1).toPythonType shouldEqual PythonInt
  }

  "DPT18 toPythonType" should "return PythonInt" in {
    DPT18(-1).toPythonType shouldEqual PythonInt
  }

  "DPT19 toPythonType" should "return PythonInt" in {
    DPT19(-1).toPythonType shouldEqual PythonInt
  }

  "DPT20 toPythonType" should "return PythonInt" in {
    DPT20(-1).toPythonType shouldEqual PythonInt
  }

  "DPTUnknown toPythonType" should "return PythonInt" in {
    DPTUnknown(-1).toPythonType shouldEqual PythonInt
  }

  "DPT1 similarTo" should "be true only for DPT1 with any sub" in {
    val d = DPT1(-1)
    d.similarTo(DPT1(-1)) shouldBe true
    d.similarTo(DPT1(12)) shouldBe true
    d.similarTo(DPT1(32)) shouldBe true
    d.similarTo(DPT5(32)) shouldBe false
    d.similarTo(DPT6(32)) shouldBe false
    d.similarTo(DPT7(32)) shouldBe false
    d.similarTo(DPT7(-1)) shouldBe false
    d.similarTo(DPT18(32)) shouldBe false
    d.similarTo(DPT9(32)) shouldBe false
    d.similarTo(DPT10(32)) shouldBe false
    d.similarTo(DPT11(32)) shouldBe false
    d.similarTo(DPT12(32)) shouldBe false
    d.similarTo(DPT13(32)) shouldBe false
    d.similarTo(DPT14(32)) shouldBe false
    d.similarTo(DPT16(32)) shouldBe false
    d.similarTo(DPT17(32)) shouldBe false
    d.similarTo(DPT18(32)) shouldBe false
    d.similarTo(DPT19(32)) shouldBe false
    d.similarTo(DPT20(32)) shouldBe false
  }
}
