package ch.epfl.core.models.prototypical

import ch.epfl.core.models.application.PermissionLevel

case class AppPrototypicalStructure(deviceInstances: List[AppPrototypicalDeviceInstance], permissionLevel: PermissionLevel)
case class AppPrototypicalDeviceInstance(name: String, deviceType: SupportedDevice)