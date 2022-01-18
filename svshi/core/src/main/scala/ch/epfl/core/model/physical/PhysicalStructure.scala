package ch.epfl.core.model.physical

import scala.util.hashing.MurmurHash3

/** Represents the physical structure of a physical KNX installation
  * @param deviceInstances
  */
case class PhysicalStructure(deviceInstances: List[PhysicalDevice])
case class PhysicalDevice(name: String, address: (String, String, String), nodes: List[PhysicalDeviceNode])
case class PhysicalDeviceNode(name: String, comObjects: List[PhysicalDeviceCommObject])
case class PhysicalDeviceCommObject(name: String, datatype: KNXDatatype, ioType: IOType, id: Int)
object PhysicalDeviceCommObject {
  def from(name: String, datatype: KNXDatatype, ioType: IOType, physicalAddress: (String, String, String), deviceNodeName: String): PhysicalDeviceCommObject =
    PhysicalDeviceCommObject(name, datatype, ioType, MurmurHash3.listHash(List(physicalAddress, deviceNodeName, name, datatype, ioType), MurmurHash3.seqSeed))
}
