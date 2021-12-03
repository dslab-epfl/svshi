package ch.epfl.core.verifier.static.python

import ch.epfl.core.models.application.ApplicationLibrary
import ch.epfl.core.models.bindings.GroupAddressAssignment
import ch.epfl.core.utils.Constants.{VERIFICATION_PYTHON_MODULE, CROSSHAIR_TIMEOUT_SECONDS}
import ch.epfl.core.verifier.VerifierTr
import ch.epfl.core.verifier.static.python.exceptions.{PythonVerifierErrors, PythonVerifierMessage}

object Verifier extends VerifierTr {
  override def verify(
      newAppLibrary: ApplicationLibrary,
      existingAppsLibrary: ApplicationLibrary,
      groupAddressAssignment: GroupAddressAssignment
  ): List[PythonVerifierMessage] = {
    val (_, stdOutLines) = ProcRunner.callPython(VERIFICATION_PYTHON_MODULE)
    if (stdOutLines.length != 1) {
      // Error while creating the verification file, output
      stdOutLines.map(l => PythonVerifierErrors(s"Verification_file creation ERRORS: $l"))
    } else {
      val verificationFileName = stdOutLines.head
      val (_, crosshairStdOutLines) = ProcRunner.callCrosshair(verificationFileName, CROSSHAIR_TIMEOUT_SECONDS)
      crosshairStdOutLines.map(l => PythonVerifierErrors(l))
    }
  }
}
