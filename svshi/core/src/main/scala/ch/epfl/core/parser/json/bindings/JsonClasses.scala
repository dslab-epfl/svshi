package ch.epfl.core.parser.json.bindings

import upickle.default.{ReadWriter, macroRW}

/** Classes used by Upickle to parse Application Bindings and Python Addresses
  */

case class AppPythonAddressesJson(permissionLevel: String, timer: Int, addresses: List[DeviceAddressJson])
object AppPythonAddressesJson {
  implicit val rw: ReadWriter[AppPythonAddressesJson] =
    macroRW[AppPythonAddressesJson]
}

sealed trait DeviceAddressJson
object DeviceAddressJson {
  implicit val rw: ReadWriter[DeviceAddressJson] =
    ReadWriter.merge(BinarySensorAddressJson.rw, SwitchAddressJson.rw, TemperatureSensorAddressJson.rw, HumiditySensorAddressJson.rw, CO2SensorAddressJson.rw)
}

case class BinarySensorAddressJson(name: String, address: String) extends DeviceAddressJson
object BinarySensorAddressJson {
  implicit val rw: ReadWriter[BinarySensorAddressJson] =
    macroRW[BinarySensorAddressJson]
}

case class SwitchAddressJson(name: String, writeAddress: String, readAddress: String) extends DeviceAddressJson
object SwitchAddressJson {
  implicit val rw: ReadWriter[SwitchAddressJson] =
    macroRW[SwitchAddressJson]
}

case class TemperatureSensorAddressJson(name: String, address: String) extends DeviceAddressJson
object TemperatureSensorAddressJson {
  implicit val rw: ReadWriter[TemperatureSensorAddressJson] =
    macroRW[TemperatureSensorAddressJson]
}

case class HumiditySensorAddressJson(name: String, address: String) extends DeviceAddressJson
object HumiditySensorAddressJson {
  implicit val rw: ReadWriter[HumiditySensorAddressJson] =
    macroRW[HumiditySensorAddressJson]
}

case class CO2SensorAddressJson(name: String, address: String) extends DeviceAddressJson
object CO2SensorAddressJson {
  implicit val rw: ReadWriter[CO2SensorAddressJson] =
    macroRW[CO2SensorAddressJson]
}
