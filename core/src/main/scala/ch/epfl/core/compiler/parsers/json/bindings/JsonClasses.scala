package ch.epfl.core.compiler.parsers.json.bindings

import ch.epfl.core.models.prototypical.SupportedDeviceBinding
import upickle.default.{macroRW, ReadWriter}

//
//case class SupportedDeviceBindingJson(deviceType: String, )
//case class DeviceInstanceBindingJson(name: String, binding: SupportedDeviceBinding)
//object DeviceInstanceBindingJson {
//  implicit val rw: ReadWriter[DeviceInstanceBindingJson] =
//    macroRW[DeviceInstanceBindingJson]
//}
//
//case class AppPrototypeBindingsJson(name: String, bindings: List[DeviceInstanceBindingJson])
//object AppPrototypeBindingsJson {
//  implicit val rw: ReadWriter[AppPrototypeBindingsJson] =
//    macroRW[AppPrototypeBindingsJson]
//}
//
//case class AppLibraryBindingsJson(appBindings: List[AppPrototypeBindingsJson])
//object AppLibraryBindingsJson {
//  implicit val rw: ReadWriter[AppLibraryBindingsJson] =
//    macroRW[AppLibraryBindingsJson]
//}
