package ch.epfl.core.api.server.json

import ch.epfl.core.parser.json.JsonParsingException
import upickle.default.{ReadWriter, macroRW, write}

/** Classes used by upickle in the CoreApiServer
  */

case class ResponseBody(status: Boolean, output: List[String]) {
  override def toString: String = write(this, indent = 2)
}
object ResponseBody {
  implicit val responseBody: ReadWriter[ResponseBody] = macroRW[ResponseBody]
  def from(s: String): ResponseBody = try {
    upickle.default.read[ResponseBody](s)
  } catch {
    case e: Exception =>
      throw new JsonParsingException(s"The given Json is not parsable, it has either a syntax error or the wrong structure.\nThe following exception was thrown $e")
  }
}
