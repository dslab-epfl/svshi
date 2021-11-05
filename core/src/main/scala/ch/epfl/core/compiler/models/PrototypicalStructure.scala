package ch.epfl.core.compiler.models

case class PrototypicalStructure(deviceTypes: List[DeviceType], deviceInstances: List[DeviceInstance])

case class DeviceType(name: String, channels: List[DeviceChannel])
case class DeviceInstance(name: String, typee: DeviceType)
case class DeviceChannel(name: String, datatype: KNXDatatype, ioType: IOType)
