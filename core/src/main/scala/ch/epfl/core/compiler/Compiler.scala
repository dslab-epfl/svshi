package ch.epfl.core.compiler

import ch.epfl.core.models.application.ApplicationLibrary
import ch.epfl.core.compiler.programming.Programmer
import ch.epfl.core.models.bindings.GroupAddressAssignment
import ch.epfl.core.models.physical._
import ch.epfl.core.models.prototypical._

object Compiler {
  def compile(library: ApplicationLibrary): ApplicationLibrary = {
    //TODO
    // here we need to read assignment of physical communicationObjects to XKNX stuff
    // assign Group addresses to communicationObject
    // generate files for the python apps with the group addresses
    // generate files for the KNX programmming module
    // val struct = PhysicalStructure(
    //   PhysicalDevice(
    //     "device1",
    //     ("1", "2", "3"),
    //     PhysicalDeviceNode(
    //       "node1",
    //       PhysicalDeviceCommObject("commObj1", KNXDatatype.fromString("DPT-1").get, IOType.fromString("in").get, 1) :: PhysicalDeviceCommObject(
    //         "commObj10",
    //         KNXDatatype.fromString("DPT-12").get,
    //         IOType.fromString("in").get, 2
    //       ) :: Nil
    //     ) :: Nil
    //   ) :: PhysicalDevice(
    //     "device2",
    //     ("1", "2", "4"),
    //     PhysicalDeviceNode("node2", PhysicalDeviceCommObject("commObj2", KNXDatatype.fromString("DPT-2").get, IOType.fromString("in/out").get, 3) :: Nil) :: Nil
    //   ) :: Nil
    // )
    // val bindings = AppLibraryBindings(
    //   AppPrototypeBindings(
    //     "test",
    //     DeviceInstanceBinding("device1", TemperatureSensorBinding("temp", 1)) :: DeviceInstanceBinding("device2", SwitchBinding("switch", 2, 3)) :: Nil
    //   ) :: Nil
    // )
    // val mapping = Map(1 -> GroupAddress(1, 1, 1), 2 -> GroupAddress(1, 2, 1), 3 -> GroupAddress(1, 2, 2))
    // Programmer.outputProgrammingFile(GroupAddressAssignment(struct, bindings, mapping))
    library
  }

}
