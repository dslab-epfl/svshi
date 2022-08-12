package ch.epfl.web.service.main.session.floors

import ch.epfl.web.service.main.utils.Utils.JsonParsingException
import upickle.default.{ReadWriter, macroRW, write}

case class Floor(number: Int, name: String, blueprintFilePath: os.Path) {
  def toJsonClass: FloorJson = FloorJson(number, name)
}

case class FloorJson(number: Int, name: String)
object FloorJson {
  implicit val rw: ReadWriter[FloorJson] =
    macroRW[FloorJson]
}

case class FloorListJson(floors: List[FloorJson]) {
  override def toString: String = write(this, indent = 2)
}
object FloorListJson {
  implicit val rw: ReadWriter[FloorListJson] =
    macroRW[FloorListJson]
  def from(s: String): FloorListJson = try {
    upickle.default.read[FloorListJson](s)
  } catch {
    case e: Exception =>
      throw new JsonParsingException(s"The given Json is not parsable, it has either a syntax error or the wrong structure.\nThe following exception was thrown $e")
  }
}
