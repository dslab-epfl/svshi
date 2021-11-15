package ch.epfl.core.models.prototypical

sealed trait SupportedDevice {

}
object SupportedDevice {
  final val binarySensorString = "binary"
  final val switchString = "switch"
  final val temperatureSensor = "temperature"
  final val humiditySensor = "humidity"
  def fromString(s: String) : SupportedDevice = s.toLowerCase match {
    case SupportedDevice.binarySensorString => BinarySensor
    case SupportedDevice.switchString => Switch
    case SupportedDevice.temperatureSensor => TemperatureSensor
    case SupportedDevice.humiditySensor => HumiditySensor
    case _ => throw new UnsupportedDeviceException(s"The device $s is not supported by Pistis!")
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
