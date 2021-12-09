package ch.epfl.core.compiler.groupAddressAssigner

import ch.epfl.core.model.bindings.GroupAddressAssignment
import ch.epfl.core.model.physical.{GroupAddress, GroupAddresses, PhysicalStructure}
import ch.epfl.core.model.prototypical.{AppLibraryBindings, AppPrototypicalStructure}

import scala.collection.mutable

trait GroupAddressAssignerTrait {
  def assignGroupAddressesToPhysical(physStructure: PhysicalStructure, appLibraryBindings: AppLibraryBindings): GroupAddressAssignment

}
object GroupAddressAssigner extends GroupAddressAssignerTrait {
  override def assignGroupAddressesToPhysical(physStructure: PhysicalStructure, appLibraryBindings: AppLibraryBindings): GroupAddressAssignment = {
    val usedIdsInBindings = appLibraryBindings.appBindings.flatMap(appBind => appBind.bindings.flatMap(deviceBind => deviceBind.binding.getBoundIds))

    GroupAddresses.resetNextGroupAddress()
    val res = mutable.HashMap.empty[Int, GroupAddress]
    for(id <- usedIdsInBindings.toSet){
      res.put(id, GroupAddresses.getNextGroupAddress)
    }
    val map = res.toMap
    GroupAddressAssignment(physStructure, appLibraryBindings, map)
  }
}