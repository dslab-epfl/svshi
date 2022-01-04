package ch.epfl.core.compiler.knxProgramming

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import ch.epfl.core.model.bindings.GroupAddressAssignment
import ch.epfl.core.model.physical.{PhysicalStructure, PhysicalDevice, PhysicalDeviceNode, PhysicalDeviceCommObject, DPT1, DPT9, DPTUnknown, In, Out}
import ch.epfl.core.model.prototypical.{AppLibraryBindings, AppPrototypeBindings, DeviceInstanceBinding, HumiditySensorBinding, BinarySensorBinding, TemperatureSensorBinding}
import ch.epfl.core.model.physical.GroupAddress
import ch.epfl.core.utils.Constants.ASSIGNMENTS_DIRECTORY_NAME
import com.github.tototoshi.csv.CSVReader

class KNXProgrammerTest extends AnyFlatSpec with Matchers {

  private val assignment = GroupAddressAssignment(
    PhysicalStructure(
      List(
        PhysicalDevice("device1", ("1", "1", "1"), List(PhysicalDeviceNode("node1", List(PhysicalDeviceCommObject("commObject1", DPT9, In, 1))))),
        PhysicalDevice(
          "device2",
          ("1", "1", "2"),
          List(PhysicalDeviceNode("node2", List(PhysicalDeviceCommObject("commObject2", DPT1, Out, 2), PhysicalDeviceCommObject("commObject3", DPTUnknown, Out, 3))))
        )
      )
    ),
    AppLibraryBindings(
      List(
        AppPrototypeBindings(
          "app1",
          List(
            DeviceInstanceBinding("device1", HumiditySensorBinding("humidity", 1)),
            DeviceInstanceBinding("device2", BinarySensorBinding("binary", 2)),
            DeviceInstanceBinding("device2", TemperatureSensorBinding("temperature", 3))
          )
        )
      )
    ),
    Map(1 -> GroupAddress(2, 1, 1), 2 -> GroupAddress(2, 1, 2), 3 -> GroupAddress(2, 1, 3))
  )
  private val programmer = Programmer(assignment)
  private val directoryPath = os.Path(ASSIGNMENTS_DIRECTORY_NAME)

  "outputGroupAddressesCsv" should "output the right file given the assignment" in {
    val expectedLines = List(
      List("Main", "Middle", "Sub", "Address", "Central", "Unfiltered", "Description", "DatapointType", "Security"),
      List("New main group", "", "", "0/-/-", "", "", "", "", "Auto"),
      List("", "New middle group", "", "0/0/-", "", "", "", "", "Auto"),
      List("", "", "1.1.1 - device1 - commObject1", "2/1/1", "", "", "", "DPT-9", "Auto"),
      List("", "", "1.1.2 - device2 - commObject2", "2/1/2", "", "", "", "DPT-1", "Auto"),
      List("", "", "1.1.2 - device2 - commObject3", "2/1/3", "", "", "", "DPT-9", "Auto")
    )

    val filename = "assignment_test.csv"
    programmer.outputGroupAddressesCsv(filename)

    val filePath = directoryPath / filename

    val reader = CSVReader.open(filePath.toIO)
    val lines = reader.all()

    lines shouldEqual expectedLines

    reader.close()

    // Cleanup
    os.remove.all(filePath)
  }

  "outputProgrammingFile" should "output the right file given the assignment" in {
    val expectedFile = """
device address '1.1.2'
  node 'node2'
    comm object 'commObject2' => group address '2/1/2'
    comm object 'commObject3' => group address '2/1/3'
device address '1.1.1'
  node 'node1'
    comm object 'commObject1' => group address '2/1/1'    
    """.strip()

    val filename = "assignment_test.txt"
    programmer.outputProgrammingFile(filename)

    val filePath = directoryPath / filename

    val file = os.read(filePath)

    file shouldEqual expectedFile

    // Cleanup
    os.remove.all(filePath)
  }
}
