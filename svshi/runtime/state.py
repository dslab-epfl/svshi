import json
import dataclasses
from typing import Dict, List, Tuple, Union
from xknx.core.value_reader import ValueReader
from xknx.telegram.telegram import Telegram
from xknx.telegram.address import GroupAddress
from xknx.xknx import XKNX
from xknx.devices import RawValue
from .verification_file import PhysicalState
from .app import App
from .conditions import check_conditions


class State:

    __GROUP_ADDRESSES_FILE_PATH = "../app_library/group_addresses.json"

    def __init__(self, addresses_listeners: Dict[str, List[App]]):
        self.__physical_state: PhysicalState
        self.__xknx = XKNX(daemon_mode=True)
        self.__xknx.telegram_queue.register_telegram_received_cb(
            self.__telegram_received_cb
        )
        self.__addresses_listeners = addresses_listeners

    async def __telegram_received_cb(self, telegram: Telegram):
        """
        Updates the state once a telegram is received.
        """
        v = telegram.payload.value
        if v:
            address = str(telegram.destination_address)
            setattr(
                self.__physical_state, self.__group_addr_to_field_name(address), v.value
            )
            await self.__notify_listeners(address)

    async def listen(self):
        """
        Connects to KNX and listens infinitely for telegrams.
        """
        await self.__xknx.start()

    async def stop(self):
        """
        Stops listening for telegrams and disconnects.
        """
        await self.__xknx.stop()

    async def initialize(self):
        """
        Initializes the system state by reading it from the KNX bus through an ephimeral connection.
        """
        # There are 6 __something__ values in the dict that we do not care about
        nb_fields = len(PhysicalState.__dict__) - 6
        n = nb_fields * [None]
        # Default value is None for each field/address
        state = PhysicalState(*n)
        async with XKNX() as xknx:
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
                            self.__group_addr_to_field_name(address),
                            telegram.payload.value.value,
                        )

        self.__physical_state = state

    def __group_addr_to_field_name(self, group_addr: str) -> str:
        return "GA_" + group_addr.replace("/", "_")

    def __field_name_to_group_addr(self, field: str) -> str:
        return field.replace("GA_", "").replace("_", "/")

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
                address = self.__field_name_to_group_addr(field)
                updated_fields.append((address, value))

        return updated_fields

    def __merge_states(
        self, old_state: PhysicalState, new_states: Dict[App, PhysicalState]
    ) -> PhysicalState:
        # Merge all the new states with the old one.
        # First all the non-privileged apps are run in alphabetical order, then the privileged ones in alphabetical order
        # In this way the privileged apps can override the behavior of the non-privileged ones
        sorted_new_states_by_priority = {
            k: v
            for k, v in sorted(
                new_states.items(),
                key=lambda item: (item[0].is_privileged, item[0].name),
            )
        }

        res = dataclasses.replace(old_state)
        for state in sorted_new_states_by_priority.values():
            updated_fields = self.__compare(old_state, state)
            for addr, value in updated_fields:
                setattr(res, self.__group_addr_to_field_name(addr), value)
        return res

    async def __notify_listeners(self, address: str):
        # We first execute all the listeners
        old_state = dataclasses.replace(self.__physical_state)
        new_states = {}
        for app in self.__addresses_listeners[address]:
            if app.should_run:
                per_app_state = dataclasses.replace(old_state)
                app.notify(per_app_state)
                if not check_conditions(per_app_state):
                    # If conditions are not preserved, we prevent the app from running again
                    app.stop()
                else:
                    # If conditions are preserved, we keep the state to later propagate
                    new_states[app] = per_app_state

        merged_state = self.__merge_states(old_state, new_states)

        # Update the physical_state with the merged one
        self.__physical_state = merged_state

        # Then we write to KNX for just the final values given to the updated fields
        updated_fields = self.__compare(old_state, merged_state)
        if updated_fields:
            for address, value in updated_fields:
                if isinstance(value, bool):
                    await RawValue(self.__xknx, "", 0, group_address=address).set(value)
                else:
                    await RawValue(self.__xknx, "", 1, group_address=address).set(
                        int(value)
                    )

                # Notify the listeners of the change
                await self.__notify_listeners(address)
