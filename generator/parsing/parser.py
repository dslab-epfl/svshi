import json
from typing import List, Dict
from generator.parsing.device import DeviceType, DeviceInstance
from generator.parsing.channel import Channel, ChannelType


class Parser:
    """
    Devices JSON parser.
    """

    def __init__(self, filename: str):
        with open(filename, "r") as file:
            json_dict = filename = json.load(file)
            self.__device_types_dict = json_dict["deviceTypes"]
            self.__device_instances_dict = json_dict["deviceInstances"]

    def read_device_types(
        self,
    ) -> List[DeviceType]:
        """
        Reads the device types from the JSON file, returning a list.
        """

        def to_channel(c: dict) -> Channel:
            name = c["name"]
            datatype = c["datatype"]
            string_type = c["type"]
            type = ChannelType.IN
            if string_type == "out":
                type = ChannelType.OUT
            elif string_type == "in/out":
                type = ChannelType.IN_OUT
            return Channel(name, datatype, type)

        def to_device_type(dt: dict) -> DeviceType:
            type = dt["type"]
            channels = list(map(to_channel, dt["channels"]))
            return DeviceType(type, channels)

        return list(map(to_device_type, self.__device_types_dict))

    def read_device_instances(
        self, device_types_map: Dict[str, DeviceType]
    ) -> List[DeviceInstance]:
        """
        Reads the device instances given a mapping from type to DeviceType, returning a list.
        """

        def to_device_instance(di: dict):
            type = device_types_map[di["type"]]
            name = di["name"]
            return DeviceInstance(name, type)

        return list(map(to_device_instance, self.__device_instances_dict))
