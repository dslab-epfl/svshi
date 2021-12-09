package ch.epfl.core.verifier

import ch.epfl.core.model.application.ApplicationLibrary
import ch.epfl.core.model.bindings.GroupAddressAssignment
import ch.epfl.core.verifier.exceptions.VerifierMessage

object Verifier extends VerifierTr{
  def verify(newAppLibrary: ApplicationLibrary, existingAppsLibrary: ApplicationLibrary, groupAddressAssignment: GroupAddressAssignment): List[VerifierMessage] = {
    val physicalStructure = groupAddressAssignment.physStruct
    val bindingsMessages = bindings.Verifier.verify(newAppLibrary, existingAppsLibrary, groupAddressAssignment)
    val pythonVerifierMessages = static.python.Verifier.verify(newAppLibrary, existingAppsLibrary, groupAddressAssignment)
    bindingsMessages ++ pythonVerifierMessages
  }
}
