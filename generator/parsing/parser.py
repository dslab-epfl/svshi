import json
from typing import List, Dict
from generator.parsing.device import DeviceType, DeviceInstance
from generator.parsing.channel import Channel, ChannelType


def read_device_types(filename: str) -> List[DeviceType]:
    def to_channel(c: dict) -> Channel:
        name = c["name"]
        datatype = c["datatype"]
        string_type = c["type"]
        type = ChannelType.IN
        if string_type == "out":
            type = ChannelType.IN
        else:
            type = ChannelType.IN_OUT
        return Channel(name, datatype, type)

    def to_device_type(dt: dict) -> DeviceType:
        type = dt["type"]
        channels = list(map(to_channel, dt["channels"]))
        return DeviceType(type, channels)

    with open(filename, "r") as file:
        json_dict = json.load(file)
        return list(map(to_device_type, json_dict["deviceTypes"]))


def read_device_instances(
    filename: str, device_types_map: Dict[str, DeviceType]
) -> List[DeviceInstance]:
    def to_device_instance(di: dict):
        type = device_types_map[di["type"]]
        name = di["name"]
        return DeviceInstance(name, type)

    with open(filename, "r") as file:
        json_dict = json.load(file)
        return list(map(to_device_instance, json_dict["deviceInstances"]))
