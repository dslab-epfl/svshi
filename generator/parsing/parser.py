import json
from typing import List

from generator.parsing.device import Device


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

        def to_device(d: dict) -> Device:
            name = d["name"]
            type = d["type"]
            import_module_name = type
            if type == "binary":
                type = "BinarySensor"
            elif type == "sensor":
                type = "Sensor"
            elif type == "switch":
                type = "Switch"
            else:
                raise ParserException(f"Unknown type '{type}'")

            return Device(name, type, import_module_name)

        return list(map(to_device, self.__devices))
