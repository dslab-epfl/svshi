package ch.epfl.core.models.bindings

import ch.epfl.core.models.physical.{GroupAddress, PhysicalStructure}
import ch.epfl.core.models.prototypical.{AppLibraryBindings, DeviceInstanceBinding}
import ch.epfl.core.models.python.PythonType

case class GroupAddressAssignment(physStruct: PhysicalStructure, appLibraryBindings: AppLibraryBindings, physIdToGA: Map[Int, GroupAddress]) {
  def getPythonTypesMap: Map[GroupAddress, List[PythonType]] = {
    val physIdsToPythonTypes = appLibraryBindings.appBindings.flatMap(appProtoBinding => appProtoBinding.bindings.flatMap(devInstBinding => devInstBinding.binding.getPythonTypes.toList))
    physIdsToPythonTypes.groupBy(_._1).toList.map{case (physId, typeList) => (physIdToGA(physId), typeList.map(_._2))}.toMap
  }
}
