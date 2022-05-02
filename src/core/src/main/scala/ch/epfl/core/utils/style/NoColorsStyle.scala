package ch.epfl.core.utils.style

import ch.epfl.core.utils.Style
import fansi.Str

/** A style without colors, only plain strings
  */
object NoColorsStyle extends Style {

  override def info(input: String): Str = Str(input)

  override def warning(input: String): Str = Str(input)

  override def error(input: String): Str = Str(input)

  override def success(input: String): Str = Str(input)

  override def debug(input: String): Str = Str(input)

}
