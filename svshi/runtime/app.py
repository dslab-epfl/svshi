import dataclasses
import os
import json
import subprocess
import sys
from typing import Callable, Dict, List, Optional, Tuple
from itertools import groupby
from importlib import import_module

from .verification_file import PhysicalState


@dataclasses.dataclass
class App:
    name: str
    directory: str
    code: Callable[[PhysicalState], None]
    is_privileged: bool = False
    should_run: bool = True
    timer: int = 0

    def __eq__(self, other: object) -> bool:
        if isinstance(other, App):
            return (
                self.name == other.name
                and self.directory == other.directory
                and self.is_privileged == other.is_privileged
                and self.should_run == other.should_run
            )
        else:
            return False

    def __hash__(self) -> int:
        return hash(repr(self))

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
                f"{self.directory}/{self.name}/requirements.txt",
            ]
        )


def __get_apps_names(app_library_dir: str) -> List[str]:
    return [
        f.name
        for f in os.scandir(app_library_dir)
        if f.is_dir() and f.name != "__pycache__"
    ]


def get_apps(app_library_dir: str, runtime_file_module: str) -> List[App]:
    """
    Gets the list of apps.
    """
    apps_names = __get_apps_names(app_library_dir)
    apps = []
    for app_name in apps_names:
        app_code = getattr(import_module(runtime_file_module), f"{app_name}_iteration")
        with open(f"{app_library_dir}/{app_name}/addresses.json", "r") as file:
            file_dict = json.load(file)
            is_privileged = file_dict["permissionLevel"] == "privileged"
            timer = file_dict["timer"]
            apps.append(
                App(app_name, app_library_dir, app_code, is_privileged, timer=timer)
            )

    return apps


def get_addresses_listeners(apps: List[App]) -> Dict[str, List[App]]:
    """
    Gets, per each address, a list of apps names listening to it.
    """
    apps_addresses: List[Tuple[App, str]] = []
    for app in apps:
        app_name = app.name
        app_directory = app.directory
        with open(f"{app_directory}/{app_name}/addresses.json", "r") as file:
            file_dict = json.load(file)
            for address_obj in file_dict["addresses"]:
                address = (
                    address_obj["address"]
                    if "address" in address_obj
                    else address_obj["writeAddress"]
                )
                apps_addresses.append((app, address))

    listeners: Dict[str, List[App]] = {}
    for address, group in groupby(
        sorted(apps_addresses, key=lambda p: p[1]), lambda x: x[1]
    ):
        ga_listeners = []
        # Sort the apps first by permission level (not privileged first), then by name
        for pair in sorted(group, key=lambda p: (p[0].is_privileged, p[0].name)):
            app = pair[0]
            ga_listeners.append(app)
        listeners[address] = ga_listeners

    return listeners
