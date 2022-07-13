package ch.epfl.core.model.physical

import scala.util.matching.Regex

/** Represents the type of I/O provided by communication objects in a physical KNX installation
  * In: The device accepts value from the and modifies its state (i.e., Actuator)
  * Out: The device writes its value to the bus or replies to a read request (i.e., Sensor)
  */
sealed trait IOType

object IOType {
  def IOTypeRegex: Regex = "in|out|in/out|unknown".r
  def fromString(s: String): Option[IOType] = {
    val sSmall = s.toLowerCase()
    if (!IOTypeRegex.matches(sSmall)) None
    else
      sSmall match {
        case "in"      => Some(In)
        case "out"     => Some(Out)
        case "in/out"  => Some(InOut)
        case "unknown" => Some(Unknown)
      }
  }
}

case object In extends IOType {
  override def toString: String = "in"
}
case object Out extends IOType {
  override def toString: String = "out"
}
case object InOut extends IOType {
  override def toString: String = "in/out"
}
case object Unknown extends IOType {
  override def toString: String = "unknown"
}
