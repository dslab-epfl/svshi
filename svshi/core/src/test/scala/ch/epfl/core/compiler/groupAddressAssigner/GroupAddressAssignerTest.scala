package ch.epfl.core.compiler.groupAddressAssigner

import ch.epfl.core.compiler.bindings.groupAddressAssigner.GroupAddressAssigner
import ch.epfl.core.model.physical._
import ch.epfl.core.model.prototypical._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class GroupAddressAssignerTest extends AnyFlatSpec with Matchers {
  "assignGroupAddressesToPhysical" should "return an assignment in which each used id has an address and all addresses are unique" in {
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

    val appLibraryBindings = AppLibraryBindings(
      List(
        AppPrototypeBindings(
          "app1",
          List(
            DeviceInstanceBinding("device1", BinarySensorBinding(BinarySensor.toString, 311)),
            DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 212)),
            DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 322)),
            DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 321))
          )
        )
      )
    )

    val result = GroupAddressAssigner.assignGroupAddressesToPhysical(physicalStructure, appLibraryBindings)

    result.physStruct shouldEqual physicalStructure
    result.appLibraryBindings shouldEqual appLibraryBindings

    result.physIdToGA.contains(311) shouldBe true
    result.physIdToGA.contains(212) shouldBe true
    result.physIdToGA.contains(322) shouldBe true
    result.physIdToGA.contains(321) shouldBe true

    val ga = List(result.physIdToGA(311), result.physIdToGA(212), result.physIdToGA(321), result.physIdToGA(322))
    ga.length shouldEqual ga.toSet.size

  }
}
