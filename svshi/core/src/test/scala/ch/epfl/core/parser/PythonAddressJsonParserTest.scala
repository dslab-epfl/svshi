package ch.epfl.core.parser

import ch.epfl.core.model.application.{Application, NotPrivileged, Privileged}
import ch.epfl.core.model.bindings.GroupAddressAssignment
import ch.epfl.core.model.physical._
import ch.epfl.core.model.prototypical._
import ch.epfl.core.parser.json.bindings._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class PythonAddressJsonParserTest extends AnyFlatSpec with Matchers {
  "assignmentToPythonAddressJson" should "return correct PythonAddressJson for correct inputs notPrivileged" in {
    val appPrototypicalStructure = AppPrototypicalStructure(
      permissionLevel = NotPrivileged,
      timer = 60,
      files = List("file1.txt", "file2.png"),
      deviceInstances = List(
        AppPrototypicalDeviceInstance("device1", BinarySensor),
        AppPrototypicalDeviceInstance("device2", Switch),
        AppPrototypicalDeviceInstance("device3", TemperatureSensor)
      )
    )
    val app = Application("app1", os.Path("/app1Path"), appPrototypicalStructure)

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
            PhysicalDeviceCommObject("device3Node1ComObj1", DPT1, Out, 311),
            PhysicalDeviceCommObject("device3Node1ComObj2", DPT5, In, 312)
          )
        ),
        PhysicalDeviceNode(
          "device3Node2",
          List(
            PhysicalDeviceCommObject("device3Node2ComObj1", DPT5, Out, 321),
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
            DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 211)),
            DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 322)),
            DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 321))
          )
        )
      )
    )
    val mapping = List(
      (211, GroupAddress(2, 1, 1)),
      (212, GroupAddress(2, 1, 2)),
      (311, GroupAddress(3, 1, 1)),
      (321, GroupAddress(3, 2, 1)),
      (322, GroupAddress(3, 2, 2))
    ).toMap
    val assignment = GroupAddressAssignment(physicalStructure, appLibraryBindings, mapping)

    val expected = AppPythonAddressesJson(
      permissionLevel = NotPrivileged.toString,
      timer = 60,
      addresses = List(
        BinarySensorAddressJson("device1", "3/1/1"),
        SwitchAddressJson("device2", "2/1/1", "2/1/1"),
        TemperatureSensorAddressJson("device3", "3/2/2"),
        HumiditySensorAddressJson("device4", "3/2/1")
      )
    )
    PythonAddressJsonParser.assignmentToPythonAddressJson(app, assignment) shouldEqual expected
  }
  "assignmentToPythonAddressJson" should "return correct PythonAddressJson for correct inputs co2" in {
    val appPrototypicalStructure = AppPrototypicalStructure(
      permissionLevel = NotPrivileged,
      timer = 60,
      files = List("file1.txt", "file2.png"),
      deviceInstances = List(
        AppPrototypicalDeviceInstance("device1", BinarySensor),
        AppPrototypicalDeviceInstance("device2", Switch),
        AppPrototypicalDeviceInstance("device3", CO2Sensor)
      )
    )
    val app = Application("app1", os.Path("/app1Path"), appPrototypicalStructure)

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
            PhysicalDeviceCommObject("device3Node1ComObj1", DPT1, Out, 311),
            PhysicalDeviceCommObject("device3Node1ComObj2", DPT5, In, 312)
          )
        ),
        PhysicalDeviceNode(
          "device3Node2",
          List(
            PhysicalDeviceCommObject("device3Node2ComObj1", DPT5, Out, 321),
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
            DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 211)),
            DeviceInstanceBinding("device3", CO2SensorBinding(TemperatureSensor.toString, 322)),
            DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 321))
          )
        )
      )
    )
    val mapping = List(
      (211, GroupAddress(2, 1, 1)),
      (212, GroupAddress(2, 1, 2)),
      (311, GroupAddress(3, 1, 1)),
      (321, GroupAddress(3, 2, 1)),
      (322, GroupAddress(3, 2, 2))
    ).toMap
    val assignment = GroupAddressAssignment(physicalStructure, appLibraryBindings, mapping)

    val expected = AppPythonAddressesJson(
      permissionLevel = NotPrivileged.toString,
      timer = 60,
      addresses = List(
        BinarySensorAddressJson("device1", "3/1/1"),
        SwitchAddressJson("device2", "2/1/1", "2/1/1"),
        CO2SensorAddressJson("device3", "3/2/2"),
        HumiditySensorAddressJson("device4", "3/2/1")
      )
    )
    PythonAddressJsonParser.assignmentToPythonAddressJson(app, assignment) shouldEqual expected
  }
  "assignmentToPythonAddressJson" should "return correct PythonAddressJson for correct inputs privileged" in {
    val appPrototypicalStructure = AppPrototypicalStructure(
      permissionLevel = Privileged,
      timer = 60,
      files = List("file1.txt", "file2.png"),
      deviceInstances = List(
        AppPrototypicalDeviceInstance("device1", BinarySensor),
        AppPrototypicalDeviceInstance("device2", Switch),
        AppPrototypicalDeviceInstance("device3", TemperatureSensor)
      )
    )
    val app = Application("app1", os.Path("/app1Path"), appPrototypicalStructure)

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
            PhysicalDeviceCommObject("device3Node1ComObj1", DPT1, Out, 311),
            PhysicalDeviceCommObject("device3Node1ComObj2", DPT5, In, 312)
          )
        ),
        PhysicalDeviceNode(
          "device3Node2",
          List(
            PhysicalDeviceCommObject("device3Node2ComObj1", DPT5, Out, 321),
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
            DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 211)),
            DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 322)),
            DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 321))
          )
        )
      )
    )
    val mapping = List(
      (211, GroupAddress(2, 1, 1)),
      (212, GroupAddress(2, 1, 2)),
      (311, GroupAddress(3, 1, 1)),
      (321, GroupAddress(3, 2, 1)),
      (322, GroupAddress(3, 2, 2))
    ).toMap
    val assignment = GroupAddressAssignment(physicalStructure, appLibraryBindings, mapping)

    val expected = AppPythonAddressesJson(
      permissionLevel = Privileged.toString,
      timer = 60,
      addresses = List(
        BinarySensorAddressJson("device1", "3/1/1"),
        SwitchAddressJson("device2", "2/1/1", "2/1/1"),
        TemperatureSensorAddressJson("device3", "3/2/2"),
        HumiditySensorAddressJson("device4", "3/2/1")
      )
    )
    PythonAddressJsonParser.assignmentToPythonAddressJson(app, assignment) shouldEqual expected
  }
}
