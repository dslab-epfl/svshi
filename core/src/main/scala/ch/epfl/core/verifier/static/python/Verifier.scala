package ch.epfl.core.verifier.static.python

import ch.epfl.core.model.application.ApplicationLibrary
import ch.epfl.core.model.bindings.GroupAddressAssignment
import ch.epfl.core.utils.Constants.{VERIFICATION_PYTHON_MODULE, CROSSHAIR_TIMEOUT_SECONDS}
import ch.epfl.core.verifier.VerifierTr
import ch.epfl.core.verifier.static.python.exceptions.{PythonVerifierError, PythonVerifierInfo, PythonVerifierMessage}

/**
 * Verifier that verifies the correctness of the python applications
 */
object Verifier extends VerifierTr{
  val CONFIRMED_ALL_PATHS_MSG: String = "Confirmed over all paths".toLowerCase

  /**
   * First generate the python code file for verification by calling the python module 'verification.main'
   * and then call Crosshair on that file and return messages
   * @param newAppLibrary
   * @param existingAppsLibrary
   * @param groupAddressAssignment
   * @return
   */
  override def verify(newAppLibrary: ApplicationLibrary, existingAppsLibrary: ApplicationLibrary, groupAddressAssignment: GroupAddressAssignment): List[PythonVerifierMessage] = {
    val (_, stdOutLines) = ProcRunner.callPython(VERIFICATION_PYTHON_MODULE)
    if(stdOutLines.length != 1) {
      // Error while creating the verification file, output
      if(stdOutLines.nonEmpty) stdOutLines.map(l => PythonVerifierError(s"Verification_file creation ERRORS: $l"))
      else List(PythonVerifierError("Verification_file creation ERRORS: The core_python module returned nothing!"))

    } else {
      val strings = stdOutLines.head.split('/')
      val verificationFileName = strings.last
      val verificationWdStr =  strings.toList.reverse.tail.reverse.mkString("/")
      val verificationWd = os.Path(verificationWdStr, base = os.pwd / os.up)
      val (_, crosshairStdOutLines) = ProcRunner.callCrosshair(verificationFileName, verificationWd, CROSSHAIR_TIMEOUT_SECONDS)
      val tupl = crosshairStdOutLines.partition(_.toLowerCase.contains(CONFIRMED_ALL_PATHS_MSG))
      tupl._1.map(l => PythonVerifierInfo(l)) ++
      tupl._2.map(l => PythonVerifierError(l))
    }
  }
}
