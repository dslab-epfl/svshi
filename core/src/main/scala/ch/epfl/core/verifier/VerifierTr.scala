package ch.epfl.core.verifier

import ch.epfl.core.model.application.ApplicationLibrary
import ch.epfl.core.model.bindings.GroupAddressAssignment
import ch.epfl.core.verifier.exceptions.VerifierMessage

/** Represents a verifier that runs on "in-compilation" application libraries
  */
trait VerifierTr {
  def verify(newAppLibrary: ApplicationLibrary, existingAppsLibrary: ApplicationLibrary, groupAddressAssignment: GroupAddressAssignment): List[VerifierMessage]
}
