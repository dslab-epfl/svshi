import json
import os
from typing import Dict, List, Tuple


class Parser:
    """
    Parser.
    """

    def __init__(self, group_addresses_filename: str):
        self.__group_addresses_filename = group_addresses_filename

    def __get_apps_directories_names(self) -> List[str]:
        return [
            f.name
            for f in os.scandir("generated")
            if f.is_dir() and f.name != "__pycache__"
        ]

    def parse_group_addresses(self) -> List[Tuple[str, str]]:
        """
        Parses the group addresses file, returning a list of (address, type) pairs.
        """
        with open(self.__group_addresses_filename, "r") as file:
            addrs_dict = json.load(file)
            return [(ga["address"], ga["type"]) for ga in addrs_dict["addresses"]]

    def parse_devices_instances(self) -> List[Tuple[str, str]]:
        """
        Parses the devices instances of all the apps, returning a map app_name -> (name, type).
        """
        apps_dirs = self.__get_apps_directories_names()
        apps_instances = []
        for app in apps_dirs:
            with open(
                f"generated/{app}/app_prototypical_structure.json", "r"
            ) as instances_file:
                instances_dict = json.load(instances_file)
                for instance in instances_dict["devices"]:
                    name = instance["name"]
                    type = instance["deviceType"]
                    apps_instances.append((f"{app}_{name}", type))

        return apps_instances

    def __parse_devices_types(self) -> Dict[Tuple[str, str], str]:
        """
        Parses the devices types for all the apps, returning a map (app_name, device_name) -> type.
        """
        device_and_app_name_to_type_map = {}
        with open("generated/apps_bindings.json") as bindings_file:
            bindings_dict = json.load(bindings_file)
            for app in bindings_dict["appBindings"]:
                app_name = app["name"]
                for device in app["bindings"]:
                    device_name = device["name"]
                    type = device["binding"]["typeString"]
                    device_and_app_name_to_type_map[(app_name, device_name)] = type

        return device_and_app_name_to_type_map

    def parse_devices_classes(self) -> List[Tuple[str, str, str, str]]:
        """
        Parses the devices classes information, returning a pair (app_name, device_name, device_type, device_address).
        """
        devices_types = self.__parse_devices_types()

        apps_dirs = self.__get_apps_directories_names()
        devices = []
        for app in apps_dirs:
            with open(f"generated/{app}/addresses.json") as file:
                devices_dict = json.load(file)
                for device in devices_dict["addresses"]:
                    name = device["name"]
                    type = devices_types[(app, name)]
                    address = device["address"] if "address" in device else ""
                    if type == "switch":
                        # Use one of the two addresses (writeAddress or readAddress), as they are actually the same
                        address = device["writeAddress"]
                    devices.append((app, name, type, address))

        return devices
