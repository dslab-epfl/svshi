package ch.epfl.core.model.prototypical

import ch.epfl.core.model.application.PermissionLevel

/** Represents the prototypical structure (i.e., devices used by the application) declared by an application
  * @param permissionLevel
  * @param deviceInstances
  */
case class AppPrototypicalStructure(permissionLevel: PermissionLevel, deviceInstances: List[AppPrototypicalDeviceInstance])
case class AppPrototypicalDeviceInstance(name: String, deviceType: SupportedDevice)
