package ch.epfl.core.models.bindings

import ch.epfl.core.models.physical.{GroupAddress, PhysicalStructure}
import ch.epfl.core.models.prototypical.AppLibraryBindings

case class GroupAddressAssignment(physStruct: PhysicalStructure, appLibraryBindings: AppLibraryBindings, physIdToGA: Map[Int, GroupAddress])
