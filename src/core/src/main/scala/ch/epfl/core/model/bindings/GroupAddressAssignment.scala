package ch.epfl.core.model.bindings

import ch.epfl.core.model.physical.{GroupAddress, KNXDatatype, PhysicalStructure}
import ch.epfl.core.model.prototypical.AppLibraryBindings
import ch.epfl.core.model.python.PythonType

/** Represents a assignment of group address to physical communication objects and to prototypical devices (in Application)
  * @param physStruct
  * @param appLibraryBindings
  * @param physIdToGA
  */
case class GroupAddressAssignment(physStruct: PhysicalStructure, appLibraryBindings: AppLibraryBindings, physIdToGA: Map[Int, GroupAddress]) {
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
}
