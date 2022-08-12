package ch.epfl.core.model.bindings

import ch.epfl.core.model.physical.{GroupAddress, KNXDatatype, PhysicalStructure}
import ch.epfl.core.model.prototypical.AppLibraryBindings
import ch.epfl.core.model.python.PythonType

/** Represents a assignment of group address to physical communication objects and to prototypical devices (in Application).
  * WARNING! The physIdToGA must not contain 2x the same GroupAddress (i.e., the map should be reversible and still be valid)
  * @param physStruct
  * @param appLibraryBindings
  * @param physIdToGA
  */
case class GroupAddressAssignment(physStruct: PhysicalStructure, appLibraryBindings: AppLibraryBindings, physIdToGA: Map[Int, GroupAddress]) {
  checkDuplicateGA(physIdToGA) match {
    case Some(value) => throw new IllegalArgumentException(f"The physIdToGA map should contain only one time every group address! $value appears more than once!")
    case None        => ()
  }

  def getPythonTypesMap: Map[GroupAddress, List[PythonType]] = {
    val physIdsToPythonTypes =
      appLibraryBindings.appBindings.flatMap(appProtoBinding => appProtoBinding.bindings.flatMap(devInstBinding => devInstBinding.binding.getPythonTypes.toList))
    physIdsToPythonTypes.groupBy(_._1).toList.map { case (physId, typeList) => (physIdToGA(physId), typeList.map(_._2)) }.toMap
  }

  def getDPTsMap: Map[GroupAddress, List[KNXDatatype]] = {
    val physIdsToPythonTypes =
      appLibraryBindings.appBindings.flatMap(appProtoBinding => appProtoBinding.bindings.flatMap(devInstBinding => devInstBinding.binding.getKNXDpt.toList))
    physIdsToPythonTypes.groupBy(_._1).toList.map { case (physId, typeList) => (physIdToGA(physId), typeList.map(_._2)) }.toMap
  }

  private def checkDuplicateGA(physIdToGA: Map[Int, GroupAddress]): Option[GroupAddress] = {
    val opt = physIdToGA.values.groupBy(ga => ga.toString).find { case (_, value) => value.toList.length > 1 }
    opt match {
      case Some(value) => Some(value._2.head)
      case None        => None
    }
  }
}
