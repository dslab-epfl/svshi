package ch.epfl.core.models.physical

import ch.epfl.core.models.python.{PythonBool, PythonFloat, PythonInt, PythonType}

import scala.util.matching.Regex

/**
 * Structure:
    data type: format + encoding
    size: value range + unit
    Notation: X.YYY
    X: defines format + encoding
    YYY: defines value range + unit
    The most often used Datapoint Types are:
    1.yyy = boolean, like switching, move up/down, step
    2.yyy = 2 x boolean, e.g. switching + priority control
    3.yyy = boolean + 3-bit unsigned value, e.g. dimming up/down
    4.yyy = character (8-bit)
    5.yyy = 8-bit unsigned value, like dim value (0..100%), blinds position (0..100%)
    6.yyy = 8-bit 2's complement, e.g. %
    7.yyy = 2 x 8-bit unsigned value, i.e. pulse counter
    8.yyy = 2 x 8-bit 2's complement, e.g. %
    9.yyy = 16-bit float, e.g. temperature
    10.yyy = time
    11.yyy = date
    12.yyy = 4 x 8-bit unsigned value, i.e. pulse counter
    13.yyy = 4 x 8-bit 2's complement, i.e. pulse counter
    14.yyy = 32-bit float, e.g. temperature
    15.yyy = access control
    16.yyy = string -> 14 characters (14 x 8-bit)
    17.yyy = scene number
    18.yyy = scene control
    19.yyy = time + data
    20.yyy = 8-bit enumeration, e.g. HVAC mode ('auto', 'comfort', 'standby', 'economy', 'protection')
 */
sealed trait KNXDatatype {
  def toPythonType: PythonType
}
object KNXDatatype {
  def datatypeRegex: Regex = "(DPT-[0-9]+)|DPT-Unknown".r
  def fromString(s: String): Option[KNXDatatype] = if(datatypeRegex.findFirstIn(s).isEmpty) None else s match {
    case _ if datatypeRegex.findFirstIn(s).get == "DPT-Unknown" => Some(DPTUnknown)
    case _ if datatypeRegex.findFirstIn(s).get.split("-").apply(1).toInt == 1 => Some(DPT1)
    case _ if datatypeRegex.findFirstIn(s).get.split("-").apply(1).toInt == 2 => Some(DPT2)
    case _ if datatypeRegex.findFirstIn(s).get.split("-").apply(1).toInt == 3 => Some(DPT3)
    case _ if datatypeRegex.findFirstIn(s).get.split("-").apply(1).toInt == 5 => Some(DPT5)
    case _ if datatypeRegex.findFirstIn(s).get.split("-").apply(1).toInt == 6 => Some(DPT6)
    case _ if datatypeRegex.findFirstIn(s).get.split("-").apply(1).toInt == 7 => Some(DPT7)
    case _ if datatypeRegex.findFirstIn(s).get.split("-").apply(1).toInt == 9 => Some(DPT9)
    case _ if datatypeRegex.findFirstIn(s).get.split("-").apply(1).toInt == 10 => Some(DPT10)
    case _ if datatypeRegex.findFirstIn(s).get.split("-").apply(1).toInt == 11 => Some(DPT11)
    case _ if datatypeRegex.findFirstIn(s).get.split("-").apply(1).toInt == 12 => Some(DPT12)
    case _ if datatypeRegex.findFirstIn(s).get.split("-").apply(1).toInt == 13 => Some(DPT13)
    case _ if datatypeRegex.findFirstIn(s).get.split("-").apply(1).toInt == 14 => Some(DPT14)
    case _ if datatypeRegex.findFirstIn(s).get.split("-").apply(1).toInt == 16 => Some(DPT16)
    case _ if datatypeRegex.findFirstIn(s).get.split("-").apply(1).toInt == 17 => Some(DPT17)
    case _ if datatypeRegex.findFirstIn(s).get.split("-").apply(1).toInt == 18 => Some(DPT18)
    case _ if datatypeRegex.findFirstIn(s).get.split("-").apply(1).toInt == 19 => Some(DPT19)
    case _ if datatypeRegex.findFirstIn(s).get.split("-").apply(1).toInt == 20 => Some(DPT20)
    case _ => None
  }

  /**
   * Returns a DPT given the object size string from the ETS project.
   * BE CAREFUL IF ADDING NEW PARSED VALUES: TYPES OTHER THAN 1 OR 2 BITS CAN CORRESPOND TO MULTIPLE
   * DIFFERENT DPTS (UNSIGNED, TWO'2 COMPLEMENT, ...)
   * @param dptSize
   * @return
   */
  def fromDPTSize(dptSize: String): Option[KNXDatatype] = {
    if(dptSize.toLowerCase.contains("bit")){
      if(dptSize.contains("1 ") || dptSize.toLowerCase.contains("one")){
        Some(DPT1)
      } else if(dptSize.contains("2 ") || dptSize.toLowerCase.contains("two")){
        Some(DPT2)
      } else {
        None
      }
    } else {
      None
    }
  }
}
case object DPT1 extends KNXDatatype {
  override def toString = "DPT-1"
  override def toPythonType: PythonType = PythonBool
}
case object DPT2 extends KNXDatatype {
  override def toString = "DPT-2"
  override def toPythonType: PythonType = PythonInt
}
case object DPT3 extends KNXDatatype {
  override def toString = "DPT-3"
  override def toPythonType: PythonType = PythonInt
}
case object DPT5 extends KNXDatatype {
  override def toString = "DPT-5"
  override def toPythonType: PythonType = PythonInt
}
case object DPT6 extends KNXDatatype {
  override def toString = "DPT-6"
  override def toPythonType: PythonType = PythonInt
}
case object DPT7 extends KNXDatatype {
  override def toString = "DPT-7"
  override def toPythonType: PythonType = PythonInt
}
case object DPT9 extends KNXDatatype {
  override def toString = "DPT-9"
  override def toPythonType: PythonType = PythonFloat
}
case object DPT10 extends KNXDatatype {
  override def toString = "DPT-10"
  override def toPythonType: PythonType = PythonInt
}
case object DPT11 extends KNXDatatype {
  override def toString = "DPT-11"
  override def toPythonType: PythonType = PythonInt
}
case object DPT12 extends KNXDatatype {
  override def toString = "DPT-12"
  override def toPythonType: PythonType = PythonInt
}
case object DPT13 extends KNXDatatype {
  override def toString = "DPT-13"
  override def toPythonType: PythonType = PythonInt
}
case object DPT14 extends KNXDatatype {
  override def toString = "DPT-14"
  override def toPythonType: PythonType = PythonFloat
}
case object DPT16 extends KNXDatatype {
  override def toString = "DPT-16"
  override def toPythonType: PythonType = PythonInt
}
case object DPT17 extends KNXDatatype {
  override def toString = "DPT-17"
  override def toPythonType: PythonType = PythonInt
}
case object DPT18 extends KNXDatatype {
  override def toString = "DPT-18"
  override def toPythonType: PythonType = PythonInt
}
case object DPT19 extends KNXDatatype {
  override def toString = "DPT-19"
  override def toPythonType: PythonType = PythonInt
}
case object DPT20 extends KNXDatatype {
  override def toString = "DPT-20"
  override def toPythonType: PythonType = PythonInt
}
case object DPTUnknown extends KNXDatatype {
  override def toString: String = "DPT-Unknown"
  override def toPythonType: PythonType = PythonInt
}

