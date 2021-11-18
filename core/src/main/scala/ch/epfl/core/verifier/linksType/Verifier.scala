package ch.epfl.core.verifier.linksType

import ch.epfl.core.models.application.ApplicationLibrary
import ch.epfl.core.parsers.json.bindings.BindingsJsonParser
import ch.epfl.core.parsers.json.physical.PhysicalStructureJsonParser
import ch.epfl.core.utils.Constants

import java.nio.file.Path

object Verifier {
  def verify(appLibrary: ApplicationLibrary): ApplicationLibrary = {
    val physicalStructure = PhysicalStructureJsonParser.parse(Path.of(appLibrary.path).resolve(Constants.PHYSICAL_STRUCTURE_JSON_FILE_NAME).toString)
    val bindings = BindingsJsonParser.parse(Path.of(appLibrary.path).resolve(Constants.APP_PROTO_BINDINGS_JSON_FILE_NAME).toString)

    appLibrary
  }
}
