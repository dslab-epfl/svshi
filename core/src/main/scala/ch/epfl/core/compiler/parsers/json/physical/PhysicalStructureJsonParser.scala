package ch.epfl.core.compiler.parsers.json.physical

import ch.epfl.core.compiler.parsers.json.{
  JsonParsingException,
  SystemStructureException
}
import ch.epfl.core.models.physical._
import upickle.default.write

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import scala.io.Source
import scala.util.Using

object PhysicalStructureJsonParser {
  def parse(filePath: String): PhysicalStructure = {
    Using(Source.fromFile(filePath)) { fileBuff =>
      constructPhysicalStructure(
        parseJson(fileBuff.getLines().toList.mkString)
      )
    }.get
  }
  def constructPhysicalStructure(
      parsed: PhysicalStructureJson
  ): PhysicalStructure = {
    def convertPhysDeviceCommObject(
        parsed: PhysicalDeviceCommObjectJson
    ): PhysicalDeviceCommObject =
      PhysicalDeviceCommObject(
        parsed.name,
        KNXDatatype.fromString(parsed.datatype).get,
        IOType.fromString(parsed.ioType).get
      )
    def convertPhysDeviceNode(
        parsed: PhysicalDeviceNodeJson
    ): PhysicalDeviceNode =
      PhysicalDeviceNode(
        parsed.name,
        parsed.comObjects.map(convertPhysDeviceCommObject)
      )
    def convertPhysicalDevice(parsed: PhysicalDeviceJson): PhysicalDevice = {
      val addressArray = parsed.address.split('.')
      if (addressArray.length != 3)
        throw new SystemStructureException(
          s"The address for the device $parsed is not in the right format (i.e., x.x.x)"
        )
      val address = (addressArray(0), addressArray(1), addressArray(2))
      PhysicalDevice(
        parsed.name,
        address,
        parsed.nodes.map(convertPhysDeviceNode)
      )
    }
    PhysicalStructure(parsed.deviceInstances.map(convertPhysicalDevice))
  }

  def parseJson(jsonContent: String): PhysicalStructureJson = {
    try {
      upickle.default.read[PhysicalStructureJson](jsonContent)
    } catch {
      case e: Exception =>
        throw new JsonParsingException(
          "The given Json is not parsable, it has either a syntax error or the wrong structure."
        )
    }
  }

  def writeToFile(
      filePath: String,
      physicalStructure: PhysicalStructure
  ): Unit = {
    val f = Paths.get(filePath).toFile
    if(f.exists()) f.delete() // So that we get a fresh copy
    Files.write(
      Paths.get(filePath),
      write(
        physicalStructureToJson(physicalStructure)
      ) getBytes StandardCharsets.UTF_8
    )
  }

  def physicalStructureToJson(
      struct: PhysicalStructure
  ): PhysicalStructureJson = {
    def addressToString(addr: (String, String, String)): String =
      List(addr._1, addr._2, addr._3).mkString(".")
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
                  obj.ioType.toString
                )
              )
            )
          )
        )
      )
    )
  }

}
