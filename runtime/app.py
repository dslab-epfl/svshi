import dataclasses
import os
import json
from typing import Callable, Dict, List, Tuple
from itertools import groupby

from verification_file import PhysicalState


@dataclasses.dataclass
class App:
    name: str
    code: Callable[[PhysicalState], None]
    is_privileged: bool = False

    def notify(self, state: PhysicalState):
        self.code(state)


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

    listeners = {}
    for key, group in groupby(
        sorted(apps_addresses, key=lambda p: p[0]), lambda x: x[1]
    ):
        listeners[key] = [tup[0] for tup in group]

    return listeners
