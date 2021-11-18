package ch.epfl.core.models.prototypical

sealed trait SupportedDevice

object SupportedDevice {
  val defaultPhysicalId: Int = -1
  final val binarySensorString = "binary"
  final val switchString = "switch"
  final val temperatureSensor = "temperature"
  final val humiditySensor = "humidity"
  def fromString(s: String) : SupportedDevice = s.toLowerCase match {
    case SupportedDevice.binarySensorString => BinarySensor
    case SupportedDevice.switchString => Switch
    case SupportedDevice.temperatureSensor => TemperatureSensor
    case SupportedDevice.humiditySensor => HumiditySensor
  }
  def getDeviceBinding(deviceType: SupportedDevice): SupportedDeviceBinding = deviceType match {
    case BinarySensor => BinarySensorBinding(deviceType.toString, defaultPhysicalId)
    case Switch => SwitchBinding(deviceType.toString, defaultPhysicalId, defaultPhysicalId)
    case TemperatureSensor => TemperatureSensorBinding(deviceType.toString, defaultPhysicalId)
    case HumiditySensor => HumiditySensorBinding(deviceType.toString, defaultPhysicalId)
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

class UnsupportedDeviceException(msg: String) extends Exception(msg)
