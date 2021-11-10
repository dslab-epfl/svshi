package ch.epfl.core.compiler.models

import scala.util.matching.Regex

sealed trait IOType

object IOType {
  def IOTypeRegex: Regex = "in|out|in/out".r
  def fromString(s: String): Option[IOType] = {
    val sSmall = s.toLowerCase()
    if(!IOTypeRegex.matches(sSmall)) None else sSmall match {
      case "in" => Some(In)
      case "out" => Some(Out)
      case "unknown" => Some(Unknown)
    }
  }
}

case object In extends IOType
case object Out extends IOType
case object Unknown extends IOType