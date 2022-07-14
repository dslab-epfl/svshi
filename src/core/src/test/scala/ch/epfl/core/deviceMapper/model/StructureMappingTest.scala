package ch.epfl.core.deviceMapper.model

import ch.epfl.core.TestUtils.convertToAnyShouldWrapper
import ch.epfl.core.deviceMapper.DeviceMapper
import ch.epfl.core.parser.json.physical._
import ch.epfl.core.utils.Constants
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

class StructureMappingTest extends AnyFlatSpec with BeforeAndAfterAll with BeforeAndAfterEach {
  private val endToEndResPath = Constants.SVSHI_SRC_FOLDER_PATH / "core" / "res" / "endToEnd"
  private val tempFolderPath: os.Path = endToEndResPath / "StructureMapping" / "tempTests"

  val device1 = PhysicalDeviceJson(
    "device1",
    "1.1.1",
    List(
      PhysicalDeviceNodeJson(
        "device1Node1",
        List(
          PhysicalDeviceCommObjectJson("device1Node1ComObj1", "DPT-1", "in", 111)
        )
      )
    )
  )
  val device2 = PhysicalDeviceJson(
    "device2",
    "1.1.2",
    List(
      PhysicalDeviceNodeJson(
        "device2Node1",
        List(
          PhysicalDeviceCommObjectJson("device2Node1ComObj1", "DPT-1", "out", 211),
          PhysicalDeviceCommObjectJson("device2Node1ComObj2", "DPT-Unknown", "in", 212)
        )
      )
    )
  )
  val device3 = PhysicalDeviceJson(
    "device3",
    "1.1.3",
    List(
      PhysicalDeviceNodeJson(
        "device3Node1",
        List(
          PhysicalDeviceCommObjectJson("device3Node1ComObj1", "DPT-1-5", "out", 311),
          PhysicalDeviceCommObjectJson("device3Node1ComObj2", "DPT-5-3", "in", 312)
        )
      ),
      PhysicalDeviceNodeJson(
        "device3Node2",
        List(
          PhysicalDeviceCommObjectJson("device3Node2ComObj1", "DPT-5-1", "in", 321),
          PhysicalDeviceCommObjectJson(
            "device3Node2ComObj2",
            "DPT-12-3",
            "in/out",
            322
          )
        )
      )
    )
  )
  val physicalStructure = PhysicalStructureJson(List(device1, device2, device3))

  override def beforeEach(): Unit = {
    os.remove.all(tempFolderPath)
    os.makeDir.all(tempFolderPath)
  }
  override def afterAll(): Unit = {
    os.remove.all(tempFolderPath)
  }

  "writing to file and parsing" should "be a reflective operation" in {
    val mapping: StructureMapping = DeviceMapper.mapStructure(PhysicalStructureJsonParser.constructPhysicalStructure(physicalStructure))
    val file = tempFolderPath / "mapping.json"
    mapping.writeToFile(file)
    val mappingAfter = StructureMapping.parseFromFile(file)
    mappingAfter shouldEqual mapping
  }
}
