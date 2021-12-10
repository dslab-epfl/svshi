package ch.epfl.core.model.physical

import upickle.default.{ReadWriter, macroRW}

/**
 * Represents a list of group addresses along with their datatype in Python
 * @param addresses
 */
final case class GroupAddressesList(addresses: List[(String, String)])
object GroupAddressesList {
  implicit val rw: ReadWriter[GroupAddressesList] = macroRW[GroupAddressesList]
}
