import json
from dataclasses import dataclass
import os
from typing import Dict, List, Set, Tuple


@dataclass
class App:
    name: str
    directory: str


@dataclass
class GroupAddress:
    address: str
    type: str


@dataclass
class DeviceInstance:
    name: str
    type: str


@dataclass
class DeviceClass:
    app: App
    name: str
    type: str
    address: str


class Parser:
    """
    JSON files parser.
    """

    def __init__(self, generated_dir_name: str, app_library_dir_name: str):
        self.__generated_dir_name = generated_dir_name
        self.__app_library_dir_name = app_library_dir_name

    def get_app_names(self) -> List[str]:
        """
        Gets all the app names.
        """
        return list(
            map(
                lambda a: a.name,
                self.__get_apps(),
            )
        )

    def __get_apps(self) -> List[App]:
        def get_apps_from_directory(directory: str) -> List[App]:
            return [
                App(f.name, directory)
                for f in os.scandir(directory)
                if f.is_dir() and f.name != "__pycache__"
            ]

        return get_apps_from_directory(
            self.__generated_dir_name
        ) + get_apps_from_directory(self.__app_library_dir_name)

    def parse_group_addresses(self) -> List[GroupAddress]:
        """
        Parses the group addresses file, returning a list of (address, type) pairs.
        """
        with open(f"{self.__generated_dir_name}/group_addresses.json", "r") as file:
            addrs_dict = json.load(file)
            return [GroupAddress(ga[0], ga[1]) for ga in addrs_dict["addresses"]]

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

    def parse_filenames(self) -> Dict[str, Set[str]]:
        """
        Parses the filenames associated to each app.
        """
        apps_dirs = self.__get_apps()
        filenames = {}
        for app in apps_dirs:
            directory = app.directory
            app_name = app.name
            with open(
                f"{directory}/{app_name}/app_prototypical_structure.json", "r"
            ) as instances_file:
                instances_dict = json.load(instances_file)
                names = set(instances_dict["files"])
                filenames[app_name] = names

        return filenames

    def parse_devices_classes(self) -> List[DeviceClass]:
        """
        Parses the devices classes information, returning a list of (app_name, device_name, device_type, device_address) pairs.
        """

        def parse_devices_types() -> Dict[Tuple[str, str], str]:
            """
            Parses the devices types for all the apps, returning a map (app_name, device_name) -> type.
            """
            device_and_app_name_to_type_map = {}
            with open(
                f"{self.__generated_dir_name}/apps_bindings.json"
            ) as bindings_file:
                bindings_dict = json.load(bindings_file)
                for app in bindings_dict["appBindings"]:
                    app_name = app["name"]
                    for device in app["bindings"]:
                        device_name = device["name"]
                        type = device["binding"]["typeString"]
                        device_and_app_name_to_type_map[(app_name, device_name)] = type
            return device_and_app_name_to_type_map

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
