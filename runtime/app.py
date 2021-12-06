import dataclasses
import os
import json
import subprocess
import sys
from typing import Callable, Dict, List, Tuple
from itertools import groupby
from importlib import import_module

from .verification_file import PhysicalState


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

    def install_requirements(self):
        """
        Installs the app's requirements.
        """
        subprocess.check_call(
            [
                sys.executable,
                "-m",
                "pip",
                "install",
                "-r",
                f"app_library/{self.name}/requirements.txt",
            ]
        )


def __get_apps_names(app_library_dir: str) -> List[str]:
    return [
        f.name
        for f in os.scandir(app_library_dir)
        if f.is_dir() and f.name != "__pycache__"
    ]


def get_apps(app_library_dir: str, verification_file_module: str) -> List[App]:
    """
    Gets the list of apps.
    """
    apps_names = __get_apps_names(app_library_dir)
    apps = []
    for app_name in apps_names:
        app_code = getattr(
            import_module(verification_file_module), f"{app_name}_iteration"
        )
        apps.append(App(app_name, app_code))
    return apps


def get_addresses_listeners(app_library_dir: str) -> Dict[str, List[str]]:
    """
    Gets, per each address, a list of apps names listening to it.
    """
    apps = __get_apps_names(app_library_dir)
    apps_addresses: List[Tuple[str, str]] = []
    for app in apps:
        with open(f"{app_library_dir}/{app}/addresses.json", "r") as file:
            file_dict = json.load(file)
            for address_obj in file_dict["addresses"]:
                address = (
                    address_obj["address"]
                    if "address" in address_obj
                    else address_obj["writeAddress"]
                )
                apps_addresses.append((app, address))

    listeners: Dict[str, List[str]] = {}
    for key, group in groupby(
        sorted(apps_addresses, key=lambda p: p[1]), lambda x: x[1]
    ):
        apps = []
        for tup in group:
            app_name = tup[0]
            apps.append(app_name)
        listeners[key] = apps

    return listeners
