package ch.epfl.core.models.prototypical

import ch.epfl.core.models.python.{PythonBool, PythonFloat}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SupportedDeviceBindingTest extends AnyFlatSpec with Matchers {
  "getPythonTypes" should "return the correct map for the different bindings" in {
    BinarySensorBinding("", 123).getPythonTypes shouldEqual Map((123, PythonBool))
    SwitchBinding("", 123).getPythonTypes shouldEqual Map((123, PythonBool))
    HumiditySensorBinding("", 123).getPythonTypes shouldEqual Map((123, PythonFloat))
    TemperatureSensorBinding("", 123).getPythonTypes shouldEqual Map((123, PythonFloat))
  }

}
