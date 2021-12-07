package ch.epfl.core.verifier.bindings.exceptions

import ch.epfl.core.verifier.exceptions.{VerifierError, VerifierMessage, VerifierWarning}

sealed trait BindingsVerifierMessage extends VerifierMessage
sealed trait BindingsVerifierError extends VerifierError with BindingsVerifierMessage
sealed trait BindingsVerifierWarning extends VerifierWarning with BindingsVerifierMessage

case class ErrorNotBoundToPhysicalDevice(msg: String) extends BindingsVerifierError
case class ErrorKNXDatatype(msg: String) extends BindingsVerifierError
case class WarningKNXDatatype(msg: String) extends BindingsVerifierWarning
case class ErrorIOType(msg: String) extends BindingsVerifierError
case class WarningIOType(msg: String) extends BindingsVerifierWarning
case class ErrorGroupAddressConflictingPythonTypes(msg: String) extends BindingsVerifierError
case class ErrorProtoDevicesBoundSameIdDifferentDPT(msg: String) extends BindingsVerifierError