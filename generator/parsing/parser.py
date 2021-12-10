import json
import re
from typing import List

from .device import Device


class ParserException(Exception):
    """
    A parser exception.
    """


class Parser:
    """
    Devices JSON parser.
    """

    def __init__(self, filename: str):
        with open(filename, "r") as file:
            json_dict = filename = json.load(file)
            self.__devices = json_dict["devices"]

    def read_devices(
        self,
    ) -> List[Device]:
        """
        Reads the devices from the JSON file, returning a list.
        """

        name_regex = re.compile(r"^_*[a-zA-Z]+[a-zA-Z_]*_*$")

        def to_device(d: dict) -> Device:
            name = d["name"]
            if not name_regex.match(name):
                raise ParserException(
                    f"Wrong device name '{name}': it has to contain only letters and underscores"
                )

            type = d["deviceType"]
            import_module_name = type
            if type == "binary":
                type = "BinarySensor"
            elif type == "temperature":
                type = "TemperatureSensor"
            elif type == "humidity":
                type = "HumiditySensor"
            elif type == "switch":
                type = "Switch"
            else:
                raise ParserException(f"Unknown type '{type}'")

            return Device(name, type, import_module_name)

        return list(map(to_device, self.__devices))
