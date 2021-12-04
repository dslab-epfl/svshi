import json
import dataclasses
from typing import Dict, List, Tuple, Union
from xknx.core.value_reader import ValueReader
from xknx.telegram.address import GroupAddress
from xknx.xknx import XKNX
from xknx.devices import RawValue
from verification.verification_file import PhysicalState
from runtime.app import App
from runtime.verifier.conditions import check_conditions


class State:

    __GROUP_ADDRESSES_FILE_PATH = "app_library/group_addresses.json"

    def __init__(
        self, xknx: XKNX, addresses_listeners: Dict[str, List[str]], apps: List[App]
    ):
        self.__physical_state: PhysicalState
        self.__xknx = xknx
        self.__addresses_listeners = addresses_listeners
        self.__app_name_to_app = {app.name: app for app in apps}

    async def initialize(self):
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
                    self.__xknx, GroupAddress(address), timeout_in_seconds=5
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
    ) -> List[Tuple[str, Union[bool, float]]]:
        def read_fields(state: PhysicalState) -> Dict[str, str]:
            return {k: v for k, v in state.__dict__ if k.startswith("GA_")}

        new_state_fields = read_fields(new_state)
        old_state_fields = read_fields(old_state)
        updated_fields = []
        for field, value in new_state_fields.items():
            if value != old_state_fields[field]:
                address = field.replace("GA_", "").replace("_", "/")
                updated_fields.append((address, value))

        return updated_fields

    async def __notify_listeners(self, address: str):
        for app_name in self.__addresses_listeners[address]:
            app = self.__app_name_to_app[app_name]
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
                            if isinstance(value, bool):
                                await RawValue(
                                    self.__xknx, "", 0, group_address=address
                                ).set(value)
                            else:
                                await RawValue(
                                    self.__xknx, "", 1, group_address=address
                                ).set(int(value))
                            
                            # Notify the listeners of the change
                            await self.__notify_listeners(address)

    async def update(self, address: str, value: Union[bool, float]):
        setattr(self.__physical_state, f"GA_{address.replace('/', '_')}", value)
        await self.__notify_listeners(address)
