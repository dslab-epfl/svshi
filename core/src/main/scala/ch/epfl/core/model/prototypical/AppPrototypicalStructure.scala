package ch.epfl.core.model.prototypical

import ch.epfl.core.model.application.PermissionLevel

case class AppPrototypicalStructure(permissionLevel: PermissionLevel, deviceInstances: List[AppPrototypicalDeviceInstance])
case class AppPrototypicalDeviceInstance(name: String, deviceType: SupportedDevice)