package ch.epfl.core.compiler.models

import scala.util.matching.Regex

sealed trait IOType

object IOType {
  def IOTypeRegex: Regex = "in|out|in/out".r
  def fromString(s: String): Option[IOType] = if(!IOTypeRegex.matches(s)) None else s match {
    case _ if s == "in" => Some(In)
    case _ if s == "out" => Some(Out)
    case _ if s == "in/out" => Some(InOut)
  }
}

case object In extends IOType
case object Out extends IOType
case object InOut extends IOType