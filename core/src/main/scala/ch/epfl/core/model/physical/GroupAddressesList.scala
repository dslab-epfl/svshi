package ch.epfl.core.model.physical

import upickle.default.{ReadWriter, macroRW}

final case class GroupAddressesList(addresses: List[(String, String)])
object GroupAddressesList {
  implicit val rw: ReadWriter[GroupAddressesList] = macroRW[GroupAddressesList]
}
