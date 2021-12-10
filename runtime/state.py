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

    __GROUP_ADDRESSES_FILE_PATH = "app_library/group_addresses.json"

    __create_key = object()

    @classmethod
    async def create(cls, addresses_listeners: Dict[str, List[App]]):
        """
        Creates the state, initializing it through XKNX as well. It uses its own connection to KNX
        that is closed after initialization.
        """
        self = State(cls.__create_key)
        self.__physical_state = await self.__initialize()
        self.__xknx = XKNX(daemon_mode=True)
        self.__xknx.telegram_queue.register_telegram_received_cb(
            self.__telegram_received_cb
        )
        self.__addresses_listeners = addresses_listeners
        return self

    def __init__(self, create_key: object):
        """
        This constructor is only meant to be used internally.
        """
        assert (
            create_key == State.__create_key
        ), "State objects must be created using State.create"
        self.__physical_state: PhysicalState
        self.__xknx: XKNX
        self.__addresses_listeners: Dict[str, List[App]]

    async def __telegram_received_cb(self, telegram: Telegram):
        """
        Updates the state once a telegram is received.
        """
        v = telegram.payload.value
        if v:
            address = str(telegram.destination_address)
            setattr(self.__physical_state, f"GA_{address.replace('/', '_')}", v.value)
            await self.__notify_listeners(address)

    async def listen(self):
        await self.__xknx.start()

    async def stop(self):
        await self.__xknx.stop()

    async def __initialize(self):
        """
        Initializes the system state by reading it from the KNX bus.
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
                            f"GA_{address.replace('/', '_')}",
                            telegram.payload.value.value,
                        )

        return state

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
        # We first execute all the listeners
        old_state = dataclasses.replace(self.__physical_state)
        for app in self.__addresses_listeners[address]:
            # First all the non-privileged apps are run in alphabetical order, then the privileged ones in alphabetical order
            # In this way the privileged apps can override the behavior of the non-privileged ones
            if app.should_run:
                old_state_before_app = dataclasses.replace(self.__physical_state)
                app.notify(self.__physical_state)
                if not check_conditions(self.__physical_state):
                    # If conditions are not preserved, we revert to the previous state and prevent the app from running again
                    self.__physical_state = old_state_before_app
                    app.stop()

        # Then we write to KNX for just the final values given to the updated fields
        updated_fields = self.__compare(self.__physical_state, old_state)
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
