package ch.epfl.core.model.prototypical

import ch.epfl.core.model.physical.{DPT1, DPT9, IOType, In, InOut, KNXDatatype, Out}
import ch.epfl.core.model.python.{PythonBool, PythonFloat, PythonType}
import upickle.default.{ReadWriter, macroRW}


// WARNING!!!!!!
// argument names must be different for all types for uPickle to be able to discriminate them

sealed trait SupportedDeviceBinding {
  def getBoundIds: List[Int]
  def getIOTypes: Map[Int, IOType]
  def getKNXDpt: Map[Int, KNXDatatype]
  def getPythonTypes: Map[Int, PythonType] = getKNXDpt.toList.map{case (id, typ) => (id, typ.toPythonType)}.toMap
}
object SupportedDeviceBinding {
  implicit val rw: ReadWriter[SupportedDeviceBinding] = ReadWriter.merge(BinarySensorBinding.rw, SwitchBinding.rw, TemperatureSensorBinding.rw, HumiditySensorBinding.rw)
}

case class BinarySensorBinding(typeString: String, physDeviceId: Int) extends SupportedDeviceBinding {
  override def getBoundIds: List[Int] = List(physDeviceId)
  override def getIOTypes: Map[Int, IOType] = Map((physDeviceId, Out))
  override def getKNXDpt: Map[Int, KNXDatatype] = Map((physDeviceId, DPT1))
}
object BinarySensorBinding {
  implicit val rw: ReadWriter[BinarySensorBinding] =
    macroRW[BinarySensorBinding]
}
case class SwitchBinding(typeString: String, physDeviceId: Int) extends SupportedDeviceBinding {
  override def getBoundIds: List[Int] = List(physDeviceId)
  override def getIOTypes: Map[Int, IOType] = Map((physDeviceId, In))
  override def getKNXDpt: Map[Int, KNXDatatype] = Map((physDeviceId, DPT1), (physDeviceId, DPT1))
}
object SwitchBinding {
  implicit val rw: ReadWriter[SwitchBinding] =
    macroRW[SwitchBinding]
}
case class TemperatureSensorBinding(typeString: String, physDeviceId: Int) extends SupportedDeviceBinding {
  override def getBoundIds: List[Int] = List(physDeviceId)
  override def getIOTypes: Map[Int, IOType] = Map((physDeviceId, Out))
  override def getKNXDpt: Map[Int, KNXDatatype] = Map((physDeviceId, DPT9))
}
object TemperatureSensorBinding {
  implicit val rw: ReadWriter[TemperatureSensorBinding] =
    macroRW[TemperatureSensorBinding]
}
case class HumiditySensorBinding(typeString: String, physDeviceId: Int) extends SupportedDeviceBinding {
  override def getBoundIds: List[Int] = List(physDeviceId)
  override def getIOTypes: Map[Int, IOType] = Map((physDeviceId, Out))
  override def getKNXDpt: Map[Int, KNXDatatype] = Map((physDeviceId, DPT9))
}
object HumiditySensorBinding {
  implicit val rw: ReadWriter[HumiditySensorBinding] =
    macroRW[HumiditySensorBinding]
}

case class DeviceInstanceBinding(name: String, binding: SupportedDeviceBinding)
object DeviceInstanceBinding {
  implicit val rw: ReadWriter[DeviceInstanceBinding] =
    macroRW[DeviceInstanceBinding]
}

case class AppPrototypeBindings(name: String, bindings: List[DeviceInstanceBinding])
object AppPrototypeBindings {
  implicit val rw: ReadWriter[AppPrototypeBindings] =
    macroRW[AppPrototypeBindings]
}

case class AppLibraryBindings(appBindings: List[AppPrototypeBindings])
object AppLibraryBindings {
  implicit val rw: ReadWriter[AppLibraryBindings] =
    macroRW[AppLibraryBindings]
}