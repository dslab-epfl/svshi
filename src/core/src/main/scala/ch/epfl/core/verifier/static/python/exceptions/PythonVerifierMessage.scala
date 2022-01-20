package ch.epfl.core.verifier.static.python.exceptions

import ch.epfl.core.verifier.exceptions.{VerifierError, VerifierInfo, VerifierMessage}

sealed trait PythonVerifierMessage extends VerifierMessage

case class PythonVerifierError(msg: String) extends VerifierError with PythonVerifierMessage
case class PythonVerifierInfo(msg: String) extends VerifierInfo with PythonVerifierMessage
