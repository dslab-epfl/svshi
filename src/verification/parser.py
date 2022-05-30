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

    # Arbitrary: non-privileged = 0 and privileged = 10
    PRIVILEGED_PRIORITY_LEVEL = 10
    NOT_PRIVILEGED_PRIORITY_LEVEL = 0

    def __init__(self, generated_dir_name: str, app_library_dir_name: str):
        self.__generated_dir_name = generated_dir_name
        self.__app_library_dir_name = app_library_dir_name
        self.__apps = self.__get_apps()

        self.__app_protos: Dict[str, dict] = {}
        for app in self.__apps:
            directory = app.directory
            app_name = app.name
            with open(
                f"{directory}/{app_name}/app_prototypical_structure.json", "r"
            ) as proto_file:
                self.__app_protos[app_name] = json.load(proto_file)

    def get_app_names(self) -> List[str]:
        """
        Gets all the app names.
        """
        return sorted(
            list(
                map(
                    lambda a: a.name,
                    self.__apps,
                )
            )
        )

    def get_app_priorities(self) -> Dict[str, int]:
        """
        Returns the priority level of all apps in a Dict mapping app names to priority level.
        The higher the priority level, the more priviledged the app is.
        """

        return {
            a.name: Parser.PRIVILEGED_PRIORITY_LEVEL
            if self.__app_protos[a.name]["permissionLevel"] == "privileged"
            else Parser.NOT_PRIVILEGED_PRIORITY_LEVEL
            for a in self.__apps
        }
    
    def get_filenames(self) -> Dict[str, Set[str]]:
        """
        Parses the filenames associated to each app.
        """
        filenames = {}
        for app in self.__apps:
            app_name = app.name
            files_folder_path = f"{app.directory}/{app_name}/files"
            names = [f.name for f in os.scandir(files_folder_path) if f.is_file()] if os.path.isdir(files_folder_path) else []
            filenames[app_name] = set(names)
        return filenames

    def __get_apps(self) -> List[App]:
        def get_apps_from_directory(directory: str) -> List[App]:
            if not os.path.exists(directory):
                return []

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
        apps_instances = []
        for app in self.__apps:
            app_name = app.name
            proto = self.__app_protos[app_name]
            for instance in proto["devices"]:
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
        devices = []
        for app in self.__apps:
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
