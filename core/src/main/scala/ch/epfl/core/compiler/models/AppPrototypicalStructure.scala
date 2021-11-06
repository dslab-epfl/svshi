package ch.epfl.core.compiler.models

case class AppPrototypicalStructure(deviceTypes: List[AppProtoDeviceType], deviceInstances: List[AppProtoDeviceInstance])

case class AppProtoDeviceType(name: String, channels: List[AppProtoDeviceChannel])
case class AppProtoDeviceInstance(name: String, typee: AppProtoDeviceType)
case class AppProtoDeviceChannel(name: String, datatype: KNXDatatype, ioType: IOType)
