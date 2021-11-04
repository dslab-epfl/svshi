from typing import List
from generator.parsing.channel import Channel


class DeviceType:
    def __init__(self, type: str, channels: List[Channel]):
        self.type = type
        self.channels = channels


class DeviceInstance:
    def __init__(self, name: str, device_type: DeviceType):
        self.name = name
        self.device_type = device_type
