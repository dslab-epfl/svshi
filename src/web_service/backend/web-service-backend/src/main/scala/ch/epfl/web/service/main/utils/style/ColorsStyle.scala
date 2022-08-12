package ch.epfl.web.service.main.utils.style

import fansi.Str

/** A style with colors
  */
object ColorsStyle extends Style {

  override def info(input: String): Str = fansi.Color.LightBlue(input)

  override def warning(input: String): Str = fansi.Color.Yellow(input)

  override def error(input: String): Str = fansi.Color.Red(input)

  override def success(input: String): Str = fansi.Color.Green(input)

  override def debug(input: String): Str = fansi.Color.Cyan(input)

}
