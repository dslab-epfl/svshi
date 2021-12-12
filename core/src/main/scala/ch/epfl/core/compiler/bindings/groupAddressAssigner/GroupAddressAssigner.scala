package ch.epfl.core.compiler.bindings.groupAddressAssigner

import ch.epfl.core.model.bindings.GroupAddressAssignment
import ch.epfl.core.model.physical.{GroupAddress, GroupAddresses, PhysicalStructure}
import ch.epfl.core.model.prototypical.AppLibraryBindings

import scala.collection.mutable

trait GroupAddressAssignerTrait {

  /** Assign a group address to each used (i.e., bound to a prototypical device) physical communication object
    * of a physical device
    * @param physStructure the physical structure used to compile applications
    * @param appLibraryBindings bindings used for this compilation
    * @return
    */
  def assignGroupAddressesToPhysical(physStructure: PhysicalStructure, appLibraryBindings: AppLibraryBindings): GroupAddressAssignment

}
object GroupAddressAssigner extends GroupAddressAssignerTrait {
  override def assignGroupAddressesToPhysical(physStructure: PhysicalStructure, appLibraryBindings: AppLibraryBindings): GroupAddressAssignment = {
    val usedIdsInBindings = appLibraryBindings.appBindings.flatMap(appBind => appBind.bindings.flatMap(deviceBind => deviceBind.binding.getBoundIds))

    GroupAddresses.resetNextGroupAddress()
    val res = mutable.HashMap.empty[Int, GroupAddress]
    for (id <- usedIdsInBindings.toSet) {
      res.put(id, GroupAddresses.getNextGroupAddress)
    }
    val map = res.toMap
    GroupAddressAssignment(physStructure, appLibraryBindings, map)
  }
}
