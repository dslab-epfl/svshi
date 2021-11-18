package ch.epfl.core.verifier.bindings

import ch.epfl.core.models.physical._
import ch.epfl.core.models.prototypical._
import ch.epfl.core.verifier.bindings.Verifier._
import ch.epfl.core.verifier.bindings.exceptions.{ErrorIOType, ErrorKNXDatatype, ErrorNotBoundToPhysicalDevice, WarningIOType, WarningKNXDatatype}
import ch.epfl.core.models.physical._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class VerifierTest extends AnyFlatSpec with Matchers {
  "verifyBindings" should "throw nothing for valid inputs" in {

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
            PhysicalDeviceCommObject("device2Node1ComObj2", DPT5, In, 212)
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
            PhysicalDeviceCommObject("device3Node1ComObj3", DPT5, Out, 313),
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
    val appLibraryBindings = AppLibraryBindings(List(
      AppPrototypeBindings("app1", List(
        DeviceInstanceBinding("device1", BinarySensorBinding(BinarySensor.toString, 311)),
        DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 212, 211)),
        DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 322)),
        DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 313))
      ))
    ))
    val appPrototypicalStructure = AppPrototypicalStructure(List(
      AppPrototypicalDeviceInstance("device1", BinarySensor),
      AppPrototypicalDeviceInstance("device2", Switch),
      AppPrototypicalDeviceInstance("device3", TemperatureSensor),
      AppPrototypicalDeviceInstance("device4", HumiditySensor)
    ))
    val appPrototypicalStructures: Map[String, AppPrototypicalStructure] = List(("app1", appPrototypicalStructure)).toMap
    verifyBindings(physicalStructure, appLibraryBindings, appPrototypicalStructures) shouldEqual Nil
  }

  "verifyBindings" should "detects an OUT physical to IN proto Switch write" in {

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
            PhysicalDeviceCommObject("device2Node1ComObj2", DPT5, Out, 212)
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
            PhysicalDeviceCommObject("device3Node1ComObj3", DPT5, Out, 313),
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
    val appLibraryBindings = AppLibraryBindings(List(
      AppPrototypeBindings("app1", List(
        DeviceInstanceBinding("device1", BinarySensorBinding(BinarySensor.toString, 311)),
        DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 212, 211)),
        DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 322)),
        DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 313))
      ))
    ))
    val appPrototypicalStructure = AppPrototypicalStructure(List(
      AppPrototypicalDeviceInstance("device1", BinarySensor),
      AppPrototypicalDeviceInstance("device2", Switch),
      AppPrototypicalDeviceInstance("device3", TemperatureSensor),
      AppPrototypicalDeviceInstance("device4", HumiditySensor)
    ))
    val appPrototypicalStructures: Map[String, AppPrototypicalStructure] = List(("app1", appPrototypicalStructure)).toMap
    val res = verifyBindings(physicalStructure, appLibraryBindings, appPrototypicalStructures)
    res.length shouldEqual 1
    res.head shouldBe an [ErrorIOType]
    res.head.msg.contains("212") shouldEqual true
  }

  "verifyBindings" should "detects an IN physical to OUT proto Switch read" in {

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
            PhysicalDeviceCommObject("device2Node1ComObj1", DPT2, In, 211),
            PhysicalDeviceCommObject("device2Node1ComObj2", DPT5, In, 212)
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
            PhysicalDeviceCommObject("device3Node1ComObj3", DPT5, Out, 313),
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
    val appLibraryBindings = AppLibraryBindings(List(
      AppPrototypeBindings("app1", List(
        DeviceInstanceBinding("device1", BinarySensorBinding(BinarySensor.toString, 311)),
        DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 212, 211)),
        DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 322)),
        DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 313))
      ))
    ))
    val appPrototypicalStructure = AppPrototypicalStructure(List(
      AppPrototypicalDeviceInstance("device1", BinarySensor),
      AppPrototypicalDeviceInstance("device2", Switch),
      AppPrototypicalDeviceInstance("device3", TemperatureSensor),
      AppPrototypicalDeviceInstance("device4", HumiditySensor)
    ))
    val appPrototypicalStructures: Map[String, AppPrototypicalStructure] = List(("app1", appPrototypicalStructure)).toMap
    val res = verifyBindings(physicalStructure, appLibraryBindings, appPrototypicalStructures)
    res.length shouldEqual 1
    res.head shouldBe an [ErrorIOType]
    res.head.msg.contains("211") shouldEqual true
  }

  "verifyBindings" should "detects an IN physical to OUT proto HumiditySensor" in {

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
            PhysicalDeviceCommObject("device2Node1ComObj2", DPT5, In, 212)
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
            PhysicalDeviceCommObject("device3Node1ComObj3", DPT5, In, 313),
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
    val appLibraryBindings = AppLibraryBindings(List(
      AppPrototypeBindings("app1", List(
        DeviceInstanceBinding("device1", BinarySensorBinding(BinarySensor.toString, 311)),
        DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 212, 211)),
        DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 322)),
        DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 313))
      ))
    ))
    val appPrototypicalStructure = AppPrototypicalStructure(List(
      AppPrototypicalDeviceInstance("device1", BinarySensor),
      AppPrototypicalDeviceInstance("device2", Switch),
      AppPrototypicalDeviceInstance("device3", TemperatureSensor),
      AppPrototypicalDeviceInstance("device4", HumiditySensor)
    ))
    val appPrototypicalStructures: Map[String, AppPrototypicalStructure] = List(("app1", appPrototypicalStructure)).toMap
    val res = verifyBindings(physicalStructure, appLibraryBindings, appPrototypicalStructures)
    res.length shouldEqual 1
    res.head shouldBe an [ErrorIOType]
    res.head.msg.contains("313") shouldEqual true
  }

  "verifyBindings" should "detects an IN physical to OUT proto Temperature" in {

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
            PhysicalDeviceCommObject("device2Node1ComObj2", DPT5, In, 212)
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
            PhysicalDeviceCommObject("device3Node1ComObj3", DPT5, Out, 313),
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
    val appLibraryBindings = AppLibraryBindings(List(
      AppPrototypeBindings("app1", List(
        DeviceInstanceBinding("device1", BinarySensorBinding(BinarySensor.toString, 311)),
        DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 212, 211)),
        DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 322)),
        DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 313))
      ))
    ))
    val appPrototypicalStructure = AppPrototypicalStructure(List(
      AppPrototypicalDeviceInstance("device1", BinarySensor),
      AppPrototypicalDeviceInstance("device2", Switch),
      AppPrototypicalDeviceInstance("device3", TemperatureSensor),
      AppPrototypicalDeviceInstance("device4", HumiditySensor)
    ))
    val appPrototypicalStructures: Map[String, AppPrototypicalStructure] = List(("app1", appPrototypicalStructure)).toMap
    val res = verifyBindings(physicalStructure, appLibraryBindings, appPrototypicalStructures)
    res.length shouldEqual 1
    res.head shouldBe an [ErrorIOType]
    res.head.msg.contains("322") shouldEqual true
  }

  "verifyBindings" should "detects an IN physical to OUT proto Binary" in {

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
            PhysicalDeviceCommObject("device2Node1ComObj2", DPT5, In, 212)
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
            PhysicalDeviceCommObject("device3Node1ComObj3", DPT5, Out, 313),
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
    val appLibraryBindings = AppLibraryBindings(List(
      AppPrototypeBindings("app1", List(
        DeviceInstanceBinding("device1", BinarySensorBinding(BinarySensor.toString, 311)),
        DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 212, 211)),
        DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 322)),
        DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 313))
      ))
    ))
    val appPrototypicalStructure = AppPrototypicalStructure(List(
      AppPrototypicalDeviceInstance("device1", BinarySensor),
      AppPrototypicalDeviceInstance("device2", Switch),
      AppPrototypicalDeviceInstance("device3", TemperatureSensor),
      AppPrototypicalDeviceInstance("device4", HumiditySensor)
    ))
    val appPrototypicalStructures: Map[String, AppPrototypicalStructure] = List(("app1", appPrototypicalStructure)).toMap
    val res = verifyBindings(physicalStructure, appLibraryBindings, appPrototypicalStructures)
    res.length shouldEqual 1
    res.head shouldBe an [ErrorIOType]
    res.head.msg.contains("311") shouldEqual true
  }

  "verifyBindings" should "return Warning for an Unknown IOType for proto Binary" in {

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
            PhysicalDeviceCommObject("device2Node1ComObj2", DPT5, In, 212)
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
            PhysicalDeviceCommObject("device3Node1ComObj3", DPT5, Out, 313),
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
    val appLibraryBindings = AppLibraryBindings(List(
      AppPrototypeBindings("app1", List(
        DeviceInstanceBinding("device1", BinarySensorBinding(BinarySensor.toString, 311)),
        DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 212, 211)),
        DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 322)),
        DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 313))
      ))
    ))
    val appPrototypicalStructure = AppPrototypicalStructure(List(
      AppPrototypicalDeviceInstance("device1", BinarySensor),
      AppPrototypicalDeviceInstance("device2", Switch),
      AppPrototypicalDeviceInstance("device3", TemperatureSensor),
      AppPrototypicalDeviceInstance("device4", HumiditySensor)
    ))
    val appPrototypicalStructures: Map[String, AppPrototypicalStructure] = List(("app1", appPrototypicalStructure)).toMap
    val res = verifyBindings(physicalStructure, appLibraryBindings, appPrototypicalStructures)
    res.length shouldEqual 1
    res.head shouldBe an [WarningIOType]
    res.head.msg.contains("device1") shouldEqual true
    res.head.msg.contains("Unknown") shouldEqual true
  }

  "verifyBindings" should "return Warning for an Unknown IOType for proto Temperature" in {

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
            PhysicalDeviceCommObject("device2Node1ComObj2", DPT5, In, 212)
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
            PhysicalDeviceCommObject("device3Node1ComObj3", DPT5, Out, 313),
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
    val appLibraryBindings = AppLibraryBindings(List(
      AppPrototypeBindings("app1", List(
        DeviceInstanceBinding("device1", BinarySensorBinding(BinarySensor.toString, 311)),
        DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 212, 211)),
        DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 322)),
        DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 313))
      ))
    ))
    val appPrototypicalStructure = AppPrototypicalStructure(List(
      AppPrototypicalDeviceInstance("device1", BinarySensor),
      AppPrototypicalDeviceInstance("device2", Switch),
      AppPrototypicalDeviceInstance("device3", TemperatureSensor),
      AppPrototypicalDeviceInstance("device4", HumiditySensor)
    ))
    val appPrototypicalStructures: Map[String, AppPrototypicalStructure] = List(("app1", appPrototypicalStructure)).toMap
    val res = verifyBindings(physicalStructure, appLibraryBindings, appPrototypicalStructures)
    res.length shouldEqual 1
    res.head shouldBe an [WarningIOType]
    res.head.msg.contains("device3") shouldEqual true
    res.head.msg.contains("Unknown") shouldEqual true
  }

  "verifyBindings" should "return Warning for an Unknown IOType for proto Humidity" in {

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
            PhysicalDeviceCommObject("device2Node1ComObj2", DPT5, In, 212)
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
            PhysicalDeviceCommObject("device3Node1ComObj3", DPT5, Unknown, 313),
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
    val appLibraryBindings = AppLibraryBindings(List(
      AppPrototypeBindings("app1", List(
        DeviceInstanceBinding("device1", BinarySensorBinding(BinarySensor.toString, 311)),
        DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 212, 211)),
        DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 322)),
        DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 313))
      ))
    ))
    val appPrototypicalStructure = AppPrototypicalStructure(List(
      AppPrototypicalDeviceInstance("device1", BinarySensor),
      AppPrototypicalDeviceInstance("device2", Switch),
      AppPrototypicalDeviceInstance("device3", TemperatureSensor),
      AppPrototypicalDeviceInstance("device4", HumiditySensor)
    ))
    val appPrototypicalStructures: Map[String, AppPrototypicalStructure] = List(("app1", appPrototypicalStructure)).toMap
    val res = verifyBindings(physicalStructure, appLibraryBindings, appPrototypicalStructures)
    res.length shouldEqual 1
    res.head shouldBe an [WarningIOType]
    res.head.msg.contains("device4") shouldEqual true
    res.head.msg.contains("Unknown") shouldEqual true
  }

  "verifyBindings" should "return Warning for an an Unknown IOType for proto Switch read" in {

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
            PhysicalDeviceCommObject("device2Node1ComObj2", DPT5, In, 212)
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
            PhysicalDeviceCommObject("device3Node1ComObj3", DPT5, Out, 313),
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
    val appLibraryBindings = AppLibraryBindings(List(
      AppPrototypeBindings("app1", List(
        DeviceInstanceBinding("device1", BinarySensorBinding(BinarySensor.toString, 311)),
        DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 212, 211)),
        DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 322)),
        DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 313))
      ))
    ))
    val appPrototypicalStructure = AppPrototypicalStructure(List(
      AppPrototypicalDeviceInstance("device1", BinarySensor),
      AppPrototypicalDeviceInstance("device2", Switch),
      AppPrototypicalDeviceInstance("device3", TemperatureSensor),
      AppPrototypicalDeviceInstance("device4", HumiditySensor)
    ))
    val appPrototypicalStructures: Map[String, AppPrototypicalStructure] = List(("app1", appPrototypicalStructure)).toMap
    val res = verifyBindings(physicalStructure, appLibraryBindings, appPrototypicalStructures)
    res.length shouldEqual 1
    res.head shouldBe an [WarningIOType]
    res.head.msg.contains("device2") shouldEqual true
    res.head.msg.contains("Unknown") shouldEqual true
  }

  "verifyBindings" should "return Warning for an an Unknown IOType for proto Switch write" in {

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
            PhysicalDeviceCommObject("device3Node1ComObj3", DPT5, Out, 313),
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
    val appLibraryBindings = AppLibraryBindings(List(
      AppPrototypeBindings("app1", List(
        DeviceInstanceBinding("device1", BinarySensorBinding(BinarySensor.toString, 311)),
        DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 212, 211)),
        DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 322)),
        DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 313))
      ))
    ))
    val appPrototypicalStructure = AppPrototypicalStructure(List(
      AppPrototypicalDeviceInstance("device1", BinarySensor),
      AppPrototypicalDeviceInstance("device2", Switch),
      AppPrototypicalDeviceInstance("device3", TemperatureSensor),
      AppPrototypicalDeviceInstance("device4", HumiditySensor)
    ))
    val appPrototypicalStructures: Map[String, AppPrototypicalStructure] = List(("app1", appPrototypicalStructure)).toMap
    val res = verifyBindings(physicalStructure, appLibraryBindings, appPrototypicalStructures)
    res.length shouldEqual 1
    res.head shouldBe an [WarningIOType]
    res.head.msg.contains("device2") shouldEqual true
    res.head.msg.contains("Unknown") shouldEqual true
  }

  "verifyBindings" should "return Warning for an Unknown IOType for proto Humidity and an Error for wrong IO Type for proto Binary" in {

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
            PhysicalDeviceCommObject("device2Node1ComObj2", DPT5, In, 212)
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
            PhysicalDeviceCommObject("device3Node1ComObj3", DPT5, Unknown, 313),
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
    val appLibraryBindings = AppLibraryBindings(List(
      AppPrototypeBindings("app1", List(
        DeviceInstanceBinding("device1", BinarySensorBinding(BinarySensor.toString, 311)),
        DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 212, 211)),
        DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 322)),
        DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 313))
      ))
    ))
    val appPrototypicalStructure = AppPrototypicalStructure(List(
      AppPrototypicalDeviceInstance("device1", BinarySensor),
      AppPrototypicalDeviceInstance("device2", Switch),
      AppPrototypicalDeviceInstance("device3", TemperatureSensor),
      AppPrototypicalDeviceInstance("device4", HumiditySensor)
    ))
    val appPrototypicalStructures: Map[String, AppPrototypicalStructure] = List(("app1", appPrototypicalStructure)).toMap
    val res = verifyBindings(physicalStructure, appLibraryBindings, appPrototypicalStructures)
    res.length shouldEqual 2
    res.exists(v => v.isInstanceOf[WarningIOType]) shouldEqual true
    res.exists(v => v.isInstanceOf[ErrorIOType]) shouldEqual true
    res.find(v => v.isInstanceOf[WarningIOType]).get.msg.contains("device4") shouldEqual true
    res.find(v => v.isInstanceOf[WarningIOType]).get.msg.contains("Unknown") shouldEqual true
    res.find(v => v.isInstanceOf[ErrorIOType]).get.msg.contains("device1") shouldEqual true
    res.find(v => v.isInstanceOf[ErrorIOType]).get.msg.contains("311") shouldEqual true
  }

  "verifyBindings" should "return Error for an unmapped physical device for proto Binary" in {

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
            PhysicalDeviceCommObject("device2Node1ComObj2", DPT5, In, 212)
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
            PhysicalDeviceCommObject("device3Node1ComObj3", DPT5, Out, 313),
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
    val appLibraryBindings = AppLibraryBindings(List(
      AppPrototypeBindings("app1", List(
        DeviceInstanceBinding("device1", BinarySensorBinding(BinarySensor.toString, -1)),
        DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 212, 211)),
        DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 322)),
        DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 313))
      ))
    ))
    val appPrototypicalStructure = AppPrototypicalStructure(List(
      AppPrototypicalDeviceInstance("device1", BinarySensor),
      AppPrototypicalDeviceInstance("device2", Switch),
      AppPrototypicalDeviceInstance("device3", TemperatureSensor),
      AppPrototypicalDeviceInstance("device4", HumiditySensor)
    ))

    val appPrototypicalStructures: Map[String, AppPrototypicalStructure] = List(("app1", appPrototypicalStructure)).toMap
    val res = verifyBindings(physicalStructure, appLibraryBindings, appPrototypicalStructures)
    res.length shouldEqual 1
    res.head shouldBe an [ErrorNotBoundToPhysicalDevice]
    res.head.msg.contains("device1") shouldEqual true
    res.head.msg.contains("not bound") shouldEqual true
  }

  "verifyBindings" should "return Error for an unmapped physical device for proto Binary and an Error for wrong IO type for proto Temperature" in {

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
            PhysicalDeviceCommObject("device2Node1ComObj2", DPT5, In, 212)
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
            PhysicalDeviceCommObject("device3Node1ComObj3", DPT5, Out, 313),
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
    val appLibraryBindings = AppLibraryBindings(List(
      AppPrototypeBindings("app1", List(
        DeviceInstanceBinding("device1", BinarySensorBinding(BinarySensor.toString, -1)),
        DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 212, 211)),
        DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 322)),
        DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 313))
      ))
    ))
    val appPrototypicalStructure = AppPrototypicalStructure(List(
      AppPrototypicalDeviceInstance("device1", BinarySensor),
      AppPrototypicalDeviceInstance("device2", Switch),
      AppPrototypicalDeviceInstance("device3", TemperatureSensor),
      AppPrototypicalDeviceInstance("device4", HumiditySensor)
    ))

    val appPrototypicalStructures: Map[String, AppPrototypicalStructure] = List(("app1", appPrototypicalStructure)).toMap
    val res = verifyBindings(physicalStructure, appLibraryBindings, appPrototypicalStructures)

    res.length shouldEqual 2
    res.exists(v => v.isInstanceOf[ErrorNotBoundToPhysicalDevice]) shouldEqual true
    res.exists(v => v.isInstanceOf[ErrorIOType]) shouldEqual true
    res.find(v => v.isInstanceOf[ErrorIOType]).get.msg.contains("device3") shouldEqual true
    res.find(v => v.isInstanceOf[ErrorIOType]).get.msg.contains("322") shouldEqual true
    res.find(v => v.isInstanceOf[ErrorNotBoundToPhysicalDevice]).get.msg.contains("device1") shouldEqual true
    res.find(v => v.isInstanceOf[ErrorNotBoundToPhysicalDevice]).get.msg.contains("not bound") shouldEqual true
  }
}
