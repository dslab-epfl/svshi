package ch.epfl.core.model.physical

import ch.epfl.core.model.python.{PythonBool, PythonFloat, PythonInt, PythonType}

import scala.util.matching.Regex

/** The datatypes used in the KNX protocol
  *
  *  The sub represents the 2nd part of the DPT (YYY below). It is optional, -1 is used to represent neutral DPT
  * Structure:
  *    data type: format + encoding
  *    size: value range + unit
  *    Notation: X.YYY
  *    X: defines format + encoding
  *    YYY: defines value range + unit
  *    The most often used Datapoint Types are:
  *    1.yyy = boolean, like switching, move up/down, step
  *    2.yyy = 2 x boolean, e.g. switching + priority control
  *    3.yyy = boolean + 3-bit unsigned value, e.g. dimming up/down
  *    4.yyy = character (8-bit)
  *    5.yyy = 8-bit unsigned value, like dim value (0..100%), blinds position (0..100%)
  *    6.yyy = 8-bit 2's complement, e.g. %
  *    7.yyy = 2 x 8-bit unsigned value, i.e. pulse counter
  *    8.yyy = 2 x 8-bit 2's complement, e.g. %
  *    9.yyy = 16-bit float, e.g. temperature
  *    10.yyy = time
  *    11.yyy = date
  *    12.yyy = 4 x 8-bit unsigned value, i.e. pulse counter
  *    13.yyy = 4 x 8-bit 2's complement, i.e. pulse counter
  *    14.yyy = 32-bit float, e.g. temperature
  *    15.yyy = access control
  *    16.yyy = string -> 14 characters (14 x 8-bit)
  *    17.yyy = scene number
  *    18.yyy = scene control
  *    19.yyy = time + data
  *    20.yyy = 8-bit enumeration, e.g. HVAC mode ('auto', 'comfort', 'standby', 'economy', 'protection')
  */
sealed abstract class KNXDatatype(sub: Int) {
  def toPythonType: PythonType
  def similarTo(other: KNXDatatype): Boolean
}
object KNXDatatype {
  def completeDatatypeRegexNoUnknown: Regex = "(DPT-[0-9]+-[0-9]+)|(DPST-[0-9]+-[0-9]+)".r
  def simpleDatatypeRegex: Regex = "(DPT-[0-9]+)|(DPST-[0-9]+)|DPT-Unknown".r
  def fromString(s: String): Option[KNXDatatype] = if (s.isEmpty) Some(DPTUnknown(-1))
  else if (completeDatatypeRegexNoUnknown.findFirstIn(s).isDefined) {
    val str = completeDatatypeRegexNoUnknown.findFirstIn(s).get
    str match {
      case _ if str.split("-").apply(1).toInt == 1  => Some(DPT1(str.split("-").apply(2).toInt))
      case _ if str.split("-").apply(1).toInt == 5  => Some(DPT5(str.split("-").apply(2).toInt))
      case _ if str.split("-").apply(1).toInt == 6  => Some(DPT6(str.split("-").apply(2).toInt))
      case _ if str.split("-").apply(1).toInt == 7  => Some(DPT7(str.split("-").apply(2).toInt))
      case _ if str.split("-").apply(1).toInt == 9  => Some(DPT9(str.split("-").apply(2).toInt))
      case _ if str.split("-").apply(1).toInt == 10 => Some(DPT10(str.split("-").apply(2).toInt))
      case _ if str.split("-").apply(1).toInt == 11 => Some(DPT11(str.split("-").apply(2).toInt))
      case _ if str.split("-").apply(1).toInt == 12 => Some(DPT12(str.split("-").apply(2).toInt))
      case _ if str.split("-").apply(1).toInt == 13 => Some(DPT13(str.split("-").apply(2).toInt))
      case _ if str.split("-").apply(1).toInt == 14 => Some(DPT14(str.split("-").apply(2).toInt))
      case _ if str.split("-").apply(1).toInt == 16 => Some(DPT16(str.split("-").apply(2).toInt))
      case _ if str.split("-").apply(1).toInt == 17 => Some(DPT17(str.split("-").apply(2).toInt))
      case _ if str.split("-").apply(1).toInt == 18 => Some(DPT18(str.split("-").apply(2).toInt))
      case _ if str.split("-").apply(1).toInt == 19 => Some(DPT19(str.split("-").apply(2).toInt))
      case _ if str.split("-").apply(1).toInt == 20 => Some(DPT20(str.split("-").apply(2).toInt))
      case _                                        => Some(DPTUnknown(-1))
    }
  } else if (simpleDatatypeRegex.findFirstIn(s).isDefined) {
    val str = simpleDatatypeRegex.findFirstIn(s).get
    str match {
      case _ if str.split("-").apply(1) == "Unknown" => Some(DPTUnknown(-1))
      case _ if str.split("-").apply(1).toInt == 1   => Some(DPT1(-1))
      case _ if str.split("-").apply(1).toInt == 5   => Some(DPT5(-1))
      case _ if str.split("-").apply(1).toInt == 6   => Some(DPT6(-1))
      case _ if str.split("-").apply(1).toInt == 7   => Some(DPT7(-1))
      case _ if str.split("-").apply(1).toInt == 9   => Some(DPT9(-1))
      case _ if str.split("-").apply(1).toInt == 10  => Some(DPT10(-1))
      case _ if str.split("-").apply(1).toInt == 11  => Some(DPT11(-1))
      case _ if str.split("-").apply(1).toInt == 12  => Some(DPT12(-1))
      case _ if str.split("-").apply(1).toInt == 13  => Some(DPT13(-1))
      case _ if str.split("-").apply(1).toInt == 14  => Some(DPT14(-1))
      case _ if str.split("-").apply(1).toInt == 16  => Some(DPT16(-1))
      case _ if str.split("-").apply(1).toInt == 17  => Some(DPT17(-1))
      case _ if str.split("-").apply(1).toInt == 18  => Some(DPT18(-1))
      case _ if str.split("-").apply(1).toInt == 19  => Some(DPT19(-1))
      case _ if str.split("-").apply(1).toInt == 20  => Some(DPT20(-1))
      case _                                         => Some(DPTUnknown(-1))
    }
  } else None

