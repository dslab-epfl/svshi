package ch.epfl.core.parser.json.physical

import ch.epfl.core.model.physical._
import ch.epfl.core.parser.json.{JsonParsingException, SystemStructureException}
import ch.epfl.core.utils.FileUtils

import java.nio.charset.StandardCharsets

/** Parser used to store and retrieve a PhysicalStructure through JSON
  */
object PhysicalStructureJsonParser {

  /** Parse a JSON file to produce a physical structure instance
    * @param filePath
    * @return
    */
  def parse(filePath: os.Path): PhysicalStructure = {
    constructPhysicalStructure(parseJson(FileUtils.readFileContentAsString(filePath)))
  }
  def constructPhysicalStructure(parsed: PhysicalStructureJson): PhysicalStructure = {
    def convertPhysDeviceCommObject(parsed: PhysicalDeviceCommObjectJson): PhysicalDeviceCommObject =
      PhysicalDeviceCommObject(parsed.name, KNXDatatype.fromString(parsed.datatype).get, IOType.fromString(parsed.ioType).get, parsed.id)
    def convertPhysDeviceNode(parsed: PhysicalDeviceNodeJson): PhysicalDeviceNode =
      PhysicalDeviceNode(parsed.name, parsed.comObjects.map(convertPhysDeviceCommObject))
    def convertPhysicalDevice(parsed: PhysicalDeviceJson): PhysicalDevice = {
      val addressArray = parsed.address.split('.')
      if (addressArray.length != 3)
        throw new SystemStructureException(
          s"The address for the device $parsed is not in the right format (i.e., x.x.x)"
        )
      val address = (addressArray(0), addressArray(1), addressArray(2))
      PhysicalDevice(parsed.name, address, parsed.nodes.map(convertPhysDeviceNode))
    }
    PhysicalStructure(parsed.deviceInstances.map(convertPhysicalDevice))
  }

  def parseJson(jsonContent: String): PhysicalStructureJson = {
    try {
      upickle.default.read[PhysicalStructureJson](jsonContent)
    } catch {
      case e: Exception =>
        throw new JsonParsingException("The given Json is not parsable, it has either a syntax error or the wrong structure.")
    }
  }

  def writeToFile(filePath: os.Path, physicalStructure: PhysicalStructure): Unit = {
    FileUtils.deleteIfExists(filePath)
    FileUtils.writeToFileOverwrite(filePath, physicalStructureToJson(physicalStructure).toString getBytes StandardCharsets.UTF_8)
  }

  def physicalStructureToJson(struct: PhysicalStructure): PhysicalStructureJson = {
    def addressToString(addr: (String, String, String)): String = List(addr._1, addr._2, addr._3).mkString(".")
    PhysicalStructureJson(
      struct.deviceInstances.map(device =>
        PhysicalDeviceJson(
          device.name,
          addressToString(device.address),
          device.nodes.map(node =>
            PhysicalDeviceNodeJson(
              node.name,
              node.comObjects.map(obj =>
                PhysicalDeviceCommObjectJson(
                  obj.name,
                  obj.datatype.toString,
                  obj.ioType.toString,
                  obj.id
                )
              )
            )
          )
        )
      )
    )
  }

}
