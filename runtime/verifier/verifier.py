from typing import List
from runtime.runner import AppRunner
from verification_file import PhysicalState
from runtime.verifier.tracker import Message
from runtime.verifier.conditions import check_conditions


class Verifier:
    """
    App verifier.
    """

    def __init__(self, app_runner: AppRunner, state: PhysicalState):
        self.__app_runner = app_runner
        self.__state = state

    def __kill_app(self, app_name: str):
        self.__app_runner.stop(app_name)

    def verify_write(self, message: Message):
        """
        Verifies that the write contained in the message does not violate the safety conditions.
        """
        app = message.app_name
        group_address = message.group_address
        data = message.data

        # Update state
        setattr(self.__state, f"GA_{group_address.replace('/', '_')}", data)

        # Check if the state is still valid, if not, kill the app
        if not check_conditions(self.__state):
            self.__kill_app(app)
