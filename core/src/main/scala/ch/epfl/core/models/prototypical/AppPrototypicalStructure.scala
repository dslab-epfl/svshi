package ch.epfl.core.models.prototypical

import ch.epfl.core.models.application.PermissionLevel

case class AppPrototypicalStructure(permissionLevel: PermissionLevel, deviceInstances: List[AppPrototypicalDeviceInstance])
case class AppPrototypicalDeviceInstance(name: String, deviceType: SupportedDevice)