package ch.epfl.core.verifier.bindings.exceptions

sealed trait BindingsVerifierErrors {
  def msg: String
}

case class ErrorNotBoundToPhysicalDevice(msg: String) extends BindingsVerifierErrors
case class ErrorKNXDatatype(msg: String) extends BindingsVerifierErrors
case class WarningKNXDatatype(msg: String) extends BindingsVerifierErrors
case class ErrorIOType(msg: String) extends BindingsVerifierErrors
case class WarningIOType(msg: String) extends BindingsVerifierErrors
case class ErrorGroupAddressConflictingPythonTypes(msg: String) extends BindingsVerifierErrors