  /** Returns a DPT given the object size string from the ETS project.
    * BE CAREFUL IF ADDING NEW PARSED VALUES: TYPES OTHER THAN 1 OR 2 BITS CAN CORRESPOND TO MULTIPLE
    * DIFFERENT DPTS (UNSIGNED, TWO'2 COMPLEMENT, ...)
    * @param dptSize
    * @return
    */
  def fromDPTSize(dptSize: String): Option[KNXDatatype] = {
    if (dptSize.toLowerCase.contains("bit")) {
      if (dptSize.contains("1 ") || dptSize.toLowerCase.contains("one")) {
        Some(DPT1(-1))
      } else {
        None
      }
    } else {
      None
    }
  }

  /** Returns the list of all defined KNXDatatype without DPTUnknown
    * @return
    */
  def availableDpts: List[KNXDatatype] = {
    List(DPT1(-1), DPT5(-1), DPT6(-1), DPT7(-1), DPT9(-1), DPT10(-1), DPT11(-1), DPT12(-1), DPT13(-1), DPT14(-1), DPT16(-1), DPT17(-1), DPT18(-1), DPT19(-1), DPT20(-1))
  }
}
case class DPT1(sub: Int) extends KNXDatatype(sub) {
  override def toString = if (sub == -1) "DPT-1" else s"DPT-1-$sub"
  override def toPythonType: PythonType = PythonBool
  override def similarTo(other: KNXDatatype): Boolean = other match {
    case DPT1(_) => true
    case _       => false
  }
}

// These two are currently not supported by the Python runtime because they are not supported by XKNX.
// To use them, python code needs to be added to support conversion

//case object DPT2 extends KNXDatatype {
//  override def toString = "DPT-2"
//  override def toPythonType: PythonType = PythonInt
//}
//case object DPT3 extends KNXDatatype {
//  override def toString = "DPT-3"
//  override def toPythonType: PythonType = PythonInt
//}

