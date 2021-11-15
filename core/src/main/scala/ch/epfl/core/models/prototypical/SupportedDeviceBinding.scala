package ch.epfl.core.models.prototypical

import ch.epfl.core.compiler.parsers.json.prototype.PrototypicalStructureJson
import upickle.default.{macroRW, ReadWriter}

trait SupportedDeviceBinding

case class BinarySensorBinding(name: String, physDeviceId: Int) extends SupportedDeviceBinding
object BinarySensorBinding {
  implicit val binarySensorBinding: ReadWriter[BinarySensorBinding] =
    macroRW[BinarySensorBinding]
}
case class SwitchBinding(name: String, writePhysDeviceId: Int, readPhysDeviceId: Int) extends SupportedDeviceBinding
object SwitchBinding {
  implicit val switchBinding: ReadWriter[SwitchBinding] =
    macroRW[SwitchBinding]
}
case class TemperatureSensorBinding(name: String, physDeviceId: Int) extends SupportedDeviceBinding
object TemperatureSensorBinding {
  implicit val temperatureSensorBinding: ReadWriter[TemperatureSensorBinding] =
    macroRW[TemperatureSensorBinding]
}
case class HumiditySensorBinding(name: String, physDeviceId: Int) extends SupportedDeviceBinding
object HumiditySensorBinding {
  implicit val humiditySensorBinding: ReadWriter[HumiditySensorBinding] =
    macroRW[HumiditySensorBinding]
}

case class DeviceInstanceBinding(name: String, binding: SupportedDeviceBinding)
object DeviceInstanceBinding {
  implicit val deviceInstanceBinding: ReadWriter[DeviceInstanceBinding] =
    macroRW[DeviceInstanceBinding]
}

case class AppPrototypeBindings(name: String, bindings: List[DeviceInstanceBinding])
object AppPrototypeBindings {
  implicit val appPrototypeBindings: ReadWriter[AppPrototypeBindings] =
    macroRW[AppPrototypeBindings]
}

case class AppLibraryBindings(appBindings: List[AppPrototypeBindings])
object AppLibraryBindings {
  implicit val appLibraryBindings: ReadWriter[AppLibraryBindings] =
    macroRW[AppLibraryBindings]
}