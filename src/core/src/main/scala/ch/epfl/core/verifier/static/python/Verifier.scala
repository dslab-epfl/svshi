package ch.epfl.core.verifier.static.python

import ch.epfl.core.model.application.ApplicationLibrary
import ch.epfl.core.model.bindings.GroupAddressAssignment
import ch.epfl.core.utils.Constants._
import ch.epfl.core.utils.{ProcRunner, Utils}
import ch.epfl.core.verifier.VerifierTr
import ch.epfl.core.verifier.static.python.exceptions.{PythonVerifierError, PythonVerifierInfo, PythonVerifierMessage}

/** Verifier that verifies the correctness of the python applications
  */
object Verifier extends VerifierTr {
  private val CONFIRMED_ALL_PATHS_MSG: String = "Confirmed over all paths".toLowerCase
  private val CROSSHAIR_ERROR_LINE_REGEX = """^.+verification_file\.py:(\d+): error: (.+)$""".r
  private val SUCCESS_CODE_VERIFICATION_MODULE_PYTHON = 0

  private def transformCrossHairErrorLine(verificationFilePath: os.Path, line: String): String = {
    line match {
      case CROSSHAIR_ERROR_LINE_REGEX(g1, g2) =>
        val lineNumber = g1.toInt
        val errorMessage = g2.capitalize
        Utils.getLineNFile(verificationFilePath, lineNumber) match {
          case Some(fileLine) => s"$errorMessage\nERROR: At line $fileLine"
          case None           => line
        }
      case _ => line
    }
  }

  /** First generate the python code file for verification by calling the python module 'verification.main'
    * and then call the extended_verification module on that file and return messages
    * @param newAppLibrary
    * @param existingAppsLibrary
    * @param groupAddressAssignment
    * @return
    */
  override def verify(newAppLibrary: ApplicationLibrary, existingAppsLibrary: ApplicationLibrary, groupAddressAssignment: GroupAddressAssignment): List[PythonVerifierMessage] = {
    val (errorCode, stdOutErrLines) = ProcRunner.callPythonBlocking(None, None, VERIFICATION_PYTHON_MODULE, os.Path(SVSHI_SRC_FOLDER))
    if (errorCode != SUCCESS_CODE_VERIFICATION_MODULE_PYTHON) {
      // Error while creating the verification file, output
      if (stdOutErrLines.nonEmpty) stdOutErrLines.map(l => PythonVerifierError(s"Verification_file creation ERRORS: $l"))
      else List(PythonVerifierError("Verification_file creation ERRORS: The verification module returned nothing!"))
    } else {
      val strings = stdOutErrLines.head.split('/')
      val verificationFileName = strings.last
      val verificationWdStr = strings.toList.reverse.tail.reverse.mkString("/")
      val verificationWd = os.Path(verificationWdStr, base = os.pwd / os.up)
      val (exit_code, crosshairStdOutLines) = ProcRunner.callPythonBlocking(
        None,
        None,
        EXTENDED_VERIFICATION_PYTHON_MODULE,
        os.Path(SVSHI_SRC_FOLDER),
        VERIFICATION_FILE_MODULE_NAME,
        "-cto",
        PER_CONDITION_TIMEOUT.toString,
        "-pto",
        PER_PATH_TIMEOUT.toString
      )
      var (errorLines, infoLines) = crosshairStdOutLines.partition(_.toLowerCase.contains("error:"))
      if (exit_code != 0) {
        errorLines = errorLines ++ List("non zero return code")
      }
      infoLines.map(l => PythonVerifierInfo(l)) ++
        errorLines.map(l => PythonVerifierError(transformCrossHairErrorLine(verificationWd / verificationFileName, l)))
    }
  }
}
