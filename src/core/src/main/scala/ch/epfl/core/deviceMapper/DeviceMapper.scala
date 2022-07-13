package ch.epfl.core.deviceMapper

import ch.epfl.core.deviceMapper.model.{DeviceMapping, StructureMapping, SupportedDeviceMapping, SupportedDeviceMappingNode}
import ch.epfl.core.model.physical._
import ch.epfl.core.model.prototypical._
import ch.epfl.core.parser.ets.EtsParser
import ch.epfl.core.parser.json.physical.PhysicalStructureJsonParser

object DeviceMapper {
  def mapStructure(physicalStructure: PhysicalStructure): StructureMapping = {
    val jsonStructure = PhysicalStructureJsonParser.physicalStructureToJson(physicalStructure)
    val deviceMappings = physicalStructure.deviceInstances.map(d => mapDevice(d))
    StructureMapping(jsonStructure, deviceMappings)
  }
  def mapDevice(physicalDevice: PhysicalDevice): DeviceMapping = {
    val physicalAddress = List(physicalDevice.address._1, physicalDevice.address._2, physicalDevice.address._3).mkString(".")
    val nodes = physicalDevice.nodes.map(n => SupportedDeviceMappingNode(name = n.name, supportedDeviceMappings = n.comObjects.flatMap(mapCommObject)))
    DeviceMapping(physicalAddress, nodes)
  }
  def mapCommObject(commObject: PhysicalDeviceCommObject): List[SupportedDeviceMapping] = {
    val dpt = commObject.datatype
    val ioType = commObject.ioType
    val supportedDevicesList = dptIoTypeToSupportedDevice(dpt, ioType)
    supportedDevicesList.map(supportedDevice => SupportedDeviceMapping(commObject.name, supportedDevice.toString, commObject.id))
  }
  def dptIoTypeToSupportedDevice(dpt: KNXDatatype, ioType: IOType): List[SupportedDevice] = {
    dpt match {
      case DPT1(_) if ioType == InOut                   => List(Switch, BinarySensor)
      case DPT1(_) if ioType == In                      => List(Switch)
      case DPT1(_) if ioType == Out                     => List(BinarySensor)
      case DPT1(_)                                      => Nil
      case DPT5(1) if ioType == InOut                   => List(DimmerSensor, DimmerActuator)
      case DPT5(1) if ioType == Out                     => List(DimmerSensor)
      case DPT5(1) if ioType == In                      => List(DimmerActuator)
      case DPT5(_)                                      => Nil
      case DPT6(_)                                      => Nil
      case DPT7(_)                                      => Nil
      case DPT9(1) if ioType == Out || ioType == InOut  => List(TemperatureSensor) // C°
      case DPT9(27) if ioType == Out || ioType == InOut => List(TemperatureSensor) // F°
      case DPT9(8) if ioType == Out || ioType == InOut  => List(CO2Sensor) // ppm
      case DPT9(7) if ioType == Out || ioType == InOut  => List(HumiditySensor) // % humidity
      case DPT9(_)                                      => Nil
      case DPT10(_)                                     => Nil
      case DPT11(_)                                     => Nil
      case DPT12(_)                                     => Nil
      case DPT13(_)                                     => Nil
      case DPT14(_)                                     => Nil
      case DPT16(_)                                     => Nil
      case DPT17(_)                                     => Nil
      case DPT18(_)                                     => Nil
      case DPT19(_)                                     => Nil
      case DPT20(_)                                     => Nil
      case DPTUnknown(_)                                => Nil
    }
  }
  def etsFileToMappingFile(inputEtsFile: os.Path, outputPath: os.Path): Unit = {
    val physicalStructure = EtsParser.parseEtsProjectFile(inputEtsFile)

  }
}
