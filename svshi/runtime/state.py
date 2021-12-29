import dataclasses
import asyncio
from asyncio.tasks import Task
from typing import Callable, Dict, Final, List, Optional, Tuple, Union, cast
from itertools import groupby
from collections import defaultdict
from xknx.core.value_reader import ValueReader
from xknx.dpt.dpt import DPTArray, DPTBase, DPTBinary
from xknx.telegram.apci import GroupValueWrite
from xknx.telegram.telegram import Telegram
from xknx.telegram.address import GroupAddress
from xknx.xknx import XKNX
from .verification_file import PhysicalState
from .app import App


class State:

    __TRUE: Final = 1
    __FALSE: Final = 0
    __GROUP_ADDRESS_PREFIX: Final = "GA_"
    __SLASH: Final = "/"
    __UNDERSCORE: Final = "_"
    __ADDRESS_INITIALIZATION_TIMEOUT: Final = 20

    def __init__(
        self,
        addresses_listeners: Dict[str, List[App]],
        xknx_for_initialization: XKNX,
        xknx_for_listening: XKNX,
        check_conditions_function: Callable[[PhysicalState], bool],
        group_address_to_dpt: Dict[str, Union[DPTBase, DPTBinary]],
    ):
        self._physical_state: PhysicalState
        # Used to access and modify the physical state
        self.__physical_state_execution_lock = asyncio.Lock()

        self.__xknx_for_initialization = xknx_for_initialization
        self.__xknx_for_listening = xknx_for_listening
        self.__xknx_for_listening.telegram_queue.register_telegram_received_cb(
            self.__telegram_received_cb
        )

        self.__addresses_listeners = addresses_listeners
        self.__addresses = list(addresses_listeners.keys())

        self.__check_conditions_function = check_conditions_function
        self.__group_address_to_dpt = group_address_to_dpt

        periodic_apps = list(
            filter(
                lambda app: app.timer > 0,
                (app for apps in self.__addresses_listeners.values() for app in apps),
            )
        )
        # Group the apps by timer
        self.__periodic_apps: Dict[int, List[App]] = {}
        for timer, group in groupby(
            sorted(periodic_apps, key=lambda app: app.timer),
            lambda app: app.timer,
        ):
            self.__periodic_apps[timer] = sorted(
                group, key=lambda a: (a.is_privileged, a.name)
            )
        self.__periodic_apps_task: Optional[Task] = None

    async def __telegram_received_cb(self, telegram: Telegram):
        """
        Updates the state once a telegram is received.
        """
        payload = telegram.payload
        address = str(telegram.destination_address)
        if address in self.__addresses:
            # The telegram was for one of the addresses we use
            if isinstance(payload, GroupValueWrite):
                # We react only to GroupValueWrite; since for reading we use ValueReader
                # to send the request and receive the response at once,
                # we do not need to listen to GroupValueResponse
                v = payload.value
                if v:
                    async with self.__physical_state_execution_lock:
                        value = await self.__from_knx(address, v.value)
                        setattr(
                            self._physical_state,
                            self.__group_addr_to_field_name(address),
                            value,
                        )
                        await self.__notify_listeners(address)

    async def listen(self):
        """
        Connects to KNX and listens infinitely for telegrams.
        """
        await self.__xknx_for_listening.start()

    async def stop(self):
        """
        Stops listening for telegrams and disconnects.
        """
        if self.__periodic_apps_task:
            self.__periodic_apps_task.cancel()
        await self.__xknx_for_listening.stop()

    async def __send_value_to_knx(
        self, address: str, value: Union[bool, float, int, None]
    ):
        """
        Converts the given value to a raw value that can be understood by KNX and sends it to the given address.
        The address is also used to determine which DPT needs to be used for the conversion.
        """
        dpt = self.__group_address_to_dpt.get(address, None)
        if dpt != None and value != None:
            write_content: Union[DPTBinary, DPTArray, None] = None
            if isinstance(dpt, DPTBinary):
                binary_value = self.__TRUE if value else self.__FALSE
                write_content = DPTBinary(value=binary_value)
            else:
                write_content = DPTArray(dpt.to_knx(value))

            telegram = Telegram(
                destination_address=GroupAddress(address),
                payload=GroupValueWrite(write_content),
            )
            await self.__xknx_for_listening.telegrams.put(telegram)

    async def __from_knx(
        self, address: str, value: tuple[int, ...]
    ) -> Union[bool, float, int, None]:
        """
        Converts the given raw value into a Python value understandable by SVSHI.
        The address is used to determine which DPT needs to be used for the conversion.
        """
        dpt = self.__group_address_to_dpt.get(address, None)
        if dpt == None:
            return None

        if isinstance(dpt, DPTBinary):
            converted_value = True if value == self.__TRUE else False
        else:
            converted_value = dpt.from_knx(value)

        return converted_value

    async def initialize(self):
        """
        Initializes the system state by reading it from the KNX bus through an ephemeral connection.
        """
        # Default value is None for each field/address
        fields = defaultdict()
        async with self.__xknx_for_initialization as xknx:
            for address in self.__addresses:
                # Read from KNX the current value
                value_reader = ValueReader(
                    xknx,
                    GroupAddress(address),
                    timeout_in_seconds=self.__ADDRESS_INITIALIZATION_TIMEOUT,
                )
                telegram = await value_reader.read()
                field_address_name = self.__group_addr_to_field_name(address)
                if telegram and telegram.payload.value:
                    fields[field_address_name] = await self.__from_knx(
                        address, telegram.payload.value.value
                    )
                else:
                    fields[field_address_name] = None

        self._physical_state = PhysicalState(**fields)

        # Start executing the periodic apps in a background task
        if self.__periodic_apps:
            self.__periodic_apps_task = asyncio.create_task(self.__run_periodic_apps())

    def __group_addr_to_field_name(self, group_addr: str) -> str:
        """
        Converts a group address to its corresponding field name in PhysicalState.
        Example: 1/1/1 -> GA_1_1_1
        """
        return self.__GROUP_ADDRESS_PREFIX + group_addr.replace(
            self.__SLASH, self.__UNDERSCORE
        )

    def __field_name_to_group_addr(self, field: str) -> str:
        """
        Converts a PhysicalState field name to its corresponding group address.
        Example: GA_1_1_1 -> 1/1/1
        """
        return field.replace(self.__GROUP_ADDRESS_PREFIX, "").replace(
            self.__UNDERSCORE, self.__SLASH
        )

    def __compare(
        self, new_state: PhysicalState, old_state: PhysicalState
    ) -> List[Tuple[str, Union[bool, float, int, None]]]:
        """
        Compares new and old state and returns a list of (address_updated, value) pairs.
        """

        def read_fields(state: PhysicalState) -> Dict[str, str]:
            return {
                k: v
                for k, v in state.__dict__.items()
                if k.startswith(self.__GROUP_ADDRESS_PREFIX)
            }

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
        """
        Merges all the new states with the old one.
        """
        # The new states are sorted first by permission level, then by name.
        # First all the non-privileged apps are run in alphabetical order,
        # then the privileged ones in alphabetical order.
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
            updated_fields = self.__compare(state, old_state)
            for addr, value in updated_fields:
                setattr(res, self.__group_addr_to_field_name(addr), value)
        return res

    async def __run_periodic_apps(self):
        async def run_apps_with_lock():
            async with self.__physical_state_execution_lock:
                await self.__run_apps(apps)

        while True:
            # Run the apps sorted by timer in ascending order
            for timer, apps in self.__periodic_apps.items():
                # We use gather + sleep to run the apps every `timer` seconds without drift
                await asyncio.gather(
                    asyncio.sleep(float(cast(int, timer))),
                    run_apps_with_lock(),
                )

    async def __run_apps(self, apps: List[App]):
        """
        Executes all the given apps. Then, the physical state is updated, and the changes propagated to KNX.
        """
        old_state = dataclasses.replace(self._physical_state)
        new_states = {}
        # We first execute all the apps
        for app in apps:
            if app.should_run:
                per_app_state = dataclasses.replace(old_state)
                app.notify(per_app_state)
                if not self.__check_conditions_function(per_app_state):
                    # If conditions are not preserved, we prevent the app from running again
                    app.stop()
                else:
                    # If conditions are preserved, we keep the state to later propagate
                    new_states[app] = per_app_state

        merged_state = self.__merge_states(old_state, new_states)

        # Update the physical_state with the merged one
        self._physical_state = merged_state

        # Then we write to KNX for just the final values given to the updated fields
        updated_fields = self.__compare(merged_state, old_state)
        if updated_fields:
            for address, value in updated_fields:
                # Send to KNX
                await self.__send_value_to_knx(address, value)
                # Notify the listeners of the change
                await self.__notify_listeners(address)

    async def __notify_listeners(self, address: str):
        """
        Notifies all the listeners (i.e. apps) of the given address, triggering their execution.
        """
        if address in self.__addresses_listeners:
            await self.__run_apps(self.__addresses_listeners[address])
