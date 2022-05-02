package ch.epfl.core.model.prototypical

/** Represents a device supported by the platform, i.e., that can be used in a PrototypicalStructure.
  * New classes are intended to be added here to extend the prototypical devices offering
  */
sealed trait SupportedDevice

object SupportedDevice {
  val defaultPhysicalId: Int = -1
  final val binarySensorString = "binary"
  final val switchString = "switch"
  final val temperatureSensorString = "temperature"
  final val humiditySensorString = "humidity"
  final val co2SensorString = "co2"
  def fromString(s: String): SupportedDevice = s.toLowerCase match {
    case SupportedDevice.binarySensorString      => BinarySensor
    case SupportedDevice.switchString            => Switch
    case SupportedDevice.temperatureSensorString => TemperatureSensor
    case SupportedDevice.humiditySensorString    => HumiditySensor
    case SupportedDevice.co2SensorString         => CO2Sensor
  }
  def getDeviceBinding(deviceType: SupportedDevice): SupportedDeviceBinding = deviceType match {
    case BinarySensor      => BinarySensorBinding(deviceType.toString, defaultPhysicalId)
    case Switch            => SwitchBinding(deviceType.toString, defaultPhysicalId)
    case TemperatureSensor => TemperatureSensorBinding(deviceType.toString, defaultPhysicalId)
    case HumiditySensor    => HumiditySensorBinding(deviceType.toString, defaultPhysicalId)
    case CO2Sensor         => CO2SensorBinding(deviceType.toString, defaultPhysicalId)
  }
  def getAvailableDevices: List[String] = List(binarySensorString, switchString, temperatureSensorString, humiditySensorString, co2SensorString)
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

class UnsupportedDeviceException(msg: String) extends Exception(msg)
