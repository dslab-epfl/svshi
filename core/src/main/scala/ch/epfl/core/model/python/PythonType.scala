package ch.epfl.core.model.python

/**
 * Represents a datatype in Python
 */
sealed trait PythonType

case object PythonBool extends PythonType {
  override def toString: String = "bool"
}

case object PythonFloat extends PythonType {
  override def toString: String = "float"
}

case object PythonInt extends PythonType {
  override def toString: String = "int"
}