package ch.epfl.core.models.prototypical

import ch.epfl.core.models.physical.{IOType, KNXDatatype}

case class AppPrototypicalStructure(deviceTypes: List[AppProtoDeviceType], deviceInstances: List[AppProtoDeviceInstance])

case class AppProtoDeviceType(name: String, channels: List[AppProtoDeviceChannel])
case class AppProtoDeviceInstance(name: String, typee: AppProtoDeviceType)
case class AppProtoDeviceChannel(name: String, datatype: KNXDatatype, ioType: IOType)
