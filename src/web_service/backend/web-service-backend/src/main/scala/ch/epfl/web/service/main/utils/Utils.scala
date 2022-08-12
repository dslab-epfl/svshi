package ch.epfl.web.service.main.utils

object Utils {

  /** Return true if the given string is of the format xxx.xxx.xxx.xxx:y or localhost:y
    * where x is a digit and y and sequence of digits
    * @param s
    * @return
    */
  def validAddressPortString(s: String): Boolean = {
    val addressRegex = """(^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3}):(\d)+$)|(localhost:(\d)+$)""".r
    addressRegex.matches(s)
  }

  class JsonParsingException(msg: String) extends Exception(msg)
}
