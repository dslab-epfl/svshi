package ch.epfl.core.compiler.models

import java.time.{LocalDateTime, LocalTime}

trait KNXDatatype {
  def fromString(s: String): Option[KNXDatatype] = s match {
    case _ if s.startsWith("DPT-1.") => Some(DPT1(false))
  }
}

// 1.yyy = boolean
case class DPT1(value: Boolean) extends KNXDatatype {
  override def toString: String = "DPT-1.002"
}
// 2.yyy = 2 x boolean
case class DPT2(control: Boolean, value: Boolean) extends KNXDatatype {
  override def toString: String = "DPT-2.002"
}
// 3.yyy = boolean + 3-bit unsigned value (0 -> 7)
case class DPT3(control: Boolean, stepCode: Int)
  extends KNXDatatype {
  require(0 <= stepCode && stepCode <= 7)

  override def toString: String = "DPT-3.007"
}
// 5.yyy = 8-bit unsigned value
case class DPT5(value: Int) extends KNXDatatype {
  require(0 <= value && value <= 255)

  override def toString: String = "DPT-5.001"
}
// 6.yyy = 8-bit 2's complement (Scala Byte is a signed value)
case class DPT6(value: Byte) extends KNXDatatype {
  override def toString: String = "DPT-6.001"
}
// 7.yyy = 16-bit unsigned value
case class DPT7(value: Int) extends KNXDatatype {
  require(0 <= value && value <= 65535)

  override def toString: String = "DPT-7.002"
}
// 9.yyy = 16-bit float (in Scala Float is 32-bit, be careful)
case class DPT9(value: Float) extends KNXDatatype {
  override def toString: String = "DPT-9.001"
}
// 10.yyy = time
case class DPT10(value: LocalTime) extends KNXDatatype {
  override def toString: String = "DPT-10.001"
}
// 11.yyy = date (Hour of the day is included
case class DPT11(value: LocalDateTime) extends KNXDatatype {
  override def toString: String = "DPT-11.001"
}
// 12.yyy = 32-bit unsigned value
case class DPT12(value: Long) extends KNXDatatype {
  require(0 <= value && value <= 4294967295L)

  override def toString: String = "DPT-12.001"
}
// 13.yyy = 32-bit 2's complement
case class DPT13(value: Int) extends KNXDatatype {
  override def toString: String = "DPT-13.010"
}
// 14.yyy = 32-bit float
case class DPT14(value: Float) extends KNXDatatype {
  override def toString: String = "DPT-14.000"
}
// 16.yyy = string -> 14 characters (14 x 8-bit)
case class DPT16(value: String) extends KNXDatatype {
  require(value.length == 14)

  override def toString: String = "DPT-16.000"
}
// 17.yyy = scene number
case class DPT17(value: Byte) extends KNXDatatype {
  require(0 <= value && value <= 63)

  override def toString: String = "DPT-17.001"
}
// 18.yyy = scene control (!learn = activate)
case class DPT18(learn: Boolean, sceneNumber: Byte) extends KNXDatatype {
  require(0 <= sceneNumber && sceneNumber <= 63)

  override def toString: String = "DPT-18.001"
}
// 19.yyy = time + data
case class DPT19(value: LocalDateTime) extends KNXDatatype { // TODO
  override def toString: String = "DPT-19.001"
}
// 20.yyy = 8-bit enumeration
case class DPT20(value: Array[Boolean]) extends KNXDatatype {
  require(value.length <= 8)

  override def toString: String = "DPT-20.001"
}
