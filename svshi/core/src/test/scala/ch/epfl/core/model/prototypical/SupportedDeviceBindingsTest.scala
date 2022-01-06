package ch.epfl.core.model.prototypical

import ch.epfl.core.model.python.{PythonBool, PythonFloat}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SupportedDeviceBindingsTest extends AnyFlatSpec with Matchers {
  "getPythonTypes" should "return the correct map for the different bindings" in {
    BinarySensorBinding("", 123).getPythonTypes shouldEqual Map((123, PythonBool))
    SwitchBinding("", 123).getPythonTypes shouldEqual Map((123, PythonBool))
    HumiditySensorBinding("", 123).getPythonTypes shouldEqual Map((123, PythonFloat))
    TemperatureSensorBinding("", 123).getPythonTypes shouldEqual Map((123, PythonFloat))
  }

  "equivalent" should "return true if the supported devices bindings are equivalent" in {
    BinarySensorBinding("", 123) equivalent BinarySensorBinding("", -1) shouldEqual true
    SwitchBinding("", 123) equivalent SwitchBinding("", -1) shouldEqual true
    HumiditySensorBinding("", 123) equivalent HumiditySensorBinding("", -1) shouldEqual true
    TemperatureSensorBinding("", 123) equivalent TemperatureSensorBinding("", -1) shouldEqual true
  }

  "equivalent" should "return false if the supported devices bindings are not equivalent" in {
    BinarySensorBinding("", 123) equivalent SwitchBinding("", -1) shouldEqual false
    SwitchBinding("", 123) equivalent BinarySensorBinding("", -1) shouldEqual false
    HumiditySensorBinding("", 123) equivalent TemperatureSensorBinding("", -1) shouldEqual false
    TemperatureSensorBinding("", 123) equivalent HumiditySensorBinding("", -1) shouldEqual false
  }

  "equivalent" should "return true if the device instance bindings are equivalent" in {
    DeviceInstanceBinding("test", BinarySensorBinding("", 123)) equivalent DeviceInstanceBinding("test", BinarySensorBinding("", -1)) shouldEqual true
  }

  "equivalent" should "return false if the device instance bindings are not equivalent" in {
    DeviceInstanceBinding("test", BinarySensorBinding("", 123)) equivalent DeviceInstanceBinding("another test", BinarySensorBinding("", 123)) shouldEqual false
    DeviceInstanceBinding("test", BinarySensorBinding("", 123)) equivalent DeviceInstanceBinding("test", HumiditySensorBinding("", -1)) shouldEqual false
  }

  "equivalent" should "return true if the app prototype bindings are equivalent" in {
    AppPrototypeBindings("app", DeviceInstanceBinding("test", BinarySensorBinding("", 123)) :: Nil) equivalent AppPrototypeBindings(
      "app",
      DeviceInstanceBinding("test", BinarySensorBinding("", 42)) :: Nil
    ) shouldEqual true
  }

  "equivalent" should "return false if the app prototype bindings are not equivalent" in {
    AppPrototypeBindings("app", DeviceInstanceBinding("test", BinarySensorBinding("", 123)) :: Nil) equivalent AppPrototypeBindings(
      "app",
      DeviceInstanceBinding("another test", BinarySensorBinding("", 42)) :: Nil
    ) shouldEqual false
    AppPrototypeBindings("app", DeviceInstanceBinding("test", BinarySensorBinding("", 123)) :: Nil) equivalent AppPrototypeBindings(
      "another app",
      DeviceInstanceBinding("test", BinarySensorBinding("", 42)) :: Nil
    ) shouldEqual false
    AppPrototypeBindings("app", DeviceInstanceBinding("test", BinarySensorBinding("", 123)) :: Nil) equivalent AppPrototypeBindings(
      "app",
      DeviceInstanceBinding("test", HumiditySensorBinding("", 42)) :: Nil
    ) shouldEqual false
  }

  "equivalent" should "return true if the app library bindings are equivalent" in {
    AppLibraryBindings(AppPrototypeBindings("app", DeviceInstanceBinding("test", BinarySensorBinding("", 123)) :: Nil) :: Nil) equivalent AppLibraryBindings(
      AppPrototypeBindings("app", DeviceInstanceBinding("test", BinarySensorBinding("", 42)) :: Nil) :: Nil
    ) shouldEqual true
  }

  "equivalent" should "return false if the app library bindings are not equivalent" in {
    AppLibraryBindings(AppPrototypeBindings("app", DeviceInstanceBinding("test", BinarySensorBinding("", 123)) :: Nil) :: Nil) equivalent AppLibraryBindings(
      AppPrototypeBindings("another app", DeviceInstanceBinding("test", BinarySensorBinding("", 42)) :: Nil) :: Nil
    ) shouldEqual false
  }
}
