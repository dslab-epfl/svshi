package ch.epfl.core.models.physical

import scala.util.matching.Regex

sealed trait IOType

object IOType {
  def IOTypeRegex: Regex = "in|out|in/out|unknown".r
  def fromString(s: String): Option[IOType] = {
    val sSmall = s.toLowerCase()
    if(!IOTypeRegex.matches(sSmall)) None else sSmall match {
      case "in" => Some(In)
      case "out" => Some(Out)
      case "in/out" => Some(InOut)
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