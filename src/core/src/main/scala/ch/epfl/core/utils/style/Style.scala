package ch.epfl.core.utils

import fansi.Str

/** A color style
  */
trait Style {

  /** Color the given string with light blue (close to purple)
    *
    * @param input
    * @return the colored string
    */
  def info(input: String): Str

  /** Color the given string with yellow
    *
    * @param input
    * @return the colored string
    */
  def warning(input: String): Str

  /** Color the given string with red
    *
    * @param input
    * @return the colored string
    */
  def error(input: String): Str

  /** Color the given string with green
    *
    * @param input
    * @return the colored string
    */
  def success(input: String): Str

  /** Color the given string with cyan
    *
    * @param input
    * @return the colored string
    */
  def debug(input: String): Str
}
