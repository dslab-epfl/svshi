package ch.epfl.core.model.prototypical

/** Represents a device supported by the platform, i.e., that can be used in a PrototypicalStructure.
  * New classes are intended to be added here to extend the prototypical devices offering
  */
sealed trait SupportedDevice

object SupportedDevice {
  val defaultPhysicalId: Int = -1
  final val binarySensorString = "binary"
  final val switchString = "switch"
  final val temperatureSensor = "temperature"
  final val humiditySensor = "humidity"
  final val co2Sensor = "co2"
  def fromString(s: String): SupportedDevice = s.toLowerCase match {
    case SupportedDevice.binarySensorString => BinarySensor
    case SupportedDevice.switchString       => Switch
    case SupportedDevice.temperatureSensor  => TemperatureSensor
    case SupportedDevice.humiditySensor     => HumiditySensor
    case SupportedDevice.co2Sensor          => CO2Sensor
  }
  def getDeviceBinding(deviceType: SupportedDevice): SupportedDeviceBinding = deviceType match {
    case BinarySensor      => BinarySensorBinding(deviceType.toString, defaultPhysicalId)
    case Switch            => SwitchBinding(deviceType.toString, defaultPhysicalId)
    case TemperatureSensor => TemperatureSensorBinding(deviceType.toString, defaultPhysicalId)
    case HumiditySensor    => HumiditySensorBinding(deviceType.toString, defaultPhysicalId)
    case CO2Sensor         => CO2SensorBinding(deviceType.toString, defaultPhysicalId)
  }
}

case object BinarySensor extends SupportedDevice {
  override def toString: String = SupportedDevice.binarySensorString
}
case object Switch extends SupportedDevice {
  override def toString: String = SupportedDevice.switchString
}
case object TemperatureSensor extends SupportedDevice {
  override def toString: String = SupportedDevice.temperatureSensor
}
case object HumiditySensor extends SupportedDevice {
  override def toString: String = SupportedDevice.humiditySensor
}
case object CO2Sensor extends SupportedDevice {
  override def toString: String = SupportedDevice.co2Sensor
}

class UnsupportedDeviceException(msg: String) extends Exception(msg)
