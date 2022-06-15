import dataclasses
import os
import json
import subprocess
import sys
from typing import Callable, Dict, Iterator, List, Tuple
from itertools import groupby
from importlib import import_module

from .verification_file import (
    AppState,
    PhysicalState,
    InternalState,
    IsolatedFunctionsValues,
)


@dataclasses.dataclass
class JointApps:
    name: str
    code: Callable[
        [AppState, PhysicalState, InternalState, IsolatedFunctionsValues], None
    ]
    timer: int = 0
    is_privileged: bool = True
    should_run: bool = True

    def __eq__(self, other: object) -> bool:
        if isinstance(other, JointApps):
            return (
                self.name == other.name
                and self.directory == other.directory
                and self.should_run == other.should_run
            )
        else:
            return False

    def __hash__(self) -> int:
        return hash(repr(self))

    def __str__(self) -> str:
        return (
            f'App(name="{self.name}", should_run={self.should_run}, timer={self.timer})'
        )

    def notify(
        self,
        app_state: Dict[str, AppState],
        physical_state: PhysicalState,
        internal_state: InternalState,
        isolated_fn_values: IsolatedFunctionsValues,
    ):
        """
        Notifies the app, triggering an iteration.
        """
        self.code(
            **app_state,
            physical_state=physical_state,
            internal_state=internal_state,
            isolated_fn_values=isolated_fn_values,
        )

    def stop(self):
        """
        Prevents the app from running again.
        """
        self.should_run = False


def get_joint_apps(runtime_file_module: str) -> List[JointApps]:
    joint_apps = []
    joint_app_code = getattr(import_module(runtime_file_module), f"system_behaviour")
    joint_apps.append(JointApps("joint_app", joint_app_code, timer=0))
    return joint_apps
