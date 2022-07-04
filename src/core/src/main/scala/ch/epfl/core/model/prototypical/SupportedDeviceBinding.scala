package ch.epfl.core.model.prototypical

import ch.epfl.core.model.physical._
import ch.epfl.core.model.python.PythonType
import upickle.default.{ReadWriter, macroRW}

// WARNING!!!!!!
// argument names must be different for all types for uPickle to be able to discriminate them

/** Represents a binding between a prototypical device of an application and a physical device's communication object from
  * the physical KNX installation.
  * When adding a new Object extending `SupportedDevice`, a new corresponding binding must be added here
  */
sealed trait SupportedDeviceBinding {
  def getBoundIds: List[Int]
  def getIOTypes: Map[Int, IOType]
  def getKNXDpt: Map[Int, KNXDatatype]
  def getPythonTypes: Map[Int, PythonType] = getKNXDpt.toList.map { case (id, typ) => (id, typ.toPythonType) }.toMap
  def equivalent(other: SupportedDeviceBinding): Boolean
}
object SupportedDeviceBinding {
  implicit val rw: ReadWriter[SupportedDeviceBinding] =
    ReadWriter.merge(
      BinarySensorBinding.rw,
      SwitchBinding.rw,
      TemperatureSensorBinding.rw,
      HumiditySensorBinding.rw,
      CO2SensorBinding.rw,
      DimmerSensorBinding.rw,
      DimmerActuatorBinding.rw
    )
}

case class BinarySensorBinding(typeString: String, physDeviceId: Int) extends SupportedDeviceBinding {
  override def getBoundIds: List[Int] = List(physDeviceId)
  override def getIOTypes: Map[Int, IOType] = Map((physDeviceId, Out))
  override def getKNXDpt: Map[Int, KNXDatatype] = Map((physDeviceId, DPT1))
  override def equivalent(other: SupportedDeviceBinding): Boolean = other match {
    case BinarySensorBinding(_, _) => true
    case _                         => false
  }
}
object BinarySensorBinding {
  implicit val rw: ReadWriter[BinarySensorBinding] =
    macroRW[BinarySensorBinding]
}
case class SwitchBinding(typeString: String, physDeviceId: Int) extends SupportedDeviceBinding {
  override def getBoundIds: List[Int] = List(physDeviceId)
  override def getIOTypes: Map[Int, IOType] = Map((physDeviceId, In))
  override def getKNXDpt: Map[Int, KNXDatatype] = Map((physDeviceId, DPT1), (physDeviceId, DPT1))
  override def equivalent(other: SupportedDeviceBinding): Boolean = other match {
    case SwitchBinding(_, _) => true
    case _                   => false
  }
}
object SwitchBinding {
  implicit val rw: ReadWriter[SwitchBinding] =
    macroRW[SwitchBinding]
}
case class TemperatureSensorBinding(typeString: String, physDeviceId: Int) extends SupportedDeviceBinding {
  override def getBoundIds: List[Int] = List(physDeviceId)
  override def getIOTypes: Map[Int, IOType] = Map((physDeviceId, Out))
  override def getKNXDpt: Map[Int, KNXDatatype] = Map((physDeviceId, DPT9))
  override def equivalent(other: SupportedDeviceBinding): Boolean = other match {
    case TemperatureSensorBinding(_, _) => true
    case _                              => false
  }
}
object TemperatureSensorBinding {
  implicit val rw: ReadWriter[TemperatureSensorBinding] =
    macroRW[TemperatureSensorBinding]
}
case class HumiditySensorBinding(typeString: String, physDeviceId: Int) extends SupportedDeviceBinding {
  override def getBoundIds: List[Int] = List(physDeviceId)
  override def getIOTypes: Map[Int, IOType] = Map((physDeviceId, Out))
  override def getKNXDpt: Map[Int, KNXDatatype] = Map((physDeviceId, DPT9))
  override def equivalent(other: SupportedDeviceBinding): Boolean = other match {
    case HumiditySensorBinding(_, _) => true
    case _                           => false
  }
}
object HumiditySensorBinding {
  implicit val rw: ReadWriter[HumiditySensorBinding] =
    macroRW[HumiditySensorBinding]
}

case class CO2SensorBinding(typeString: String, physDeviceId: Int) extends SupportedDeviceBinding {
  override def getBoundIds: List[Int] = List(physDeviceId)
  override def getIOTypes: Map[Int, IOType] = Map((physDeviceId, Out))
  override def getKNXDpt: Map[Int, KNXDatatype] = Map((physDeviceId, DPT9))
  override def equivalent(other: SupportedDeviceBinding): Boolean = other match {
    case CO2SensorBinding(_, _) => true
    case _                      => false
  }
}
object CO2SensorBinding {
  implicit val rw: ReadWriter[CO2SensorBinding] =
    macroRW[CO2SensorBinding]
}

case class DimmerSensorBinding(typeString: String, physDeviceId: Int) extends SupportedDeviceBinding {
  override def getBoundIds: List[Int] = List(physDeviceId)
  override def getIOTypes: Map[Int, IOType] = Map((physDeviceId, Out))
  override def getKNXDpt: Map[Int, KNXDatatype] = Map((physDeviceId, DPT5))
  override def equivalent(other: SupportedDeviceBinding): Boolean = other match {
    case DimmerSensorBinding(_, _) => true
    case _                         => false
  }
}
object DimmerSensorBinding {
  implicit val rw: ReadWriter[DimmerSensorBinding] =
    macroRW[DimmerSensorBinding]
}

case class DimmerActuatorBinding(typeString: String, physDeviceId: Int) extends SupportedDeviceBinding {
  override def getBoundIds: List[Int] = List(physDeviceId)
  override def getIOTypes: Map[Int, IOType] = Map((physDeviceId, In))
  override def getKNXDpt: Map[Int, KNXDatatype] = Map((physDeviceId, DPT5))
  override def equivalent(other: SupportedDeviceBinding): Boolean = other match {
    case DimmerActuatorBinding(_, _) => true
    case _                           => false
  }
}
object DimmerActuatorBinding {
  implicit val rw: ReadWriter[DimmerActuatorBinding] =
    macroRW[DimmerActuatorBinding]
}

case class DeviceInstanceBinding(name: String, binding: SupportedDeviceBinding) {
  def equivalent(other: DeviceInstanceBinding): Boolean = other match {
    case DeviceInstanceBinding(otherName, otherBinding) => binding.equivalent(otherBinding) && name == otherName
    case _                                              => false
  }
}
object DeviceInstanceBinding {
  implicit val rw: ReadWriter[DeviceInstanceBinding] =
    macroRW[DeviceInstanceBinding]
}

case class AppPrototypeBindings(name: String, bindings: List[DeviceInstanceBinding]) {
  def equivalent(other: AppPrototypeBindings): Boolean = other match {
    case AppPrototypeBindings(otherName, otherBindings) => name == otherName && bindings.forall(b => otherBindings.exists(ob => b.equivalent(ob)))
    case _                                              => false
  }
}
object AppPrototypeBindings {
  implicit val rw: ReadWriter[AppPrototypeBindings] =
    macroRW[AppPrototypeBindings]
}

case class AppLibraryBindings(appBindings: List[AppPrototypeBindings]) {
  def equivalent(other: AppLibraryBindings): Boolean = other match {
    case AppLibraryBindings(otherAppBindings) => appBindings.forall(bs => otherAppBindings.exists(obs => bs.equivalent(obs)))
  }
}
object AppLibraryBindings {
  implicit val rw: ReadWriter[AppLibraryBindings] =
    macroRW[AppLibraryBindings]
}
