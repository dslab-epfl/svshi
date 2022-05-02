package ch.epfl.core.verifier.static.python

import ch.epfl.core.model.application.ApplicationLibrary
import ch.epfl.core.model.bindings.GroupAddressAssignment
import ch.epfl.core.utils.Constants.{CROSSHAIR_TIMEOUT_SECONDS, SVSHI_SRC_FOLDER, VERIFICATION_PYTHON_MODULE}
import ch.epfl.core.utils.{ProcRunner, Utils}
import ch.epfl.core.verifier.VerifierTr
import ch.epfl.core.verifier.static.python.exceptions.{PythonVerifierError, PythonVerifierInfo, PythonVerifierMessage}

/** Verifier that verifies the correctness of the python applications
  */
object Verifier extends VerifierTr {
  private val CONFIRMED_ALL_PATHS_MSG: String = "Confirmed over all paths".toLowerCase
  private val CROSSHAIR_ERROR_LINE_REGEX = """^.+verification_file\.py:(\d+): error: (.+)$""".r

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
    * and then call Crosshair on that file and return messages
    * @param newAppLibrary
    * @param existingAppsLibrary
    * @param groupAddressAssignment
    * @return
    */
  override def verify(newAppLibrary: ApplicationLibrary, existingAppsLibrary: ApplicationLibrary, groupAddressAssignment: GroupAddressAssignment): List[PythonVerifierMessage] = {
    val (_, stdOutLines) = ProcRunner.callPythonBlocking(None, None, VERIFICATION_PYTHON_MODULE, os.Path(SVSHI_SRC_FOLDER))
    if (stdOutLines.length != 1) {
      // Error while creating the verification file, output
      if (stdOutLines.nonEmpty) stdOutLines.map(l => PythonVerifierError(s"Verification_file creation ERRORS: $l"))
      else List(PythonVerifierError("Verification_file creation ERRORS: The verification module returned nothing!"))
    } else {
      val strings = stdOutLines.head.split('/')
      val verificationFileName = strings.last
      val verificationWdStr = strings.toList.reverse.tail.reverse.mkString("/")
      val verificationWd = os.Path(verificationWdStr, base = os.pwd / os.up)
      val (_, crosshairStdOutLines) = ProcRunner.callCrosshair(verificationFileName, verificationWd, CROSSHAIR_TIMEOUT_SECONDS)
      val (infoLines, errorLines) = crosshairStdOutLines.partition(_.toLowerCase.contains(CONFIRMED_ALL_PATHS_MSG))
      infoLines.map(l => PythonVerifierInfo(l)) ++
        errorLines.map(l => PythonVerifierError(transformCrossHairErrorLine(verificationWd / verificationFileName, l)))
    }
  }
}
