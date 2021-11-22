from typing import Callable
from verifier.tracker import Message
import subprocess


class Verifier:
    """
    App verifier.
    """

    def __init__(self, apps_pids: dict, state: dict, preconditions_check: Callable[[dict], bool]):
        self.__apps_pids = apps_pids
        self.__state = state
        self.__preconditions_check = preconditions_check

    def __kill_app(self, app_name: str):
        app_pid = self.__apps_pids[app_name]
        subprocess.run(f"kill {app_pid}", shell=True)

    def verify_write(self, message: Message):
        """
        Verifies that the write contained in the message does not violate the safety conditions.
        """
        app = message.app_name
        group_address = message.group_address
        data = message.data

        # Update state
        self.__state[group_address] = data

        # Check if the state is still valid, if not, kill the app
        if not self.__preconditions_check(self.__state):
            self.__kill_app(app)
