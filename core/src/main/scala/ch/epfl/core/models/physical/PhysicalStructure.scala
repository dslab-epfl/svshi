package ch.epfl.core.models.physical

import scala.util.hashing.MurmurHash3

case class PhysicalStructure(deviceInstances: List[PhysicalDevice])
case class PhysicalDevice(name: String, address: (String, String, String), nodes: List[PhysicalDeviceNode])
case class PhysicalDeviceNode(name: String, comObjects: List[PhysicalDeviceCommObject])
case class PhysicalDeviceCommObject(name: String, datatype: KNXDatatype, ioType: IOType, id: Int)
object PhysicalDeviceCommObject{
  def from(name: String, datatype: KNXDatatype, ioType: IOType, physicalAddress: (String, String, String)): PhysicalDeviceCommObject = PhysicalDeviceCommObject(name, datatype, ioType, MurmurHash3.listHash(List(physicalAddress, name, datatype, ioType), MurmurHash3.seqSeed))
}

