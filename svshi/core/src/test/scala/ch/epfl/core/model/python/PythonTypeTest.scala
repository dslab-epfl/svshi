package ch.epfl.core.model.python

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class PythonTypeTest extends AnyFlatSpec with Matchers {
  "toString" should "return the correct string for the python types" in {
    PythonBool.toString shouldEqual "bool"
    PythonInt.toString shouldEqual "int"
    PythonFloat.toString shouldEqual "float"
  }
}
