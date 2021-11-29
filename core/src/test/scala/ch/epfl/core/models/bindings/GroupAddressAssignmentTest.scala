package ch.epfl.core.models.bindings

import ch.epfl.core.models.physical.{GroupAddress, PhysicalStructure}
import ch.epfl.core.models.prototypical.{AppLibraryBindings, AppPrototypeBindings, BinarySensor, BinarySensorBinding, DeviceInstanceBinding, HumiditySensor, HumiditySensorBinding, Switch, SwitchBinding, TemperatureSensor, TemperatureSensorBinding}
import ch.epfl.core.models.python.{PythonBool, PythonFloat}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class GroupAddressAssignmentTest extends AnyFlatSpec with Matchers {
  "getPythonTypes" should "return the correct list for correct input" in {
    val physStruct = PhysicalStructure(Nil)
    val appLibraryBindings = AppLibraryBindings(List(
      AppPrototypeBindings("app1", List(
        DeviceInstanceBinding("device1", BinarySensorBinding(BinarySensor.toString, 311)),
        DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 212)),
        DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 322)),
        DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 321)),
        DeviceInstanceBinding("device5", BinarySensorBinding(BinarySensor.toString, 321))
      ))
    ))
    val physIdToGA = List(
      (311, GroupAddress(3, 1, 1)),
      (212, GroupAddress(2, 1, 2)),
      (322, GroupAddress(3, 2, 2)),
      (321, GroupAddress(3, 2, 1))).toMap

    val assignment = GroupAddressAssignment(physStruct, appLibraryBindings, physIdToGA)
    val res = assignment.getPythonTypesMap
    res.contains(GroupAddress(3, 1, 1)) shouldEqual true
    res.contains(GroupAddress(2, 1, 2)) shouldEqual true
    res.contains(GroupAddress(3, 2, 2)) shouldEqual true
    res.contains(GroupAddress(3, 2, 1)) shouldEqual true
    res(GroupAddress(3, 1, 1)) shouldEqual List(PythonBool)
    res(GroupAddress(2, 1, 2)) shouldEqual List(PythonBool)
    res(GroupAddress(3, 2, 2)) shouldEqual List(PythonFloat)
    res(GroupAddress(3, 2, 1)).length shouldEqual 2
    res(GroupAddress(3, 2, 1)).contains(PythonBool) shouldEqual true
    res(GroupAddress(3, 2, 1)).contains(PythonFloat) shouldEqual true
  }

}
