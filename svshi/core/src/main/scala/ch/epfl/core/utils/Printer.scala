package ch.epfl.core.utils

/** Message printer
  */
object Printer {

  /** Print the given info message
    *
    * @param input
    */
  def info(input: String)(implicit style: Style): Unit = println(style.info(input))

  /** Print the given warning message
    *
    * @param input
    */
  def warning(input: String)(implicit style: Style): Unit = println(style.warning(input))

  /** Print the given error message
    *
    * @param input
    */
  def error(input: String)(implicit style: Style): Unit = println(style.error(input))

  /** Print the given success message
    *
    * @param input
    */
  def success(input: String)(implicit style: Style): Unit = println(style.success(input))
}
