package ch.epfl.core.verifier.bindings.exceptions

sealed trait BindingsVerifierReturnValues {
  def msg: String
}

case class ErrorNotBoundToPhysicalDevice(msg: String) extends BindingsVerifierReturnValues
case class ErrorKNXDatatype(msg: String) extends BindingsVerifierReturnValues
case class WarningKNXDatatype(msg: String) extends BindingsVerifierReturnValues
case class ErrorIOType(msg: String) extends BindingsVerifierReturnValues
case class WarningIOType(msg: String) extends BindingsVerifierReturnValues
