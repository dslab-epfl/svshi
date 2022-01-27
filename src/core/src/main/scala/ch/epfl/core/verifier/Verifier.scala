package ch.epfl.core.verifier

import ch.epfl.core.model.application.ApplicationLibrary
import ch.epfl.core.model.bindings.GroupAddressAssignment
import ch.epfl.core.verifier.exceptions.VerifierMessage

import scala.collection.parallel.immutable.ParVector
import scala.language.postfixOps

/** Main verifier that calls sub-verifiers
  */
object Verifier extends VerifierTr {

  /** Calls all verifiers on the libraries and outputs the messages
    * @param newAppLibrary
    * @param existingAppsLibrary
    * @param groupAddressAssignment
    * @return List of messages produces by the various verifiers
    */
  def verify(newAppLibrary: ApplicationLibrary, existingAppsLibrary: ApplicationLibrary, groupAddressAssignment: GroupAddressAssignment): List[VerifierMessage] = {
    val physicalStructure = groupAddressAssignment.physStruct
    val bindingsMessagesLambda = () => bindings.Verifier.verify(newAppLibrary, existingAppsLibrary, groupAddressAssignment)
    val pythonVerifierMessagesLambda = () => static.python.Verifier.verify(newAppLibrary, existingAppsLibrary, groupAddressAssignment)
    ParVector(bindingsMessagesLambda, pythonVerifierMessagesLambda).flatMap(_()).toList
  }

}
