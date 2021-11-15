package ch.epfl.core.compiler.parsers

import ch.epfl.core.compiler.parsers.json.bindings.PythonAddressJsonParser
import ch.epfl.core.models.application.Application
import ch.epfl.core.models.physical.{DPT1, DPT12, DPT2, DPT5, In, InOut, Out, PhysicalDevice, PhysicalDeviceCommObject, PhysicalDeviceNode, PhysicalStructure}
import ch.epfl.core.models.prototypical.{AppPrototypicalDeviceInstance, AppPrototypicalStructure, BinarySensor, Switch, TemperatureSensor}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class PythonAddressJsonParserTest extends AnyFlatSpec with Matchers {
  "assignmentToPythonAddressJson" should "return correct PythonAddressJson for correct inputs" in {
    val appPrototypicalStructure = AppPrototypicalStructure(List(
      AppPrototypicalDeviceInstance("device1", BinarySensor),
      AppPrototypicalDeviceInstance("device2", Switch),
      AppPrototypicalDeviceInstance("device3", TemperatureSensor)
    ))
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
            PhysicalDeviceCommObject("device3Node2ComObj1", DPT5, In, 321),
            PhysicalDeviceCommObject("device3Node2ComObj2", DPT12, InOut, 322)
          )
        )
      )
    )
    val physicalStructure = PhysicalStructure(List(device1Physical, device2Physical, device3Physical))
  }
}
