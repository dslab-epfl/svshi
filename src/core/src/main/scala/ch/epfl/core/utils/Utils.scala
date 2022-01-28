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
        val protoStruct = AppInputJsonParser.parse(protoStructPath)
        Application(f.segments.toList.last, f, protoStruct)
      }),
      path
    )
  }

  /** Return the n_th line of the given file, with the line number with the following format "n:  line_content".
    * If the file has fewer lines than n or if the file does not exist, it returns None.
    *
    * WARNING: The line 1 is the first line of the file (it is NOT 0 base index).
    * @param file
    * @param n
    * @return
    */
  def getLineNFile(file: os.Path, n: Int): Option[String] = {
    if (!os.exists(file)) {
      None
    } else {
      val lines = os.read.lines(file).toList
      if (lines.length <= n) {
        None
      } else {
        Some(s"$n:  ${lines(n - 1)}")
      }
    }
  }
}
