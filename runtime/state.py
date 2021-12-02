import json
import dataclasses
from inspect import signature
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
        new_state_fields = {k: v for k, v in new_state.__dict__ if k.startswith("GA_")}
        old_state_fields = {k: v for k, v in old_state.__dict__ if k.startswith("GA_")}
        updated_fields = []
        for field, value in new_state_fields.items():
            if value != old_state_fields[field]:
                address = field.replace("GA_", "").replace("_", "/")
                updated_fields.append((address, value))

        return updated_fields

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
                    updated_fields = self.__compare(self.__physical_state, old_state)
                    if updated_fields:
                        # Write to KNX
                        for address, value in updated_fields:
                            # The action to call for the given address
                            action = app.group_address_to_action[address]
                            if signature(action).parameters:
                                # The action takes the value as parameter
                                action(value)
                            else:
                                # The action does not take any parameter
                                action()

    def update(self, address: str, value: Union[str, bool, float]):
        setattr(self.__physical_state, f"GA_{address.replace('/', '_')}", value)
        self.__notify_listeners(address)
