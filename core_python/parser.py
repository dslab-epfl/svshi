import json
import dataclasses
import os
from typing import Dict, List, Tuple


@dataclasses.dataclass
class App:
    name: str
    directory: str


@dataclasses.dataclass
class GroupAddress:
    address: str
    type: str


@dataclasses.dataclass
class DeviceInstance:
    name: str
    type: str


@dataclasses.dataclass
class DeviceClass:
    app: App
    name: str
    type: str
    address: str


class Parser:
    """
    JSON files parser.
    """

    __GENERATED_DIR_NAME = "generated"
    __APP_LIBRARY_DIR_NAME = "app_library"

    def __get_apps(self) -> List[App]:
        return [
            App(f.name, self.__GENERATED_DIR_NAME)
            for f in os.scandir(self.__GENERATED_DIR_NAME)
            if f.is_dir() and f.name != "__pycache__"
        ] + [
            App(f.name, self.__APP_LIBRARY_DIR_NAME)
            for f in os.scandir(self.__APP_LIBRARY_DIR_NAME)
            if f.is_dir() and f.name != "__pycache__"
        ]

    def parse_group_addresses(self) -> List[GroupAddress]:
        """
        Parses the group addresses file, returning a list of (address, type) pairs.
        """

        def read_addresses(directory: str) -> List[GroupAddress]:
            with open(f"{directory}/group_addresses.json", "r") as file:
                addrs_dict = json.load(file)
                return [GroupAddress(ga[0], ga[1]) for ga in addrs_dict["addresses"]]

        return read_addresses(self.__GENERATED_DIR_NAME) # + read_addresses(self.__APP_LIBRARY_DIR_NAME)

    def parse_devices_instances(self) -> List[DeviceInstance]:
        """
        Parses the devices instances of all the apps, returning a list of (name, type) pairs.
        """
        apps_dirs = self.__get_apps()
        apps_instances = []
        for app in apps_dirs:
            directory = app.directory
            app_name = app.name
            with open(
                f"{directory}/{app_name}/app_prototypical_structure.json", "r"
            ) as instances_file:
                instances_dict = json.load(instances_file)
                for instance in instances_dict["devices"]:
                    name = instance["name"]
                    type = instance["deviceType"]
                    apps_instances.append(DeviceInstance(f"{app_name}_{name}", type))

        return apps_instances

    def parse_devices_classes(self) -> List[DeviceClass]:
        """
        Parses the devices classes information, returning a list of (app_name, device_name, device_type, device_address) pairs.
        """

        def parse_devices_types() -> Dict[Tuple[str, str], str]:
            """
            Parses the devices types for all the apps, returning a map (app_name, device_name) -> type.
            """

            def generate_map(directory: str) -> Dict[Tuple[str, str], str]:
                device_and_app_name_to_type_map = {}
                with open(f"{directory}/apps_bindings.json") as bindings_file:
                    bindings_dict = json.load(bindings_file)
                    for app in bindings_dict["appBindings"]:
                        app_name = app["name"]
                        for device in app["bindings"]:
                            device_name = device["name"]
                            type = device["binding"]["typeString"]
                            device_and_app_name_to_type_map[
                                (app_name, device_name)
                            ] = type
                return device_and_app_name_to_type_map

            return {
                **generate_map(self.__GENERATED_DIR_NAME),
                # **generate_map(self.__APP_LIBRARY_DIR_NAME),
            }

        devices_types = parse_devices_types()

        apps_dirs = self.__get_apps()
        devices = []
        for app in apps_dirs:
            directory = app.directory
            app_name = app.name
            with open(f"{directory}/{app_name}/addresses.json") as file:
                devices_dict = json.load(file)
                for device in devices_dict["addresses"]:
                    name = device["name"]
                    type = devices_types[(app_name, name)]
                    address = device["address"] if "address" in device else ""
                    if type == "switch":
                        # Use one of the two addresses (writeAddress or readAddress), as they are actually the same
                        address = device["writeAddress"]
                    devices.append(DeviceClass(app, name, type, address))

        return devices
