package ch.epfl.core.model.prototypical

/** Represents a device supported by the platform, i.e., that can be used in a PrototypicalStructure.
  * New classes are intended to be added here to extend the prototypical devices offering
  */
sealed trait SupportedDevice {
  def toString: String
}

object SupportedDevice {
  val defaultPhysicalId: Int = -1
  final val binarySensorString = "binarySensor"
  private final val binarySensorStringLower = binarySensorString.toLowerCase

  final val switchString = "switch"
  private final val switchStringLower = switchString.toLowerCase

  final val temperatureSensorString = "temperatureSensor"
  private final val temperatureSensorStringLower = temperatureSensorString.toLowerCase

  final val humiditySensorString = "humiditySensor"
  private final val humiditySensorStringLower = humiditySensorString.toLowerCase

  final val co2SensorString = "co2Sensor"
  private final val co2SensorStringLower = co2SensorString.toLowerCase

  final val dimmerSensorString = "dimmerSensor"
  private final val dimmerSensorStringLower = dimmerSensorString.toLowerCase

  final val dimmerActuatorString = "dimmerActuator"
  private final val dimmerActuatorStringLower = dimmerActuatorString.toLowerCase

  def fromString(s: String): SupportedDevice = s.toLowerCase match {
    case SupportedDevice.binarySensorStringLower      => BinarySensor
    case SupportedDevice.switchStringLower            => Switch
    case SupportedDevice.temperatureSensorStringLower => TemperatureSensor
    case SupportedDevice.humiditySensorStringLower    => HumiditySensor
    case SupportedDevice.co2SensorStringLower         => CO2Sensor
    case SupportedDevice.dimmerSensorStringLower      => DimmerSensor
    case SupportedDevice.dimmerActuatorStringLower    => DimmerActuator
    case _                                            => throw new UnsupportedDeviceException(s"The device '$s' is not supported by SVSHI")
  }
  def getDeviceBinding(deviceType: SupportedDevice): SupportedDeviceBinding = deviceType match {
    case BinarySensor      => BinarySensorBinding(deviceType.toString, defaultPhysicalId)
    case Switch            => SwitchBinding(deviceType.toString, defaultPhysicalId)
    case TemperatureSensor => TemperatureSensorBinding(deviceType.toString, defaultPhysicalId)
    case HumiditySensor    => HumiditySensorBinding(deviceType.toString, defaultPhysicalId)
    case CO2Sensor         => CO2SensorBinding(deviceType.toString, defaultPhysicalId)
    case DimmerSensor      => DimmerSensorBinding(deviceType.toString, defaultPhysicalId)
    case DimmerActuator    => DimmerActuatorBinding(deviceType.toString, defaultPhysicalId)
  }
  def getAvailableDevices: List[String] =
    List(binarySensorString, switchString, temperatureSensorString, humiditySensorString, co2SensorString, dimmerSensorString, dimmerActuatorString)
}

case object BinarySensor extends SupportedDevice {
  override def toString: String = SupportedDevice.binarySensorString
}
case object Switch extends SupportedDevice {
  override def toString: String = SupportedDevice.switchString
}
case object TemperatureSensor extends SupportedDevice {
  override def toString: String = SupportedDevice.temperatureSensorString
}
case object HumiditySensor extends SupportedDevice {
  override def toString: String = SupportedDevice.humiditySensorString
}
case object CO2Sensor extends SupportedDevice {
  override def toString: String = SupportedDevice.co2SensorString
}
case object DimmerSensor extends SupportedDevice {
  override def toString: String = SupportedDevice.dimmerSensorString
}
case object DimmerActuator extends SupportedDevice {
  override def toString: String = SupportedDevice.dimmerActuatorString
}

class UnsupportedDeviceException(msg: String) extends Exception(msg)
