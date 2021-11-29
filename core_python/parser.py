import json
import os
from typing import Dict, List, Tuple


class Parser:
    """
    Parser.
    """

    def __init__(self, group_addresses_filename: str):
        self.__group_addresses_filename = group_addresses_filename

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
        apps_dirs = [
            f.name
            for f in os.scandir("generated")
            if f.is_dir() and f.name != "__pycache__"
        ]
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
