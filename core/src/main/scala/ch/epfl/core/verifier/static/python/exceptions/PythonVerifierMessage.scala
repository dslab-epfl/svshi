package ch.epfl.core.verifier.static.python.exceptions

import ch.epfl.core.verifier.exceptions.{VerifierError, VerifierMessage}

sealed trait PythonVerifierMessage extends VerifierMessage
sealed trait PythonVerifierError extends VerifierError with PythonVerifierMessage


case class PythonVerifierErrors(msg: String) extends PythonVerifierError