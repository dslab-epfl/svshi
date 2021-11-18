package ch.epfl.core.verifier.bindings.exceptions

class ErrorNotBoundToPhysicalDeviceException(msg: String) extends Exception(msg)
class ErrorKNXDatatype(msg: String) extends Exception(msg)
class WarningKNXDatatype(msg: String) extends Exception(msg)
class ErrorIOType(msg: String) extends Exception(msg)
class WarningIOType(msg: String) extends Exception(msg)
