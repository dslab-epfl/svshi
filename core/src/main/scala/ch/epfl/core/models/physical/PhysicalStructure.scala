package ch.epfl.core.models.physical

case class PhysicalStructure(deviceInstances: List[PhysicalDevice])
case class PhysicalDevice(
    name: String,
    address: (String, String, String),
    nodes: List[PhysicalDeviceNode]
)
case class PhysicalDeviceNode(
    name: String,
    comObjects: List[PhysicalDeviceCommObject]
)
case class PhysicalDeviceCommObject(
    name: String,
    datatype: KNXDatatype,
    ioType: IOType
)
