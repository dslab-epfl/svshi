import dataclasses
import os
import json
from typing import Callable, Dict, List, Tuple
from itertools import groupby
from importlib import import_module
from xknx.devices import Device

from verification_file import PhysicalState


@dataclasses.dataclass
class App:
    name: str
    code: Callable[[PhysicalState], None]
    devices: List[Device]
    group_address_to_action: Dict[str, Callable]
    is_privileged: bool = False
    should_run: bool = True

    def notify(self, state: PhysicalState):
        """
        Notifies the app, triggering an iteration.
        """
        self.code(state)

    def stop(self):
        """
        Prevents the app from running again.
        """
        self.should_run = False


def get_addresses_listeners() -> Dict[str, List[App]]:
    """
    Gets, per each address, a list of apps listening to it.
    """

    def get_devices() -> List[Device]:
        return []

    def compute_action_per_group_address(devices: List[Device]) -> Dict[str, Callable]:
        return {}

    apps = [
        f.name
        for f in os.scandir("app_library")
        if f.is_dir() and f.name != "__pycache__"
    ]
    apps_addresses: List[Tuple[str, str]] = []
    for app in apps:
        with open(f"app_library/{app}/addresses.json", "r") as file:
            file_dict = json.load(file)
            for address in file_dict["addresses"]:
                apps_addresses.append((app, address))

    listeners: Dict[str, List[App]] = {}
    for key, group in groupby(
        sorted(apps_addresses, key=lambda p: p[0]), lambda x: x[1]
    ):
        apps = []
        for tup in group:
            app_name = tup[0]
            app_code = getattr(
                import_module("verification_file"), f"{app_name}_iteration"
            )
            # TODO add missing arguments
            devices = get_devices()
            group_address_to_action_dict = compute_action_per_group_address(devices)
            apps.append(App(app_name, app_code, devices, group_address_to_action_dict))
        listeners[key] = apps

    return listeners
