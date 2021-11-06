package ch.epfl.core.compiler.models

case class PhysicalStructure(deviceInstances: List[PhysicalDevice])

case class PhysicalDevice(name: String, address: (String, String, String), nodes: List[PhysicalDeviceNode])
case class PhysicalDeviceNode(name: String, channels: List[PhysicalDeviceChannel])
case class PhysicalDeviceChannel(name: String, datatype: KNXDatatype, ioType: IOType)
