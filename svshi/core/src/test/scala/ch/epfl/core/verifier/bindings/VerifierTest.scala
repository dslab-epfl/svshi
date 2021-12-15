package ch.epfl.core.verifier.bindings

import ch.epfl.core.model.application.NotPrivileged
import ch.epfl.core.model.bindings.GroupAddressAssignment
import ch.epfl.core.model.physical._
import ch.epfl.core.model.prototypical._
import ch.epfl.core.model.python.{PythonBool, PythonFloat}
import ch.epfl.core.verifier.bindings.Verifier._
import ch.epfl.core.verifier.bindings.exceptions._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class VerifierTest extends AnyFlatSpec with Matchers {
  "verifyBindingsIoTypes" should "return Nil for valid inputs" in {

    val device1Physical = PhysicalDevice(
      "device1",
      ("1", "1", "1"),
      List(
        PhysicalDeviceNode(
          "device1Node1",
          List(PhysicalDeviceCommObject("device1Node1ComObj1", DPT1, In, 111))
        )
      )
    )
    val device2Physical = PhysicalDevice(
      "device2",
      ("1", "1", "2"),
      List(
        PhysicalDeviceNode(
          "device2Node1",
          List(
            PhysicalDeviceCommObject("device2Node1ComObj1", DPT2, Out, 211),
            PhysicalDeviceCommObject("device2Node1ComObj2", DPT5, InOut, 212)
          )
        )
      )
    )
    val device3Physical = PhysicalDevice(
      "device3",
      ("1", "1", "3"),
      List(
        PhysicalDeviceNode(
          "device3Node1",
          List(
            PhysicalDeviceCommObject("device3Node1ComObj1", DPT2, Out, 311),
            PhysicalDeviceCommObject("device3Node1ComObj2", DPT5, In, 312),
            PhysicalDeviceCommObject("device3Node1ComObj3", DPT5, Out, 313)
          )
        ),
        PhysicalDeviceNode(
          "device3Node2",
          List(
            PhysicalDeviceCommObject("device3Node2ComObj1", DPT5, In, 321),
            PhysicalDeviceCommObject("device3Node2ComObj2", DPT12, InOut, 322)
          )
        )
      )
    )
    val physicalStructure = PhysicalStructure(List(device1Physical, device2Physical, device3Physical))
    val appLibraryBindings = AppLibraryBindings(
      List(
        AppPrototypeBindings(
          "app1",
          List(
            DeviceInstanceBinding("device1", BinarySensorBinding(BinarySensor.toString, 311)),
            DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 212)),
            DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 322)),
            DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 313))
          )
        )
      )
    )
    val appPrototypicalStructure = AppPrototypicalStructure(
      permissionLevel = NotPrivileged,
      deviceInstances = List(
        AppPrototypicalDeviceInstance("device1", BinarySensor),
        AppPrototypicalDeviceInstance("device2", Switch),
        AppPrototypicalDeviceInstance("device3", TemperatureSensor),
        AppPrototypicalDeviceInstance("device4", HumiditySensor)
      )
    )
    val appPrototypicalStructures: Map[String, AppPrototypicalStructure] = List(("app1", appPrototypicalStructure)).toMap
    verifyBindingsIoTypes(physicalStructure, appLibraryBindings, appPrototypicalStructures) shouldEqual Nil
  }

  "verifyBindingsIoTypes" should "detects an IN physical to OUT proto HumiditySensor" in {

    val device1Physical = PhysicalDevice(
      "device1",
      ("1", "1", "1"),
      List(
        PhysicalDeviceNode(
          "device1Node1",
          List(PhysicalDeviceCommObject("device1Node1ComObj1", DPT1, In, 111))
        )
      )
    )
    val device2Physical = PhysicalDevice(
      "device2",
      ("1", "1", "2"),
      List(
        PhysicalDeviceNode(
          "device2Node1",
          List(
            PhysicalDeviceCommObject("device2Node1ComObj1", DPT2, Out, 211),
            PhysicalDeviceCommObject("device2Node1ComObj2", DPT5, InOut, 212)
          )
        )
      )
    )
    val device3Physical = PhysicalDevice(
      "device3",
      ("1", "1", "3"),
      List(
        PhysicalDeviceNode(
          "device3Node1",
          List(
            PhysicalDeviceCommObject("device3Node1ComObj1", DPT2, Out, 311),
            PhysicalDeviceCommObject("device3Node1ComObj2", DPT5, In, 312),
            PhysicalDeviceCommObject("device3Node1ComObj3", DPT5, In, 313)
          )
        ),
        PhysicalDeviceNode(
          "device3Node2",
          List(
            PhysicalDeviceCommObject("device3Node2ComObj1", DPT5, In, 321),
            PhysicalDeviceCommObject("device3Node2ComObj2", DPT12, InOut, 322)
          )
        )
      )
    )
    val physicalStructure = PhysicalStructure(List(device1Physical, device2Physical, device3Physical))
    val appLibraryBindings = AppLibraryBindings(
      List(
        AppPrototypeBindings(
          "app1",
          List(
            DeviceInstanceBinding("device1", BinarySensorBinding(BinarySensor.toString, 311)),
            DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 212)),
            DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 322)),
            DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 313))
          )
        )
      )
    )
    val appPrototypicalStructure = AppPrototypicalStructure(
      permissionLevel = NotPrivileged,
      deviceInstances = List(
        AppPrototypicalDeviceInstance("device1", BinarySensor),
        AppPrototypicalDeviceInstance("device2", Switch),
        AppPrototypicalDeviceInstance("device3", TemperatureSensor),
        AppPrototypicalDeviceInstance("device4", HumiditySensor)
      )
    )
    val appPrototypicalStructures: Map[String, AppPrototypicalStructure] = List(("app1", appPrototypicalStructure)).toMap
    val res = verifyBindingsIoTypes(physicalStructure, appLibraryBindings, appPrototypicalStructures)
    res.length shouldEqual 1
    res.head shouldBe an[ErrorIOType]
    res.head.msg.contains("313") shouldEqual true
  }

  "verifyBindingsIoTypes" should "detects an IN physical to OUT proto Temperature" in {

    val device1Physical = PhysicalDevice(
      "device1",
      ("1", "1", "1"),
      List(
        PhysicalDeviceNode(
          "device1Node1",
          List(PhysicalDeviceCommObject("device1Node1ComObj1", DPT1, In, 111))
        )
      )
    )
    val device2Physical = PhysicalDevice(
      "device2",
      ("1", "1", "2"),
      List(
        PhysicalDeviceNode(
          "device2Node1",
          List(
            PhysicalDeviceCommObject("device2Node1ComObj1", DPT2, Out, 211),
            PhysicalDeviceCommObject("device2Node1ComObj2", DPT5, InOut, 212)
          )
        )
      )
    )
    val device3Physical = PhysicalDevice(
      "device3",
      ("1", "1", "3"),
      List(
        PhysicalDeviceNode(
          "device3Node1",
          List(
            PhysicalDeviceCommObject("device3Node1ComObj1", DPT2, Out, 311),
            PhysicalDeviceCommObject("device3Node1ComObj2", DPT5, In, 312),
            PhysicalDeviceCommObject("device3Node1ComObj3", DPT5, Out, 313)
          )
        ),
        PhysicalDeviceNode(
          "device3Node2",
          List(
            PhysicalDeviceCommObject("device3Node2ComObj1", DPT5, In, 321),
            PhysicalDeviceCommObject("device3Node2ComObj2", DPT12, In, 322)
          )
        )
      )
    )
    val physicalStructure = PhysicalStructure(List(device1Physical, device2Physical, device3Physical))
    val appLibraryBindings = AppLibraryBindings(
      List(
        AppPrototypeBindings(
          "app1",
          List(
            DeviceInstanceBinding("device1", BinarySensorBinding(BinarySensor.toString, 311)),
            DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 212)),
            DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 322)),
            DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 313))
          )
        )
      )
    )
    val appPrototypicalStructure = AppPrototypicalStructure(
      permissionLevel = NotPrivileged,
      deviceInstances = List(
        AppPrototypicalDeviceInstance("device1", BinarySensor),
        AppPrototypicalDeviceInstance("device2", Switch),
        AppPrototypicalDeviceInstance("device3", TemperatureSensor),
        AppPrototypicalDeviceInstance("device4", HumiditySensor)
      )
    )
    val appPrototypicalStructures: Map[String, AppPrototypicalStructure] = List(("app1", appPrototypicalStructure)).toMap
    val res = verifyBindingsIoTypes(physicalStructure, appLibraryBindings, appPrototypicalStructures)
    res.length shouldEqual 1
    res.head shouldBe an[ErrorIOType]
    res.head.msg.contains("322") shouldEqual true
  }

  "verifyBindingsIoTypes" should "detects an IN physical to OUT proto Binary" in {
    val device1Physical = PhysicalDevice(
      "device1",
      ("1", "1", "1"),
      List(
        PhysicalDeviceNode(
          "device1Node1",
          List(PhysicalDeviceCommObject("device1Node1ComObj1", DPT1, In, 111))
        )
      )
    )
    val device2Physical = PhysicalDevice(
      "device2",
      ("1", "1", "2"),
      List(
        PhysicalDeviceNode(
          "device2Node1",
          List(
            PhysicalDeviceCommObject("device2Node1ComObj1", DPT2, Out, 211),
            PhysicalDeviceCommObject("device2Node1ComObj2", DPT5, InOut, 212)
          )
        )
      )
    )
    val device3Physical = PhysicalDevice(
      "device3",
      ("1", "1", "3"),
      List(
        PhysicalDeviceNode(
          "device3Node1",
          List(
            PhysicalDeviceCommObject("device3Node1ComObj1", DPT2, In, 311),
            PhysicalDeviceCommObject("device3Node1ComObj2", DPT5, In, 312),
            PhysicalDeviceCommObject("device3Node1ComObj3", DPT5, Out, 313)
          )
        ),
        PhysicalDeviceNode(
          "device3Node2",
          List(
            PhysicalDeviceCommObject("device3Node2ComObj1", DPT5, In, 321),
            PhysicalDeviceCommObject("device3Node2ComObj2", DPT12, InOut, 322)
          )
        )
      )
    )
    val physicalStructure = PhysicalStructure(List(device1Physical, device2Physical, device3Physical))
    val appLibraryBindings = AppLibraryBindings(
      List(
        AppPrototypeBindings(
          "app1",
          List(
            DeviceInstanceBinding("device1", BinarySensorBinding(BinarySensor.toString, 311)),
            DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 212)),
            DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 322)),
            DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 313))
          )
        )
      )
    )
    val appPrototypicalStructure = AppPrototypicalStructure(
      permissionLevel = NotPrivileged,
      deviceInstances = List(
        AppPrototypicalDeviceInstance("device1", BinarySensor),
        AppPrototypicalDeviceInstance("device2", Switch),
        AppPrototypicalDeviceInstance("device3", TemperatureSensor),
        AppPrototypicalDeviceInstance("device4", HumiditySensor)
      )
    )
    val appPrototypicalStructures: Map[String, AppPrototypicalStructure] = List(("app1", appPrototypicalStructure)).toMap
    val res = verifyBindingsIoTypes(physicalStructure, appLibraryBindings, appPrototypicalStructures)
    res.length shouldEqual 1
    res.head shouldBe an[ErrorIOType]
    res.head.msg.contains("311") shouldEqual true
  }

  "verifyBindingsIoTypes" should "return Nil for an OUT physical to IN proto" in {

    val device1Physical = PhysicalDevice(
      "device1",
      ("1", "1", "1"),
      List(
        PhysicalDeviceNode(
          "device1Node1",
          List(PhysicalDeviceCommObject("device1Node1ComObj1", DPT1, In, 111))
        )
      )
    )
    val device2Physical = PhysicalDevice(
      "device2",
      ("1", "1", "2"),
      List(
        PhysicalDeviceNode(
          "device2Node1",
          List(
            PhysicalDeviceCommObject("device2Node1ComObj1", DPT1, Out, 211),
            PhysicalDeviceCommObject("device2Node1ComObj2", DPT5, InOut, 212)
          )
        )
      )
    )
    val device3Physical = PhysicalDevice(
      "device3",
      ("1", "1", "3"),
      List(
        PhysicalDeviceNode(
          "device3Node1",
          List(
            PhysicalDeviceCommObject("device3Node1ComObj1", DPT2, Out, 311),
            PhysicalDeviceCommObject("device3Node1ComObj2", DPT5, In, 312),
            PhysicalDeviceCommObject("device3Node1ComObj3", DPT5, Out, 313)
          )
        ),
        PhysicalDeviceNode(
          "device3Node2",
          List(
            PhysicalDeviceCommObject("device3Node2ComObj1", DPT5, In, 321),
            PhysicalDeviceCommObject("device3Node2ComObj2", DPT12, In, 322)
          )
        )
      )
    )
    val physicalStructure = PhysicalStructure(List(device1Physical, device2Physical, device3Physical))
    val appLibraryBindings = AppLibraryBindings(
      List(
        AppPrototypeBindings(
          "app1",
          List(
            DeviceInstanceBinding("device1", BinarySensorBinding(BinarySensor.toString, 311)),
            DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 212)),
            DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 322)),
            DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 313))
          )
        )
      )
    )
    val appPrototypicalStructure = AppPrototypicalStructure(
      permissionLevel = NotPrivileged,
      deviceInstances = List(
        AppPrototypicalDeviceInstance("device1", BinarySensor),
        AppPrototypicalDeviceInstance("device2", Switch),
        AppPrototypicalDeviceInstance("device3", TemperatureSensor),
        AppPrototypicalDeviceInstance("device4", HumiditySensor)
      )
    )
    val appPrototypicalStructures: Map[String, AppPrototypicalStructure] = List(("app1", appPrototypicalStructure)).toMap
    val res = verifyBindingsIoTypes(physicalStructure, appLibraryBindings, appPrototypicalStructures)
    res.length shouldEqual 1
    res.head shouldBe an[ErrorIOType]
    res.head.msg.contains("322") shouldEqual true
  }

  "verifyBindingsIoTypes" should "accept an IN physical to OUT proto Binary" in {

    val device1Physical = PhysicalDevice(
      "device1",
      ("1", "1", "1"),
      List(
        PhysicalDeviceNode(
          "device1Node1",
          List(PhysicalDeviceCommObject("device1Node1ComObj1", DPT1, In, 111))
        )
      )
    )
    val device2Physical = PhysicalDevice(
      "device2",
      ("1", "1", "2"),
      List(
        PhysicalDeviceNode(
          "device2Node1",
          List(
            PhysicalDeviceCommObject("device2Node1ComObj1", DPT2, Out, 211),
            PhysicalDeviceCommObject("device2Node1ComObj2", DPT5, InOut, 212)
          )
        )
      )
    )
    val device3Physical = PhysicalDevice(
      "device3",
      ("1", "1", "3"),
      List(
        PhysicalDeviceNode(
          "device3Node1",
          List(
            PhysicalDeviceCommObject("device3Node1ComObj1", DPT2, In, 311),
            PhysicalDeviceCommObject("device3Node1ComObj2", DPT5, In, 312),
            PhysicalDeviceCommObject("device3Node1ComObj3", DPT5, Out, 313)
          )
        ),
        PhysicalDeviceNode(
          "device3Node2",
          List(
            PhysicalDeviceCommObject("device3Node2ComObj1", DPT5, In, 321),
            PhysicalDeviceCommObject("device3Node2ComObj2", DPT12, InOut, 322)
          )
        )
      )
    )
    val physicalStructure = PhysicalStructure(List(device1Physical, device2Physical, device3Physical))
    val appLibraryBindings = AppLibraryBindings(
      List(
        AppPrototypeBindings(
          "app1",
          List(
            DeviceInstanceBinding("device1", BinarySensorBinding(BinarySensor.toString, 211)),
            DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 212)),
            DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 322)),
            DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 313))
          )
        )
      )
    )
    val appPrototypicalStructure = AppPrototypicalStructure(
      permissionLevel = NotPrivileged,
      deviceInstances = List(
        AppPrototypicalDeviceInstance("device1", BinarySensor),
        AppPrototypicalDeviceInstance("device2", Switch),
        AppPrototypicalDeviceInstance("device3", TemperatureSensor),
        AppPrototypicalDeviceInstance("device4", HumiditySensor)
      )
    )
    val appPrototypicalStructures: Map[String, AppPrototypicalStructure] = List(("app1", appPrototypicalStructure)).toMap
    verifyBindingsIoTypes(physicalStructure, appLibraryBindings, appPrototypicalStructures) shouldEqual Nil
  }

  "verifyBindingsIoTypes" should "return Warning for an Unknown IOType for proto Binary" in {

    val device1Physical = PhysicalDevice(
      "device1",
      ("1", "1", "1"),
      List(
        PhysicalDeviceNode(
          "device1Node1",
          List(PhysicalDeviceCommObject("device1Node1ComObj1", DPT1, In, 111))
        )
      )
    )
    val device2Physical = PhysicalDevice(
      "device2",
      ("1", "1", "2"),
      List(
        PhysicalDeviceNode(
          "device2Node1",
          List(
            PhysicalDeviceCommObject("device2Node1ComObj1", DPT2, Out, 211),
            PhysicalDeviceCommObject("device2Node1ComObj2", DPT5, InOut, 212)
          )
        )
      )
    )
    val device3Physical = PhysicalDevice(
      "device3",
      ("1", "1", "3"),
      List(
        PhysicalDeviceNode(
          "device3Node1",
          List(
            PhysicalDeviceCommObject("device3Node1ComObj1", DPT2, Unknown, 311),
            PhysicalDeviceCommObject("device3Node1ComObj2", DPT5, In, 312),
            PhysicalDeviceCommObject("device3Node1ComObj3", DPT5, Out, 313)
          )
        ),
        PhysicalDeviceNode(
          "device3Node2",
          List(
            PhysicalDeviceCommObject("device3Node2ComObj1", DPT5, In, 321),
            PhysicalDeviceCommObject("device3Node2ComObj2", DPT12, InOut, 322)
          )
        )
      )
    )
    val physicalStructure = PhysicalStructure(List(device1Physical, device2Physical, device3Physical))
    val appLibraryBindings = AppLibraryBindings(
      List(
        AppPrototypeBindings(
          "app1",
          List(
            DeviceInstanceBinding("device1", BinarySensorBinding(BinarySensor.toString, 311)),
            DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 212)),
            DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 322)),
            DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 313))
          )
        )
      )
    )
    val appPrototypicalStructure = AppPrototypicalStructure(
      permissionLevel = NotPrivileged,
      deviceInstances = List(
        AppPrototypicalDeviceInstance("device1", BinarySensor),
        AppPrototypicalDeviceInstance("device2", Switch),
        AppPrototypicalDeviceInstance("device3", TemperatureSensor),
        AppPrototypicalDeviceInstance("device4", HumiditySensor)
      )
    )
    val appPrototypicalStructures: Map[String, AppPrototypicalStructure] = List(("app1", appPrototypicalStructure)).toMap
    val res = verifyBindingsIoTypes(physicalStructure, appLibraryBindings, appPrototypicalStructures)
    res.length shouldEqual 1
    res.head shouldBe an[WarningIOType]
    res.head.msg.contains("device1") shouldEqual true
    res.head.msg.contains("Unknown") shouldEqual true
  }

  "verifyBindingsIoTypes" should "return Warning for an Unknown IOType for proto Temperature" in {

    val device1Physical = PhysicalDevice(
      "device1",
      ("1", "1", "1"),
      List(
        PhysicalDeviceNode(
          "device1Node1",
          List(PhysicalDeviceCommObject("device1Node1ComObj1", DPT1, In, 111))
        )
      )
    )
    val device2Physical = PhysicalDevice(
      "device2",
      ("1", "1", "2"),
      List(
        PhysicalDeviceNode(
          "device2Node1",
          List(
            PhysicalDeviceCommObject("device2Node1ComObj1", DPT2, Out, 211),
            PhysicalDeviceCommObject("device2Node1ComObj2", DPT5, InOut, 212)
          )
        )
      )
    )
    val device3Physical = PhysicalDevice(
      "device3",
      ("1", "1", "3"),
      List(
        PhysicalDeviceNode(
          "device3Node1",
          List(
            PhysicalDeviceCommObject("device3Node1ComObj1", DPT2, Out, 311),
            PhysicalDeviceCommObject("device3Node1ComObj2", DPT5, In, 312),
            PhysicalDeviceCommObject("device3Node1ComObj3", DPT5, Out, 313)
          )
        ),
        PhysicalDeviceNode(
          "device3Node2",
          List(
            PhysicalDeviceCommObject("device3Node2ComObj1", DPT5, In, 321),
            PhysicalDeviceCommObject("device3Node2ComObj2", DPT12, Unknown, 322)
          )
        )
      )
    )
    val physicalStructure = PhysicalStructure(List(device1Physical, device2Physical, device3Physical))
    val appLibraryBindings = AppLibraryBindings(
      List(
        AppPrototypeBindings(
          "app1",
          List(
            DeviceInstanceBinding("device1", BinarySensorBinding(BinarySensor.toString, 311)),
            DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 212)),
            DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 322)),
            DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 313))
          )
        )
      )
    )
    val appPrototypicalStructure = AppPrototypicalStructure(
      permissionLevel = NotPrivileged,
      deviceInstances = List(
        AppPrototypicalDeviceInstance("device1", BinarySensor),
        AppPrototypicalDeviceInstance("device2", Switch),
        AppPrototypicalDeviceInstance("device3", TemperatureSensor),
        AppPrototypicalDeviceInstance("device4", HumiditySensor)
      )
    )
    val appPrototypicalStructures: Map[String, AppPrototypicalStructure] = List(("app1", appPrototypicalStructure)).toMap
    val res = verifyBindingsIoTypes(physicalStructure, appLibraryBindings, appPrototypicalStructures)
    res.length shouldEqual 1
    res.head shouldBe an[WarningIOType]
    res.head.msg.contains("device3") shouldEqual true
    res.head.msg.contains("Unknown") shouldEqual true
  }

  "verifyBindingsIoTypes" should "return Warning for an Unknown IOType for proto Humidity" in {

    val device1Physical = PhysicalDevice(
      "device1",
      ("1", "1", "1"),
      List(
        PhysicalDeviceNode(
          "device1Node1",
          List(PhysicalDeviceCommObject("device1Node1ComObj1", DPT1, In, 111))
        )
      )
    )
    val device2Physical = PhysicalDevice(
      "device2",
      ("1", "1", "2"),
      List(
        PhysicalDeviceNode(
          "device2Node1",
          List(
            PhysicalDeviceCommObject("device2Node1ComObj1", DPT2, Out, 211),
            PhysicalDeviceCommObject("device2Node1ComObj2", DPT5, InOut, 212)
          )
        )
      )
    )
    val device3Physical = PhysicalDevice(
      "device3",
      ("1", "1", "3"),
      List(
        PhysicalDeviceNode(
          "device3Node1",
          List(
            PhysicalDeviceCommObject("device3Node1ComObj1", DPT2, Out, 311),
            PhysicalDeviceCommObject("device3Node1ComObj2", DPT5, In, 312),
            PhysicalDeviceCommObject("device3Node1ComObj3", DPT5, Unknown, 313)
          )
        ),
        PhysicalDeviceNode(
          "device3Node2",
          List(
            PhysicalDeviceCommObject("device3Node2ComObj1", DPT5, In, 321),
            PhysicalDeviceCommObject("device3Node2ComObj2", DPT12, InOut, 322)
          )
        )
      )
    )
    val physicalStructure = PhysicalStructure(List(device1Physical, device2Physical, device3Physical))
    val appLibraryBindings = AppLibraryBindings(
      List(
        AppPrototypeBindings(
          "app1",
          List(
            DeviceInstanceBinding("device1", BinarySensorBinding(BinarySensor.toString, 311)),
            DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 212)),
            DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 322)),
            DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 313))
          )
        )
      )
    )
    val appPrototypicalStructure = AppPrototypicalStructure(
      permissionLevel = NotPrivileged,
      deviceInstances = List(
        AppPrototypicalDeviceInstance("device1", BinarySensor),
        AppPrototypicalDeviceInstance("device2", Switch),
        AppPrototypicalDeviceInstance("device3", TemperatureSensor),
        AppPrototypicalDeviceInstance("device4", HumiditySensor)
      )
    )
    val appPrototypicalStructures: Map[String, AppPrototypicalStructure] = List(("app1", appPrototypicalStructure)).toMap
    val res = verifyBindingsIoTypes(physicalStructure, appLibraryBindings, appPrototypicalStructures)
    res.length shouldEqual 1
    res.head shouldBe an[WarningIOType]
    res.head.msg.contains("device4") shouldEqual true
    res.head.msg.contains("Unknown") shouldEqual true
  }

  "verifyBindingsIoTypes" should "return Warning for an an Unknown IOType for proto Switch" in {

    val device1Physical = PhysicalDevice(
      "device1",
      ("1", "1", "1"),
      List(
        PhysicalDeviceNode(
          "device1Node1",
          List(PhysicalDeviceCommObject("device1Node1ComObj1", DPT1, In, 111))
        )
      )
    )
    val device2Physical = PhysicalDevice(
      "device2",
      ("1", "1", "2"),
      List(
        PhysicalDeviceNode(
          "device2Node1",
          List(
            PhysicalDeviceCommObject("device2Node1ComObj1", DPT2, Unknown, 211),
            PhysicalDeviceCommObject("device2Node1ComObj2", DPT5, Unknown, 212)
          )
        )
      )
    )
    val device3Physical = PhysicalDevice(
      "device3",
      ("1", "1", "3"),
      List(
        PhysicalDeviceNode(
          "device3Node1",
          List(
            PhysicalDeviceCommObject("device3Node1ComObj1", DPT2, Out, 311),
            PhysicalDeviceCommObject("device3Node1ComObj2", DPT5, In, 312),
            PhysicalDeviceCommObject("device3Node1ComObj3", DPT5, Out, 313)
          )
        ),
        PhysicalDeviceNode(
          "device3Node2",
          List(
            PhysicalDeviceCommObject("device3Node2ComObj1", DPT5, In, 321),
            PhysicalDeviceCommObject("device3Node2ComObj2", DPT12, InOut, 322)
          )
        )
      )
    )
    val physicalStructure = PhysicalStructure(List(device1Physical, device2Physical, device3Physical))
    val appLibraryBindings = AppLibraryBindings(
      List(
        AppPrototypeBindings(
          "app1",
          List(
            DeviceInstanceBinding("device1", BinarySensorBinding(BinarySensor.toString, 311)),
            DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 212)),
            DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 322)),
            DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 313))
          )
        )
      )
    )
    val appPrototypicalStructure = AppPrototypicalStructure(
      permissionLevel = NotPrivileged,
      deviceInstances = List(
        AppPrototypicalDeviceInstance("device1", BinarySensor),
        AppPrototypicalDeviceInstance("device2", Switch),
        AppPrototypicalDeviceInstance("device3", TemperatureSensor),
        AppPrototypicalDeviceInstance("device4", HumiditySensor)
      )
    )
    val appPrototypicalStructures: Map[String, AppPrototypicalStructure] = List(("app1", appPrototypicalStructure)).toMap
    val res = verifyBindingsIoTypes(physicalStructure, appLibraryBindings, appPrototypicalStructures)
    res.length shouldEqual 1
    res.head shouldBe an[WarningIOType]
    res.head.msg.contains("device2") shouldEqual true
    res.head.msg.contains("Unknown") shouldEqual true
  }

  "verifyBindingsIoTypes" should "return Warning for an Unknown IOType for proto Humidity and an Error for wrong IO Type for proto Binary" in {

    val device1Physical = PhysicalDevice(
      "device1",
      ("1", "1", "1"),
      List(
        PhysicalDeviceNode(
          "device1Node1",
          List(PhysicalDeviceCommObject("device1Node1ComObj1", DPT1, In, 111))
        )
      )
    )
    val device2Physical = PhysicalDevice(
      "device2",
      ("1", "1", "2"),
      List(
        PhysicalDeviceNode(
          "device2Node1",
          List(
            PhysicalDeviceCommObject("device2Node1ComObj1", DPT2, Out, 211),
            PhysicalDeviceCommObject("device2Node1ComObj2", DPT5, InOut, 212)
          )
        )
      )
    )
    val device3Physical = PhysicalDevice(
      "device3",
      ("1", "1", "3"),
      List(
        PhysicalDeviceNode(
          "device3Node1",
          List(
            PhysicalDeviceCommObject("device3Node1ComObj1", DPT2, In, 311),
            PhysicalDeviceCommObject("device3Node1ComObj2", DPT5, In, 312),
            PhysicalDeviceCommObject("device3Node1ComObj3", DPT5, Unknown, 313)
          )
        ),
        PhysicalDeviceNode(
          "device3Node2",
          List(
            PhysicalDeviceCommObject("device3Node2ComObj1", DPT5, In, 321),
            PhysicalDeviceCommObject("device3Node2ComObj2", DPT12, InOut, 322)
          )
        )
      )
    )
    val physicalStructure = PhysicalStructure(List(device1Physical, device2Physical, device3Physical))
    val appLibraryBindings = AppLibraryBindings(
      List(
        AppPrototypeBindings(
          "app1",
          List(
            DeviceInstanceBinding("device1", BinarySensorBinding(BinarySensor.toString, 311)),
            DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 212)),
            DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 322)),
            DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 313))
          )
        )
      )
    )
    val appPrototypicalStructure = AppPrototypicalStructure(
      permissionLevel = NotPrivileged,
      deviceInstances = List(
        AppPrototypicalDeviceInstance("device1", BinarySensor),
        AppPrototypicalDeviceInstance("device2", Switch),
        AppPrototypicalDeviceInstance("device3", TemperatureSensor),
        AppPrototypicalDeviceInstance("device4", HumiditySensor)
      )
    )
    val appPrototypicalStructures: Map[String, AppPrototypicalStructure] = List(("app1", appPrototypicalStructure)).toMap
    val res = verifyBindingsIoTypes(physicalStructure, appLibraryBindings, appPrototypicalStructures)
    res.length shouldEqual 2
    res.exists(v => v.isInstanceOf[WarningIOType]) shouldEqual true
    res.exists(v => v.isInstanceOf[ErrorIOType]) shouldEqual true
    res.find(v => v.isInstanceOf[WarningIOType]).get.msg.contains("device4") shouldEqual true
    res.find(v => v.isInstanceOf[WarningIOType]).get.msg.contains("Unknown") shouldEqual true
    res.find(v => v.isInstanceOf[ErrorIOType]).get.msg.contains("device1") shouldEqual true
    res.find(v => v.isInstanceOf[ErrorIOType]).get.msg.contains("311") shouldEqual true
  }

  "verifyBindingsIoTypes" should "return Error for an unmapped physical device for proto Binary" in {

    val device1Physical = PhysicalDevice(
      "device1",
      ("1", "1", "1"),
      List(
        PhysicalDeviceNode(
          "device1Node1",
          List(PhysicalDeviceCommObject("device1Node1ComObj1", DPT1, In, 111))
        )
      )
    )
    val device2Physical = PhysicalDevice(
      "device2",
      ("1", "1", "2"),
      List(
        PhysicalDeviceNode(
          "device2Node1",
          List(
            PhysicalDeviceCommObject("device2Node1ComObj1", DPT2, Out, 211),
            PhysicalDeviceCommObject("device2Node1ComObj2", DPT5, InOut, 212)
          )
        )
      )
    )
    val device3Physical = PhysicalDevice(
      "device3",
      ("1", "1", "3"),
      List(
        PhysicalDeviceNode(
          "device3Node1",
          List(
            PhysicalDeviceCommObject("device3Node1ComObj1", DPT2, Out, 311),
            PhysicalDeviceCommObject("device3Node1ComObj2", DPT5, In, 312),
            PhysicalDeviceCommObject("device3Node1ComObj3", DPT5, Out, 313)
          )
        ),
        PhysicalDeviceNode(
          "device3Node2",
          List(
            PhysicalDeviceCommObject("device3Node2ComObj1", DPT5, In, 321),
            PhysicalDeviceCommObject("device3Node2ComObj2", DPT12, InOut, 322)
          )
        )
      )
    )
    val physicalStructure = PhysicalStructure(List(device1Physical, device2Physical, device3Physical))
    val appLibraryBindings = AppLibraryBindings(
      List(
        AppPrototypeBindings(
          "app1",
          List(
            DeviceInstanceBinding("device1", BinarySensorBinding(BinarySensor.toString, -1)),
            DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 212)),
            DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 322)),
            DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 313))
          )
        )
      )
    )
    val appPrototypicalStructure = AppPrototypicalStructure(
      permissionLevel = NotPrivileged,
      deviceInstances = List(
        AppPrototypicalDeviceInstance("device1", BinarySensor),
        AppPrototypicalDeviceInstance("device2", Switch),
        AppPrototypicalDeviceInstance("device3", TemperatureSensor),
        AppPrototypicalDeviceInstance("device4", HumiditySensor)
      )
    )

    val appPrototypicalStructures: Map[String, AppPrototypicalStructure] = List(("app1", appPrototypicalStructure)).toMap
    val res = verifyBindingsIoTypes(physicalStructure, appLibraryBindings, appPrototypicalStructures)
    res.length shouldEqual 1
    res.head shouldBe an[ErrorNotBoundToPhysicalDevice]
    res.head.msg.contains("device1") shouldEqual true
    res.head.msg.contains("not bound") shouldEqual true
  }

  "verifyBindingsIoTypes" should "return Error for an unmapped physical device for proto Binary and an Error for wrong IO type for proto Temperature" in {

    val device1Physical = PhysicalDevice(
      "device1",
      ("1", "1", "1"),
      List(
        PhysicalDeviceNode(
          "device1Node1",
          List(PhysicalDeviceCommObject("device1Node1ComObj1", DPT1, In, 111))
        )
      )
    )
    val device2Physical = PhysicalDevice(
      "device2",
      ("1", "1", "2"),
      List(
        PhysicalDeviceNode(
          "device2Node1",
          List(
            PhysicalDeviceCommObject("device2Node1ComObj1", DPT2, Out, 211),
            PhysicalDeviceCommObject("device2Node1ComObj2", DPT5, InOut, 212)
          )
        )
      )
    )
    val device3Physical = PhysicalDevice(
      "device3",
      ("1", "1", "3"),
      List(
        PhysicalDeviceNode(
          "device3Node1",
          List(
            PhysicalDeviceCommObject("device3Node1ComObj1", DPT2, Out, 311),
            PhysicalDeviceCommObject("device3Node1ComObj2", DPT5, In, 312),
            PhysicalDeviceCommObject("device3Node1ComObj3", DPT5, Out, 313)
          )
        ),
        PhysicalDeviceNode(
          "device3Node2",
          List(
            PhysicalDeviceCommObject("device3Node2ComObj1", DPT5, In, 321),
            PhysicalDeviceCommObject("device3Node2ComObj2", DPT12, In, 322)
          )
        )
      )
    )
    val physicalStructure = PhysicalStructure(List(device1Physical, device2Physical, device3Physical))
    val appLibraryBindings = AppLibraryBindings(
      List(
        AppPrototypeBindings(
          "app1",
          List(
            DeviceInstanceBinding("device1", BinarySensorBinding(BinarySensor.toString, -1)),
            DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 212)),
            DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 322)),
            DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 313))
          )
        )
      )
    )
    val appPrototypicalStructure = AppPrototypicalStructure(
      permissionLevel = NotPrivileged,
      deviceInstances = List(
        AppPrototypicalDeviceInstance("device1", BinarySensor),
        AppPrototypicalDeviceInstance("device2", Switch),
        AppPrototypicalDeviceInstance("device3", TemperatureSensor),
        AppPrototypicalDeviceInstance("device4", HumiditySensor)
      )
    )

    val appPrototypicalStructures: Map[String, AppPrototypicalStructure] = List(("app1", appPrototypicalStructure)).toMap
    val res = verifyBindingsIoTypes(physicalStructure, appLibraryBindings, appPrototypicalStructures)

    res.length shouldEqual 2
    res.exists(v => v.isInstanceOf[ErrorNotBoundToPhysicalDevice]) shouldEqual true
    res.exists(v => v.isInstanceOf[ErrorIOType]) shouldEqual true
    res.find(v => v.isInstanceOf[ErrorIOType]).get.msg.contains("device3") shouldEqual true
    res.find(v => v.isInstanceOf[ErrorIOType]).get.msg.contains("322") shouldEqual true
    res.find(v => v.isInstanceOf[ErrorNotBoundToPhysicalDevice]).get.msg.contains("device1") shouldEqual true
    res.find(v => v.isInstanceOf[ErrorNotBoundToPhysicalDevice]).get.msg.contains("not bound") shouldEqual true
  }

  "verifyBindingsKNXDatatypes" should "return Nil for valid inputs" in {

    val device1Physical = PhysicalDevice(
      "device1",
      ("1", "1", "1"),
      List(
        PhysicalDeviceNode(
          "device1Node1",
          List(PhysicalDeviceCommObject("device1Node1ComObj1", DPT1, In, 111))
        )
      )
    )
    val device2Physical = PhysicalDevice(
      "device2",
      ("1", "1", "2"),
      List(
        PhysicalDeviceNode(
          "device2Node1",
          List(
            PhysicalDeviceCommObject("device2Node1ComObj1", DPT1, Out, 211),
            PhysicalDeviceCommObject("device2Node1ComObj2", DPT1, InOut, 212)
          )
        )
      )
    )
    val device3Physical = PhysicalDevice(
      "device3",
      ("1", "1", "3"),
      List(
        PhysicalDeviceNode(
          "device3Node1",
          List(
            PhysicalDeviceCommObject("device3Node1ComObj1", DPT1, Out, 311),
            PhysicalDeviceCommObject("device3Node1ComObj2", DPT5, In, 312),
            PhysicalDeviceCommObject("device3Node1ComObj3", DPT9, Out, 313)
          )
        ),
        PhysicalDeviceNode(
          "device3Node2",
          List(
            PhysicalDeviceCommObject("device3Node2ComObj1", DPT5, In, 321),
            PhysicalDeviceCommObject("device3Node2ComObj2", DPT9, InOut, 322)
          )
        )
      )
    )
    val physicalStructure = PhysicalStructure(List(device1Physical, device2Physical, device3Physical))
    val appLibraryBindings = AppLibraryBindings(
      List(
        AppPrototypeBindings(
          "app1",
          List(
            DeviceInstanceBinding("device1", BinarySensorBinding(BinarySensor.toString, 311)),
            DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 212)),
            DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 322)),
            DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 313))
          )
        )
      )
    )
    val appPrototypicalStructure = AppPrototypicalStructure(
      permissionLevel = NotPrivileged,
      deviceInstances = List(
        AppPrototypicalDeviceInstance("device1", BinarySensor),
        AppPrototypicalDeviceInstance("device2", Switch),
        AppPrototypicalDeviceInstance("device3", TemperatureSensor),
        AppPrototypicalDeviceInstance("device4", HumiditySensor)
      )
    )
    val appPrototypicalStructures: Map[String, AppPrototypicalStructure] = List(("app1", appPrototypicalStructure)).toMap
    verifyBindingsKNXDatatypes(physicalStructure, appLibraryBindings, appPrototypicalStructures) shouldEqual Nil
  }

  "verifyBindingsKNXDatatypes" should "return an ErrorKNXDatatype for wrong DPT for proto Binary" in {

    val device1Physical = PhysicalDevice(
      "device1",
      ("1", "1", "1"),
      List(
        PhysicalDeviceNode(
          "device1Node1",
          List(PhysicalDeviceCommObject("device1Node1ComObj1", DPT1, In, 111))
        )
      )
    )
    val device2Physical = PhysicalDevice(
      "device2",
      ("1", "1", "2"),
      List(
        PhysicalDeviceNode(
          "device2Node1",
          List(
            PhysicalDeviceCommObject("device2Node1ComObj1", DPT1, Out, 211),
            PhysicalDeviceCommObject("device2Node1ComObj2", DPT1, InOut, 212)
          )
        )
      )
    )
    val device3Physical = PhysicalDevice(
      "device3",
      ("1", "1", "3"),
      List(
        PhysicalDeviceNode(
          "device3Node1",
          List(
            PhysicalDeviceCommObject("device3Node1ComObj1", DPT5, Out, 311),
            PhysicalDeviceCommObject("device3Node1ComObj2", DPT5, In, 312),
            PhysicalDeviceCommObject("device3Node1ComObj3", DPT9, Out, 313)
          )
        ),
        PhysicalDeviceNode(
          "device3Node2",
          List(
            PhysicalDeviceCommObject("device3Node2ComObj1", DPT5, In, 321),
            PhysicalDeviceCommObject("device3Node2ComObj2", DPT9, InOut, 322)
          )
        )
      )
    )
    val physicalStructure = PhysicalStructure(List(device1Physical, device2Physical, device3Physical))
    val appLibraryBindings = AppLibraryBindings(
      List(
        AppPrototypeBindings(
          "app1",
          List(
            DeviceInstanceBinding("device1", BinarySensorBinding(BinarySensor.toString, 311)),
            DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 212)),
            DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 322)),
            DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 313))
          )
        )
      )
    )
    val appPrototypicalStructure = AppPrototypicalStructure(
      permissionLevel = NotPrivileged,
      deviceInstances = List(
        AppPrototypicalDeviceInstance("device1", BinarySensor),
        AppPrototypicalDeviceInstance("device2", Switch),
        AppPrototypicalDeviceInstance("device3", TemperatureSensor),
        AppPrototypicalDeviceInstance("device4", HumiditySensor)
      )
    )
    val appPrototypicalStructures: Map[String, AppPrototypicalStructure] = List(("app1", appPrototypicalStructure)).toMap
    val res = verifyBindingsKNXDatatypes(physicalStructure, appLibraryBindings, appPrototypicalStructures)
    res.length shouldEqual 1
    res.head shouldBe an[ErrorKNXDatatype]
    res.head.msg.contains("device1") shouldEqual true
    res.head.msg.contains("DPT-1") shouldEqual true
    res.head.msg.contains("DPT-5") shouldEqual true
    res.head.msg.contains("311") shouldEqual true
  }

  "verifyBindingsKNXDatatypes" should "return an ErrorKNXDatatype for wrong DPT for proto Switch" in {

    val device1Physical = PhysicalDevice(
      "device1",
      ("1", "1", "1"),
      List(
        PhysicalDeviceNode(
          "device1Node1",
          List(PhysicalDeviceCommObject("device1Node1ComObj1", DPT1, In, 111))
        )
      )
    )
    val device2Physical = PhysicalDevice(
      "device2",
      ("1", "1", "2"),
      List(
        PhysicalDeviceNode(
          "device2Node1",
          List(
            PhysicalDeviceCommObject("device2Node1ComObj1", DPT19, Out, 211),
            PhysicalDeviceCommObject("device2Node1ComObj2", DPT19, InOut, 212)
          )
        )
      )
    )
    val device3Physical = PhysicalDevice(
      "device3",
      ("1", "1", "3"),
      List(
        PhysicalDeviceNode(
          "device3Node1",
          List(
            PhysicalDeviceCommObject("device3Node1ComObj1", DPT1, Out, 311),
            PhysicalDeviceCommObject("device3Node1ComObj2", DPT5, In, 312),
            PhysicalDeviceCommObject("device3Node1ComObj3", DPT9, Out, 313)
          )
        ),
        PhysicalDeviceNode(
          "device3Node2",
          List(
            PhysicalDeviceCommObject("device3Node2ComObj1", DPT5, In, 321),
            PhysicalDeviceCommObject("device3Node2ComObj2", DPT9, InOut, 322)
          )
        )
      )
    )
    val physicalStructure = PhysicalStructure(List(device1Physical, device2Physical, device3Physical))
    val appLibraryBindings = AppLibraryBindings(
      List(
        AppPrototypeBindings(
          "app1",
          List(
            DeviceInstanceBinding("device1", BinarySensorBinding(BinarySensor.toString, 311)),
            DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 212)),
            DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 322)),
            DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 313))
          )
        )
      )
    )
    val appPrototypicalStructure = AppPrototypicalStructure(
      permissionLevel = NotPrivileged,
      deviceInstances = List(
        AppPrototypicalDeviceInstance("device1", BinarySensor),
        AppPrototypicalDeviceInstance("device2", Switch),
        AppPrototypicalDeviceInstance("device3", TemperatureSensor),
        AppPrototypicalDeviceInstance("device4", HumiditySensor)
      )
    )
    val appPrototypicalStructures: Map[String, AppPrototypicalStructure] = List(("app1", appPrototypicalStructure)).toMap
    val res = verifyBindingsKNXDatatypes(physicalStructure, appLibraryBindings, appPrototypicalStructures)
    res.length shouldEqual 1
    res.head shouldBe an[ErrorKNXDatatype]
    res.head.msg.contains("device2") shouldEqual true
    res.head.msg.contains("DPT-1") shouldEqual true
    res.head.msg.contains("DPT-19") shouldEqual true
    res.head.msg.contains("212") shouldEqual true
  }

  "verifyBindingsKNXDatatypes" should "return an WarningKNXDatatypes for Unknown DPT for proto Switch" in {

    val device1Physical = PhysicalDevice(
      "device1",
      ("1", "1", "1"),
      List(
        PhysicalDeviceNode(
          "device1Node1",
          List(PhysicalDeviceCommObject("device1Node1ComObj1", DPT1, In, 111))
        )
      )
    )
    val device2Physical = PhysicalDevice(
      "device2",
      ("1", "1", "2"),
      List(
        PhysicalDeviceNode(
          "device2Node1",
          List(
            PhysicalDeviceCommObject("device2Node1ComObj1", DPTUnknown, Out, 211),
            PhysicalDeviceCommObject("device2Node1ComObj2", DPTUnknown, InOut, 212)
          )
        )
      )
    )
    val device3Physical = PhysicalDevice(
      "device3",
      ("1", "1", "3"),
      List(
        PhysicalDeviceNode(
          "device3Node1",
          List(
            PhysicalDeviceCommObject("device3Node1ComObj1", DPT1, Out, 311),
            PhysicalDeviceCommObject("device3Node1ComObj2", DPT5, In, 312),
            PhysicalDeviceCommObject("device3Node1ComObj3", DPT9, Out, 313)
          )
        ),
        PhysicalDeviceNode(
          "device3Node2",
          List(
            PhysicalDeviceCommObject("device3Node2ComObj1", DPT5, In, 321),
            PhysicalDeviceCommObject("device3Node2ComObj2", DPT9, InOut, 322)
          )
        )
      )
    )
    val physicalStructure = PhysicalStructure(List(device1Physical, device2Physical, device3Physical))
    val appLibraryBindings = AppLibraryBindings(
      List(
        AppPrototypeBindings(
          "app1",
          List(
            DeviceInstanceBinding("device1", BinarySensorBinding(BinarySensor.toString, 311)),
            DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 212)),
            DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 322)),
            DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 313))
          )
        )
      )
    )
    val appPrototypicalStructure = AppPrototypicalStructure(
      permissionLevel = NotPrivileged,
      deviceInstances = List(
        AppPrototypicalDeviceInstance("device1", BinarySensor),
        AppPrototypicalDeviceInstance("device2", Switch),
        AppPrototypicalDeviceInstance("device3", TemperatureSensor),
        AppPrototypicalDeviceInstance("device4", HumiditySensor)
      )
    )
    val appPrototypicalStructures: Map[String, AppPrototypicalStructure] = List(("app1", appPrototypicalStructure)).toMap
    val res = verifyBindingsKNXDatatypes(physicalStructure, appLibraryBindings, appPrototypicalStructures)
    res.length shouldEqual 1
    res.head shouldBe an[WarningKNXDatatype]
    res.head.msg.contains("device2") shouldEqual true
    res.head.msg.contains("Unknown") shouldEqual true
    res.head.msg.contains("212") shouldEqual true
  }

  "verifyBindingsKNXDatatypes" should "return Error for an unmapped physical device for proto Binary" in {

    val device1Physical = PhysicalDevice(
      "device1",
      ("1", "1", "1"),
      List(
        PhysicalDeviceNode(
          "device1Node1",
          List(PhysicalDeviceCommObject("device1Node1ComObj1", DPT1, In, 111))
        )
      )
    )
    val device2Physical = PhysicalDevice(
      "device2",
      ("1", "1", "2"),
      List(
        PhysicalDeviceNode(
          "device2Node1",
          List(
            PhysicalDeviceCommObject("device2Node1ComObj1", DPT1, Out, 211),
            PhysicalDeviceCommObject("device2Node1ComObj2", DPT1, InOut, 212)
          )
        )
      )
    )
    val device3Physical = PhysicalDevice(
      "device3",
      ("1", "1", "3"),
      List(
        PhysicalDeviceNode(
          "device3Node1",
          List(
            PhysicalDeviceCommObject("device3Node1ComObj1", DPT1, Out, 311),
            PhysicalDeviceCommObject("device3Node1ComObj2", DPT5, In, 312),
            PhysicalDeviceCommObject("device3Node1ComObj3", DPT9, Out, 313)
          )
        ),
        PhysicalDeviceNode(
          "device3Node2",
          List(
            PhysicalDeviceCommObject("device3Node2ComObj1", DPT5, In, 321),
            PhysicalDeviceCommObject("device3Node2ComObj2", DPT9, InOut, 322)
          )
        )
      )
    )
    val physicalStructure = PhysicalStructure(List(device1Physical, device2Physical, device3Physical))
    val appLibraryBindings = AppLibraryBindings(
      List(
        AppPrototypeBindings(
          "app1",
          List(
            DeviceInstanceBinding("device1", BinarySensorBinding(BinarySensor.toString, -1)),
            DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 212)),
            DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 322)),
            DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 313))
          )
        )
      )
    )
    val appPrototypicalStructure = AppPrototypicalStructure(
      permissionLevel = NotPrivileged,
      deviceInstances = List(
        AppPrototypicalDeviceInstance("device1", BinarySensor),
        AppPrototypicalDeviceInstance("device2", Switch),
        AppPrototypicalDeviceInstance("device3", TemperatureSensor),
        AppPrototypicalDeviceInstance("device4", HumiditySensor)
      )
    )

    val appPrototypicalStructures: Map[String, AppPrototypicalStructure] = List(("app1", appPrototypicalStructure)).toMap
    val res = verifyBindingsKNXDatatypes(physicalStructure, appLibraryBindings, appPrototypicalStructures)
    res.length shouldEqual 1
    res.head shouldBe an[ErrorNotBoundToPhysicalDevice]
    res.head.msg.contains("device1") shouldEqual true
    res.head.msg.contains("not bound") shouldEqual true
  }

  "verifyBindingsKNXDatatypes" should "return an WarningKNXDatatypes for Unknown DPT for proto Switch and Error for wrong DPT in proto Temperature" in {

    val device1Physical = PhysicalDevice(
      "device1",
      ("1", "1", "1"),
      List(
        PhysicalDeviceNode(
          "device1Node1",
          List(PhysicalDeviceCommObject("device1Node1ComObj1", DPT1, In, 111))
        )
      )
    )
    val device2Physical = PhysicalDevice(
      "device2",
      ("1", "1", "2"),
      List(
        PhysicalDeviceNode(
          "device2Node1",
          List(
            PhysicalDeviceCommObject("device2Node1ComObj1", DPTUnknown, Out, 211),
            PhysicalDeviceCommObject("device2Node1ComObj2", DPTUnknown, InOut, 212)
          )
        )
      )
    )
    val device3Physical = PhysicalDevice(
      "device3",
      ("1", "1", "3"),
      List(
        PhysicalDeviceNode(
          "device3Node1",
          List(
            PhysicalDeviceCommObject("device3Node1ComObj1", DPT1, Out, 311),
            PhysicalDeviceCommObject("device3Node1ComObj2", DPT5, In, 312),
            PhysicalDeviceCommObject("device3Node1ComObj3", DPT9, Out, 313)
          )
        ),
        PhysicalDeviceNode(
          "device3Node2",
          List(
            PhysicalDeviceCommObject("device3Node2ComObj1", DPT5, In, 321),
            PhysicalDeviceCommObject("device3Node2ComObj2", DPT7, InOut, 322)
          )
        )
      )
    )
    val physicalStructure = PhysicalStructure(List(device1Physical, device2Physical, device3Physical))
    val appLibraryBindings = AppLibraryBindings(
      List(
        AppPrototypeBindings(
          "app1",
          List(
            DeviceInstanceBinding("device1", BinarySensorBinding(BinarySensor.toString, 311)),
            DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 212)),
            DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 322)),
            DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 313))
          )
        )
      )
    )
    val appPrototypicalStructure = AppPrototypicalStructure(
      permissionLevel = NotPrivileged,
      deviceInstances = List(
        AppPrototypicalDeviceInstance("device1", BinarySensor),
        AppPrototypicalDeviceInstance("device2", Switch),
        AppPrototypicalDeviceInstance("device3", TemperatureSensor),
        AppPrototypicalDeviceInstance("device4", HumiditySensor)
      )
    )
    val appPrototypicalStructures: Map[String, AppPrototypicalStructure] = List(("app1", appPrototypicalStructure)).toMap
    val res = verifyBindingsKNXDatatypes(physicalStructure, appLibraryBindings, appPrototypicalStructures)

    res.length shouldEqual 2
    res.exists(v => v.isInstanceOf[WarningKNXDatatype]) shouldEqual true
    res.exists(v => v.isInstanceOf[ErrorKNXDatatype]) shouldEqual true
    res.find(v => v.isInstanceOf[WarningKNXDatatype]).get.msg.contains("device2") shouldEqual true
    res.find(v => v.isInstanceOf[WarningKNXDatatype]).get.msg.contains("Unknown") shouldEqual true
    res.find(v => v.isInstanceOf[WarningKNXDatatype]).get.msg.contains("212") shouldEqual true
    res.find(v => v.isInstanceOf[ErrorKNXDatatype]).get.msg.contains("device3") shouldEqual true
    res.find(v => v.isInstanceOf[ErrorKNXDatatype]).get.msg.contains("322") shouldEqual true
    res.find(v => v.isInstanceOf[ErrorKNXDatatype]).get.msg.contains("DPT-9") shouldEqual true
    res.find(v => v.isInstanceOf[ErrorKNXDatatype]).get.msg.contains("DPT-7") shouldEqual true
  }

  "verifyBindingsPythonType" should "return Nil when no conflicting types exist" in {
    val appLibraryBindings = AppLibraryBindings(
      List(
        AppPrototypeBindings(
          "app1",
          List(
            DeviceInstanceBinding("device1", BinarySensorBinding(BinarySensor.toString, 311)),
            DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 212)),
            DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 322)),
            DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 313))
          )
        )
      )
    )
    val physicalStructure = PhysicalStructure(Nil)
    val mapPhysIdToGa = List(
      (311, GroupAddress(3, 1, 1)),
      (212, GroupAddress(2, 1, 2)),
      (322, GroupAddress(3, 2, 2)),
      (313, GroupAddress(3, 1, 1))
    ).toMap
    val groupAddressAssignment = GroupAddressAssignment(physicalStructure, appLibraryBindings, mapPhysIdToGa)
    verifyBindingsPythonType(groupAddressAssignment).isEmpty shouldBe true
  }

  "verifyBindingsPythonType" should "return an error when there are conflicting types on a group addresses" in {
    val appLibraryBindings = AppLibraryBindings(
      List(
        AppPrototypeBindings(
          "app1",
          List(
            DeviceInstanceBinding("device1", BinarySensorBinding(BinarySensor.toString, 311)),
            DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 212)),
            DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 322)),
            DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 311))
          )
        )
      )
    )
    val physicalStructure = PhysicalStructure(Nil)
    val mapPhysIdToGa = List(
      (311, GroupAddress(3, 1, 1)),
      (212, GroupAddress(2, 1, 2)),
      (322, GroupAddress(3, 2, 2)),
      (313, GroupAddress(3, 1, 1))
    ).toMap
    val groupAddressAssignment = GroupAddressAssignment(physicalStructure, appLibraryBindings, mapPhysIdToGa)
    val res = verifyBindingsPythonType(groupAddressAssignment)
    res.length shouldEqual 1
    res.exists(v => v.isInstanceOf[ErrorGroupAddressConflictingPythonTypes]) shouldEqual true
    res.head.msg.contains(PythonBool.toString) shouldBe true
    res.head.msg.contains(PythonFloat.toString) shouldBe true
    res.head.msg.contains(GroupAddress(3, 1, 1).toString) shouldBe true

  }

  "verifyBindingsMutualDPT" should "return Nil when no conflicting types exist" in {
    val appLibraryBindings = AppLibraryBindings(
      List(
        AppPrototypeBindings(
          "app1",
          List(
            DeviceInstanceBinding("device1", BinarySensorBinding(BinarySensor.toString, 311)),
            DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 212)),
            DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 322)),
            DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 313))
          )
        )
      )
    )
    verifyBindingsMutualDPT(appLibraryBindings).isEmpty shouldBe true
  }

  "verifyBindingsMutualDPT" should "return Nil when no conflicting types exist 2" in {
    val appLibraryBindings = AppLibraryBindings(
      List(
        AppPrototypeBindings(
          "app1",
          List(
            DeviceInstanceBinding("device1", BinarySensorBinding(BinarySensor.toString, 311)),
            DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 212)),
            DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 322)),
            DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 313)),
            DeviceInstanceBinding("device5", BinarySensorBinding(BinarySensor.toString, 212))
          )
        )
      )
    )
    verifyBindingsMutualDPT(appLibraryBindings).isEmpty shouldBe true
  }

  "verifyBindingsMutualDPT" should "return an error when conflicting DPT are bound to the same physical ID" in {
    val appLibraryBindings = AppLibraryBindings(
      List(
        AppPrototypeBindings(
          "app1",
          List(
            DeviceInstanceBinding("device1", BinarySensorBinding(BinarySensor.toString, 311)),
            DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 212)),
            DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 322)),
            DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 313)),
            DeviceInstanceBinding("device5", BinarySensorBinding(BinarySensor.toString, 322))
          )
        )
      )
    )
    val res = verifyBindingsMutualDPT(appLibraryBindings)
    println(res)
    res.length shouldEqual 2
    res.head.isInstanceOf[ErrorProtoDevicesBoundSameIdDifferentDPT] shouldEqual true
    res.last.isInstanceOf[ErrorProtoDevicesBoundSameIdDifferentDPT] shouldEqual true

    res.head.msg.contains(DPT1.toString) shouldBe true
    res.head.msg.contains(DPT9.toString) shouldBe true
    res.head.msg.contains("device3") shouldBe true
    res.head.msg.contains("device5") shouldBe true
    res.head.msg.contains("322") shouldBe true

    res.last.msg.contains(DPT1.toString) shouldBe true
    res.last.msg.contains(DPT9.toString) shouldBe true
    res.last.msg.contains("device3") shouldBe true
    res.last.msg.contains("device5") shouldBe true
    res.last.msg.contains("322") shouldBe true
  }
}
