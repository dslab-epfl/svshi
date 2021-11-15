package ch.epfl.core.models.prototypical

case class AppPrototypicalStructure(
    deviceInstances: List[AppPrototypicalDeviceInstance]
)
case class AppPrototypicalDeviceInstance(
    name: String,
    deviceType: SupportedDevice
)
