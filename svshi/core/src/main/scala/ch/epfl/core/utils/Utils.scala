package ch.epfl.core.utils

import ch.epfl.core.model.application.{Application, ApplicationLibrary}
import ch.epfl.core.parser.json.prototype.AppInputJsonParser
import ch.epfl.core.utils.FileUtils.getListOfFolders

/** Utility functions for the compiler and the verifier
  */
object Utils {
  def loadApplicationsLibrary(path: os.Path): ApplicationLibrary = {
    ApplicationLibrary(
      getListOfFolders(path).map(f => {
        val protoStructPath = f / Constants.APP_PROTO_STRUCT_FILE_NAME
        val protoStruct = AppInputJsonParser.parse(protoStructPath.toString)
        Application(f.segments.toList.last, f, protoStruct)
      }),
      path
    )
  }
}
