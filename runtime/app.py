import dataclasses
import os
import json
from typing import Callable, Dict, List, Tuple
from itertools import groupby
from importlib import import_module

from verification_file import PhysicalState


@dataclasses.dataclass
class App:
    name: str
    code: Callable[[PhysicalState], None]
    is_privileged: bool = False
    should_run: bool = True

    def notify(self, state: PhysicalState):
        self.code(state)

    def stop(self):
        self.should_run = False


def get_addresses_listeners() -> Dict[str, List[App]]:
    """
    Gets, per each address, a list of apps listening to it.
    """
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
            apps.append(App(app_name, app_code))
        listeners[key] = apps

    return listeners
