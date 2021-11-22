package ch.epfl.core.models.physical

import upickle.default.{ReadWriter, macroRW}

final case class GroupAddressesList(addresses: List[String])
object GroupAddressesList {
  implicit val rw: ReadWriter[GroupAddressesList] = macroRW[GroupAddressesList]
}
