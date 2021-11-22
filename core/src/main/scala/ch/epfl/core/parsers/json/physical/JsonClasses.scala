package ch.epfl.core.parsers.json.physical

import upickle.default.{ReadWriter, macroRW}


case class PhysicalStructureJson(deviceInstances: List[PhysicalDeviceJson])
object PhysicalStructureJson {
  implicit val physicalStructure: ReadWriter[PhysicalStructureJson] = macroRW[PhysicalStructureJson]
}
case class PhysicalDeviceJson(name: String, address: String, nodes: List[PhysicalDeviceNodeJson])
object PhysicalDeviceJson {
  implicit val physicalDeviceJson: ReadWriter[PhysicalDeviceJson] = macroRW[PhysicalDeviceJson]
}

case class PhysicalDeviceNodeJson(name: String, comObjects: List[PhysicalDeviceCommObjectJson])
object PhysicalDeviceNodeJson {
  implicit val physicalDeviceNodeJson: ReadWriter[PhysicalDeviceNodeJson] = macroRW[PhysicalDeviceNodeJson]
}

case class PhysicalDeviceCommObjectJson(name: String, datatype: String, ioType: String, id: Int)
object PhysicalDeviceCommObjectJson {
  implicit val physicalDeviceCommObjectJson: ReadWriter[PhysicalDeviceCommObjectJson] = macroRW[PhysicalDeviceCommObjectJson]
}
