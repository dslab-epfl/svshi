package ch.epfl.web.service.main.utils

import ch.epfl.web.service.main.utils.style.Style

/** Message printer
  */
object Printer {

  /** Print the given info message
    *
    * @param input
    */
  def info(input: String)(implicit style: Style): Unit = println(style.info(s"$input"))

  /** Print the given warning message
    *
    * @param input
    */
  def warning(input: String)(implicit style: Style): Unit = println(style.warning(s"WARNING: $input"))

  /** Print the given error message
    *
    * @param input
    */
  def error(input: String)(implicit style: Style): Unit = println(style.error(s"ERROR: $input"))

  /** Print the given success message
    *
    * @param input
    */
  def success(input: String)(implicit style: Style): Unit = println(style.success(s"$input"))

  def debug(input: String)(implicit style: Style): Unit = println(style.debug(s"$input"))
}
