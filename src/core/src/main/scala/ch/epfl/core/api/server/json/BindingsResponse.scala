package ch.epfl.core.api.server.json

import ch.epfl.core.model.prototypical.AppLibraryBindings
import ch.epfl.core.parser.json.JsonParsingException
import ch.epfl.core.parser.json.physical.PhysicalStructureJson
import upickle.default.{ReadWriter, macroRW, write}

/** Classes used by upickle in the CoreApiServer
  */

case class BindingsResponse(physicalStructure: PhysicalStructureJson, bindings: AppLibraryBindings) {
  override def toString: String = write(this, indent = 2)
}
object BindingsResponse {
  implicit val bindingsResponse: ReadWriter[BindingsResponse] = macroRW[BindingsResponse]
  def from(s: String): BindingsResponse = try {
    upickle.default.read[BindingsResponse](s)
  } catch {
    case e: Exception =>
      throw new JsonParsingException(s"The given Json is not parsable, it has either a syntax error or the wrong structure.\nThe following exception was thrown $e")
  }
}
