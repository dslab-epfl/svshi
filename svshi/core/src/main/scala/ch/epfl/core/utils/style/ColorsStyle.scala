package ch.epfl.core.utils.style

import ch.epfl.core.utils.Style
import fansi.{Str, Color}

/** A style with colors
  */
object ColorsStyle extends Style {

  override def info(input: String): Str = fansi.Color.LightBlue(input)

  override def warning(input: String): Str = fansi.Color.Yellow(input)

  override def error(input: String): Str = fansi.Color.Red(input)

  override def success(input: String): Str = fansi.Color.Green(input)

}
