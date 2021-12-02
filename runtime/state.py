import json
import dataclasses
from typing import Dict, List, Tuple, Union
from xknx.core.value_reader import ValueReader
from xknx.telegram.address import GroupAddress
from xknx.xknx import XKNX
from verification_file import PhysicalState
from runtime.app import App
from runtime.verifier.conditions import check_conditions


class State:

    __GROUP_ADDRESSES_FILE_PATH = "app_library/group_addresses.json"

    def __init__(self, addresses_listeners: Dict[str, List[App]]):
        self.__physical_state: PhysicalState
        self.__addresses_listeners = addresses_listeners

    async def initialize(self, xknx: XKNX):
        """
        Initializes the system state by reading it from the KNX bus.
        """
        # There are 6 __something__ values in the dict that we do not care about
        nb_fields = len(PhysicalState.__dict__) - 6
        n = nb_fields * [None]
        # Default value is None for each field/address
        state = PhysicalState(*n)
        with open(self.__GROUP_ADDRESSES_FILE_PATH) as addresses_file:
            addresses_dict = json.load(addresses_file)
            for address_and_type in addresses_dict["addresses"]:
                # Read from KNX the current value
                address = address_and_type[0]
                value_reader = ValueReader(
                    xknx, GroupAddress(address), timeout_in_seconds=5
                )
                telegram = await value_reader.read()
                if telegram and telegram.payload.value:
                    setattr(
                        state,
                        f"GA_{address.replace('/', '_')}",
                        telegram.payload.value.value,
                    )

        self.__physical_state = state

    def __compare(
        self, new_state: PhysicalState, old_state: PhysicalState
    ) -> List[Tuple[str, Union[str, bool, float]]]:
        # TODO Returns a list of (group_address, value) pairs
        return []

    def __notify_listeners(self, address: str):
        for app in self.__addresses_listeners[address]:
            if app.should_run:
                old_state = dataclasses.replace(self.__physical_state)
                app.notify(self.__physical_state)
                if not check_conditions(self.__physical_state):
                    # Conditions are not preserved, revert to previous state and prevent app from running again
                    self.__physical_state = old_state
                    app.stop()
                else:
                    diff = self.__compare(self.__physical_state, old_state)
                    if diff:
                        # Write to KNX
                        pass

    def update(self, address: str, value: Union[str, bool, float]):
        setattr(self.__physical_state, f"GA_{address.replace('/', '_')}", value)
        self.__notify_listeners(address)
