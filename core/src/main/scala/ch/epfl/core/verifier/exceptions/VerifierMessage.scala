package ch.epfl.core.verifier.exceptions

trait VerifierMessage {
  def msg: String
}

trait VerifierWarning extends VerifierMessage
trait VerifierInfo extends VerifierMessage
trait VerifierError extends VerifierMessage
