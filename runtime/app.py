import dataclasses
import os
import json
from typing import Callable, Dict, List, Tuple
from itertools import groupby
from importlib import import_module

from verification.verification_file import PhysicalState


@dataclasses.dataclass
class App:
    name: str
    code: Callable[[PhysicalState], None]
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


def __get_apps_names() -> List[str]:
    return [
        f.name
        for f in os.scandir("app_library")
        if f.is_dir() and f.name != "__pycache__"
    ]


def get_apps() -> List[App]:
    """
    Gets the list of apps.
    """
    apps_names = __get_apps_names()
    apps = []
    for app_name in apps_names:
        app_code = getattr(
            import_module("verification.verification_file"), f"{app_name}_iteration"
        )
        apps.append(App(app_name, app_code))
    return apps


def get_addresses_listeners() -> Dict[str, List[str]]:
    """
    Gets, per each address, a list of apps names listening to it.
    """
    apps = __get_apps_names()
    apps_addresses: List[Tuple[str, str]] = []
    for app in apps:
        with open(f"app_library/{app}/addresses.json", "r") as file:
            file_dict = json.load(file)
            for address in file_dict["addresses"]:
                apps_addresses.append((app, address))

    listeners: Dict[str, List[str]] = {}
    for key, group in groupby(
        sorted(apps_addresses, key=lambda p: p[0]), lambda x: x[1]
    ):
        apps = []
        for tup in group:
            app_name = tup[0]
            apps.append(app_name)
        listeners[key] = apps

    return listeners
