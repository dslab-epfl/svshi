package ch.epfl.core.utils

import ch.epfl.core.model.application.{Application, ApplicationLibrary}
import ch.epfl.core.parser.json.prototype.AppInputJsonParser
import ch.epfl.core.utils.FileUtils.getListOfFolders

import java.nio.file.Path

/** Utility functions for the compiler and the verifier
  */
object Utils {
  def loadApplicationsLibrary(path: String): ApplicationLibrary = {
    ApplicationLibrary(
      getListOfFolders(path).map(f => {
        val protoStruct = AppInputJsonParser.parse(f.toPath.resolve(Path.of(Constants.APP_PROTO_STRUCT_FILE_NAME)).toString)
        Application(f.getName, f.getPath, protoStruct)
      }),
      path
    )
  }
}
