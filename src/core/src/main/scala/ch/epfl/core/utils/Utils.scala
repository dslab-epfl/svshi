package ch.epfl.core.utils

import ch.epfl.core.model.application.{Application, ApplicationLibrary, NotPrivileged}
import ch.epfl.core.model.prototypical.AppPrototypicalStructure
import ch.epfl.core.parser.json.prototype.AppInputJsonParser
import ch.epfl.core.utils.FileUtils.{getListOfFiles, getListOfFolders}

/** Utility functions for the compiler and the verifier
  */
object Utils {

  /** Return true if the given string is of the format xxx.xxx.xxx.xxx:y or localhost:y
    * where x is a digit and y and sequence of digits
    * @param s
    * @return
    */
  def validAddressPortString(s: String): Boolean = {
//    val addressRegex = """(^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3}):(\d)+$)|(localhost:(\d)+$)""".r
    val addressRegex = """(^([A-Za-z0-9\.\-\_])+:\d+$)""".r // Relaxed because of docker usage and container name as IP
    addressRegex.matches(s)
  }
  def loadApplicationsLibrary(path: os.Path): ApplicationLibrary = {
    ApplicationLibrary(
      getListOfFolders(path).map(f => {
        val protoStructPath = f / Constants.APP_PROTO_STRUCT_FILE_NAME
        val protoStruct = if (os.exists(protoStructPath)) {
          AppInputJsonParser.parse(protoStructPath)
        } else {
          // Default empty config, the process fails later when generateBindings or compile if no file
          AppPrototypicalStructure(NotPrivileged, 0, Nil)
        }
        val filesFolderPath = f / Constants.FILES_FOLDER_EACH_APPLICATION_NAME
        if (!os.exists(filesFolderPath)) os.makeDir(filesFolderPath)
        val files = getListOfFiles(filesFolderPath)
        Application(name = f.segments.toList.last, appFolderPath = f, appProtoStructure = protoStruct, files = files)
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
