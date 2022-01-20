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

    __REQUIRED_FIELDS = {
        ("permissionLevel", str),
        ("timer", int),
        ("files", list),
        ("devices", list),
    }

    def __init__(self, filename: str):
        with open(filename, "r") as file:
            json_dict: dict = json.load(file)
            fields = set(json_dict.keys())
            diff = set(map(lambda p: p[0], self.__REQUIRED_FIELDS)) - fields
            if diff:
                raise ParserException(f"The fields {diff} are missing in '{filename}'")

            for field, field_type in self.__REQUIRED_FIELDS:
                if not isinstance(json_dict[field], field_type):
                    raise ParserException(
                        f"The field '{field}' must be of type '{field_type.__name__}'"
                    )

            self.__devices = json_dict["devices"]

    def read_devices(
        self,
    ) -> List[Device]:
        """
        Reads the devices from the JSON file, returning a list.
        """

        name_regex = re.compile(r"^_*[a-zA-Z]+[a-zA-Z_]*_*$")

        def to_device(d: dict) -> Device:
            if "name" not in d:
                raise ParserException(f"The field 'name' is missing in 'devices'")

            if "deviceType" not in d:
                raise ParserException(f"The field 'deviceType' is missing in 'devices'")

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
            elif type == "co2":
                type = "CO2Sensor"
            else:
                raise ParserException(f"Unknown type '{type}'")

            return Device(name, type, import_module_name)

        return list(map(to_device, self.__devices))