case class DPT5(sub: Int) extends KNXDatatype(sub) {
  override def toString = if (sub == -1) "DPT-5" else s"DPT-5-$sub"
  override def toPythonType: PythonType = PythonInt
  override def similarTo(other: KNXDatatype): Boolean = other match {
    case DPT5(_) => true
    case _       => false
  }
}
case class DPT6(sub: Int) extends KNXDatatype(sub) {
  override def toString = if (sub == -1) "DPT-6" else s"DPT-6-$sub"
  override def toPythonType: PythonType = PythonInt
  override def similarTo(other: KNXDatatype): Boolean = other match {
    case DPT6(_) => true
    case _       => false
  }
}
case class DPT7(sub: Int) extends KNXDatatype(sub) {
  override def toString = if (sub == -1) "DPT-7" else s"DPT-7-$sub"
  override def toPythonType: PythonType = PythonInt
  override def similarTo(other: KNXDatatype): Boolean = other match {
    case DPT7(_) => true
    case _       => false
  }
}
case class DPT9(sub: Int) extends KNXDatatype(sub) {
  override def toString = if (sub == -1) "DPT-9" else s"DPT-9-$sub"
  override def toPythonType: PythonType = PythonFloat
  override def similarTo(other: KNXDatatype): Boolean = other match {
    case DPT9(_) => true
    case _       => false
  }
}
case class DPT10(sub: Int) extends KNXDatatype(sub) {
  override def toString = if (sub == -1) "DPT-10" else s"DPT-10-$sub"
  override def toPythonType: PythonType = PythonInt
  override def similarTo(other: KNXDatatype): Boolean = other match {
    case DPT10(_) => true
    case _        => false
  }
}
case class DPT11(sub: Int) extends KNXDatatype(sub) {
  override def toString = if (sub == -1) "DPT-11" else s"DPT-11-$sub"
  override def toPythonType: PythonType = PythonInt
  override def similarTo(other: KNXDatatype): Boolean = other match {
    case DPT11(_) => true
    case _        => false
  }
}
case class DPT12(sub: Int) extends KNXDatatype(sub) {
  override def toString = if (sub == -1) "DPT-12" else s"DPT-12-$sub"
  override def toPythonType: PythonType = PythonInt
  override def similarTo(other: KNXDatatype): Boolean = other match {
    case DPT12(_) => true
    case _        => false
  }
}
case class DPT13(sub: Int) extends KNXDatatype(sub) {
  override def toString = if (sub == -1) "DPT-13" else s"DPT-13-$sub"
  override def toPythonType: PythonType = PythonInt
  override def similarTo(other: KNXDatatype): Boolean = other match {
    case DPT13(_) => true
    case _        => false
  }
}
case class DPT14(sub: Int) extends KNXDatatype(sub) {
  override def toString = if (sub == -1) "DPT-14" else s"DPT-14-$sub"
  override def toPythonType: PythonType = PythonFloat
  override def similarTo(other: KNXDatatype): Boolean = other match {
    case DPT14(_) => true
    case _        => false
  }
}
case class DPT16(sub: Int) extends KNXDatatype(sub) {
  override def toString = if (sub == -1) "DPT-16" else s"DPT-16-$sub"
  override def toPythonType: PythonType = PythonInt
  override def similarTo(other: KNXDatatype): Boolean = other match {
    case DPT16(_) => true
    case _        => false
  }
}
case class DPT17(sub: Int) extends KNXDatatype(sub) {
  override def toString = if (sub == -1) "DPT-17" else s"DPT-17-$sub"
  override def toPythonType: PythonType = PythonInt
  override def similarTo(other: KNXDatatype): Boolean = other match {
    case DPT17(_) => true
    case _        => false
  }
}
case class DPT18(sub: Int) extends KNXDatatype(sub) {
  override def toString = if (sub == -1) "DPT-18" else s"DPT-18-$sub"
  override def toPythonType: PythonType = PythonInt
  override def similarTo(other: KNXDatatype): Boolean = other match {
    case DPT18(_) => true
    case _        => false
  }
}
case class DPT19(sub: Int) extends KNXDatatype(sub) {
  override def toString = if (sub == -1) "DPT-19" else s"DPT-19-$sub"
  override def toPythonType: PythonType = PythonInt
  override def similarTo(other: KNXDatatype): Boolean = other match {
    case DPT19(_) => true
    case _        => false
  }
}
case class DPT20(sub: Int) extends KNXDatatype(sub) {
  override def toString = if (sub == -1) "DPT-20" else s"DPT-20-$sub"
  override def toPythonType: PythonType = PythonInt
  override def similarTo(other: KNXDatatype): Boolean = other match {
    case DPT20(_) => true
    case _        => false
  }
}
case class DPTUnknown(sub: Int) extends KNXDatatype(sub) {
  override def toString: String = "DPT-Unknown"
  override def toPythonType: PythonType = PythonInt
  override def similarTo(other: KNXDatatype): Boolean = true
}
