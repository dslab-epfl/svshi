package ch.epfl.core.verifier

import ch.epfl.core.model.application.ApplicationLibrary
import ch.epfl.core.model.bindings.GroupAddressAssignment
import ch.epfl.core.verifier.exceptions.VerifierMessage

trait VerifierTr {
  def verify(newAppLibrary: ApplicationLibrary, existingAppsLibrary: ApplicationLibrary, groupAddressAssignment: GroupAddressAssignment): List[VerifierMessage]
}
