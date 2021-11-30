package ch.epfl.core.verifier

import ch.epfl.core.models.application.ApplicationLibrary
import ch.epfl.core.models.bindings.GroupAddressAssignment

object Verifier {
  def verify(newAppLibrary: ApplicationLibrary, existingAppsLibrary: ApplicationLibrary, groupAddressAssignment: GroupAddressAssignment): ApplicationLibrary = {
    val physicalStructure = groupAddressAssignment.physStruct
    bindings.Verifier.verify(newAppLibrary, existingAppsLibrary, groupAddressAssignment)
    existingAppsLibrary
  }
}
