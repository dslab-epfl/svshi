package ch.epfl.core.utils

import ch.epfl.core.parsers.json.prototype.AppInputJsonParser
import ch.epfl.core.models.application.{Application, ApplicationLibrary}
import ch.epfl.core.utils.FileUtils.getListOfFolders

import java.nio.file.Path

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
