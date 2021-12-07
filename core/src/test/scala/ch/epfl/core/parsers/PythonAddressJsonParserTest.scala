package ch.epfl.core.parsers

import ch.epfl.core.parsers.json.bindings.{AppPythonAddressesJson, BinarySensorAddressJson, DeviceAddressJson, HumiditySensorAddressJson, PythonAddressJsonParser, SwitchAddressJson, TemperatureSensorAddressJson}
import ch.epfl.core.models.application.{Application, NonPrivileged}
import ch.epfl.core.models.bindings.GroupAddressAssignment
import ch.epfl.core.models.physical.{DPT1, DPT12, DPT2, DPT5, GroupAddress, In, InOut, Out, PhysicalDevice, PhysicalDeviceCommObject, PhysicalDeviceNode, PhysicalStructure}
import ch.epfl.core.models.prototypical.{AppLibraryBindings, AppPrototypeBindings, AppPrototypicalDeviceInstance, AppPrototypicalStructure, BinarySensor, BinarySensorBinding, DeviceInstanceBinding, HumiditySensor, HumiditySensorBinding, Switch, SwitchBinding, TemperatureSensor, TemperatureSensorBinding}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class PythonAddressJsonParserTest extends AnyFlatSpec with Matchers {
  "assignmentToPythonAddressJson" should "return correct PythonAddressJson for correct inputs" in {
    val appPrototypicalStructure = AppPrototypicalStructure(List(
      AppPrototypicalDeviceInstance("device1", BinarySensor),
      AppPrototypicalDeviceInstance("device2", Switch),
      AppPrototypicalDeviceInstance("device3", TemperatureSensor)
    ), NonPrivileged)
    val app = Application("app1", "app1Path", appPrototypicalStructure)


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
    val appLibraryBindings = AppLibraryBindings(List(
      AppPrototypeBindings("app1", List(
        DeviceInstanceBinding("device1", BinarySensorBinding(BinarySensor.toString, 311)),
        DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 211)),
        DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 322)),
        DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 321))
      ))
    ))
    val mapping = List(
      (211, GroupAddress(2,1,1)),
      (212, GroupAddress(2,1,2)),
      (311, GroupAddress(3,1,1)),
      (321, GroupAddress(3,2,1)),
      (322, GroupAddress(3,2,2))
    ).toMap
    val assignment = GroupAddressAssignment(physicalStructure, appLibraryBindings, mapping)

    val expected = AppPythonAddressesJson(List(
      BinarySensorAddressJson("device1", "3/1/1"),
      SwitchAddressJson("device2", "2/1/1", "2/1/1"),
      TemperatureSensorAddressJson("device3", "3/2/2"),
      HumiditySensorAddressJson("device4", "3/2/1")
    ))
    PythonAddressJsonParser.assignmentToPythonAddressJson(app, assignment) shouldEqual expected
  }
}
