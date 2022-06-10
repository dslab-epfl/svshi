from asyncio.log import logger
import dataclasses
from dataclasses import dataclass, fields
import asyncio
import time
from asyncio.tasks import Task
from typing import Any, Callable, Dict, Final, List, Optional, Tuple, Union, cast
from collections import defaultdict
from enum import Enum
from xknx.core.value_reader import ValueReader
from xknx.dpt.dpt import DPTArray, DPTBase, DPTBinary
from xknx.telegram.apci import GroupValueWrite
from xknx.telegram.telegram import Telegram
from xknx.telegram.address import GroupAddress
from xknx.xknx import XKNX

import json

from .logger import Logger
from .verification_file import AppState, PhysicalState
from .runtime_file import InternalState
from .app import App
from .joint_apps import JointApps


class _LOG_TYPE(Enum):
    TELEGRAM = 1
    EXECUTION = 2


class State:

    __TRUE: Final = 1
    __FALSE: Final = 0
    __GROUP_ADDRESS_PREFIX: Final = "GA_"
    __SLASH: Final = "/"
    __UNDERSCORE: Final = "_"
    __ADDRESS_INITIALIZATION_TIMEOUT: Final = 10
    __LOGGER_BUFFER_SIZE = 32
    __APP_STATE_ARGUMENT_SUFFIX = "_app_state"

    def __init__(
        self,
        addresses_listeners: Dict[str, List[App]],
        joint_apps: List[JointApps],
        xknx_for_initialization: XKNX,
        xknx_for_listening: XKNX,
        check_conditions_function: Callable,
        group_address_to_dpt: Dict[str, Union[DPTBase, DPTBinary]],
        logs_dir: str,
        runtime_app_files_folder_path: str,
        physical_state_log_file_path: str,
    ):
        self.__addresses_listeners = addresses_listeners
        self.__addresses = list(addresses_listeners.keys())

        self.__apps = set(
            app for apps in self.__addresses_listeners.values() for app in apps
        )
        self.joint_apps = joint_apps

        self._physical_state: PhysicalState
        self._last_valid_physical_state: PhysicalState
        self._app_states = {f"{app.name}{self.__APP_STATE_ARGUMENT_SUFFIX}": AppState() for app in self.__apps}

        self._internal_state: InternalState = InternalState(time.localtime(), runtime_app_files_folder_path)

        # Used to access and modify the states
        self.__execution_lock = asyncio.Lock()

        self.__xknx_for_initialization = xknx_for_initialization
        self.__xknx_for_listening = xknx_for_listening
        self.__xknx_for_listening.telegram_queue.register_telegram_received_cb(
            self.__telegram_received_cb
        )

        self.__check_conditions_function = check_conditions_function
        self.__group_address_to_dpt = group_address_to_dpt

        periodic_apps = filter(
            lambda app: app.timer > 0,
            self.__apps,
        )

        # Group the apps by timer
        self.__periodic_apps: Dict[int, List[App]] = {}
        # for timer, group in groupby(
        #     sorted(periodic_apps, key=lambda app: app.timer),
        #     lambda app: app.timer,
        # ):
        #     self.__periodic_apps[timer] = sorted(
        #         group, key=lambda a: (a.is_privileged, a.name)
        #     )
        timers = []
        for timed_app in periodic_apps:
            timers.append(timed_app.timer)
        if len(timers) > 0:
            self.__periodic_apps[min(timers)] = self.joint_apps

        self.__periodic_apps_task: Optional[Task] = None

        self.__logger = Logger(logs_dir, self.__LOGGER_BUFFER_SIZE)

        self.physical_state_log_file_path = physical_state_log_file_path

    async def __telegram_received_cb(self, telegram: Telegram):
        """
        Updates the state once a telegram is received.
        """
        payload = telegram.payload
        address = str(telegram.destination_address)
        self.__logger.log_received_telegram(str(telegram))
        if address in self.__addresses:
            # The telegram was for one of the addresses we use
            if isinstance(payload, GroupValueWrite):
                # We react only to GroupValueWrite; since for reading we use ValueReader
                # to send the request and receive the response at once,
                # we do not need to listen to GroupValueResponse
                v = payload.value
                if v:
                    async with self.__execution_lock:
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

        # Dump all logs
        self.__logger.flush()

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
        self._last_valid_physical_state = PhysicalState(**fields)
        self._update_internal_state()

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

    def __read_physical_state_fields(
        self, state: PhysicalState
    ) -> Dict[str, Union[bool, float, int, None]]:
        return {
            k: v
            for k, v in state.__dict__.items()
            if k.startswith(self.__GROUP_ADDRESS_PREFIX)
        }

    def __compare(
        self, new_state: PhysicalState, old_state: PhysicalState
    ) -> List[Tuple[str, Union[bool, float, int, None]]]:
        """
        Compares new and old state and returns a list of (address_updated, value) pairs.
        """
        new_state_fields = self.__read_physical_state_fields(new_state)
        old_state_fields = self.__read_physical_state_fields(old_state)
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
        """
        Runs the periodic apps.
        """

        async def run_apps_with_lock():
            async with self.__execution_lock:
                await self.__run_apps(apps)

        while True:
            # Run the apps sorted by timer in ascending order
            for timer, apps in self.__periodic_apps.items():
                # We use gather + sleep to run the apps every `timer` seconds without drift
                await asyncio.gather(
                    asyncio.sleep(float(cast(int, timer))),
                    run_apps_with_lock(),
                )

    def __get_check_conditions_args(
        self, physical_state: PhysicalState, internal_state: InternalState
    ) -> Dict[str, Any]:
        check_conditions_args: Dict[str, Any] = {
            app_state_arg_name: state
            for app_state_arg_name, state in self._app_states.items()
        }
        check_conditions_args["physical_state"] = physical_state
        check_conditions_args["internal_state"] = internal_state
        return check_conditions_args

    async def __propagate_last_valid_state(self):
        fields = self.__read_physical_state_fields(self._last_valid_physical_state)
        for field, value in fields.items():
            # Send to KNX
            await self.__send_value_to_knx(
                self.__field_name_to_group_addr(field), value
            )

    def _update_internal_state(self, simulated_time=False):
        if not simulated_time:
            self._internal_state.date_time = time.localtime()

    async def __run_apps(self, apps: List[App]):
        """
        Executes all the given apps. Then, the physical state is updated, and the changes propagated to KNX.
        The execution lock needs to be acquired.
        """
        old_state = dataclasses.replace(self._physical_state)
        self._update_internal_state()

        # Check if the last state was valid
        check_conditions_args = self.__get_check_conditions_args(
            old_state, self._internal_state
        )
        is_last_state_valid = self.__check_conditions_function(**check_conditions_args)

        # We first execute all the apps
        new_states = {}
        for joint_app in self.joint_apps:
            self.__logger.log_execution(f"App to execute: {joint_app}")
            if joint_app.should_run:
                # Copy the states before executing the app
                # app_local_state_copy = dataclasses.replace(self._app_states[app.name])
                per_app_physical_state_copy = dataclasses.replace(old_state)
                internal_state_copy = dataclasses.replace(self._internal_state)

                self.__logger.log_execution(
                    f"With physical state (app: '{joint_app.name}'): {per_app_physical_state_copy}"
                )
                self.__logger.log_execution(
                    f"With app state (app: '{joint_app.name}'): {dict(sorted(self._app_states.items()))}"
                )
                self.__logger.log_execution(
                    f"With internal state (app: '{joint_app.name}: {internal_state_copy}"
                )

                # Notify the app to trigger execution
                joint_app.notify(
                    self._app_states,
                    physical_state=per_app_physical_state_copy,
                    internal_state=internal_state_copy,
                )

                # Build the check conditions args with all app states and the physical state
                check_conditions_args = self.__get_check_conditions_args(
                    per_app_physical_state_copy, internal_state_copy
                )

                # Update the conditions with the new app state
                # check_conditions_args[f"{app.name}_app_state"] = app_local_state_copy
                if is_last_state_valid and not self.__check_conditions_function(
                    **check_conditions_args
                ):
                    # If the last state was valid and conditions are not preserved after the app execution, we propagate
                    # the last valid state and stop svshi
                    joint_app.stop()

                    print(
                        "ERROR: the physical state is no longer valid! Propagating the last valid state to KNX... ",
                        end="",
                    )
                    await self.__propagate_last_valid_state()
                    print("done!")

                    await self.stop()
                    raise KeyboardInterrupt()
                else:
                    # If conditions are preserved, we keep the physical state to later propagate
                    new_states[joint_app] = per_app_physical_state_copy

                    # We update the app local state
                    # self._app_states[app.name] = app_local_state_copy

        merged_state = self.__merge_states(old_state, new_states)

        # Check if the merged state is valid
        check_conditions_args = self.__get_check_conditions_args(
            merged_state, self._internal_state
        )
        if not self.__check_conditions_function(**check_conditions_args):
            # Stop all apps
            for joint_app in self.joint_apps:
                joint_app.should_run = False

            print(
                "ERROR: the physical state is no longer valid! Propagating the last valid state to KNX... ",
                end="",
            )
            await self.__propagate_last_valid_state()
            print("done!")

            await self.stop()
            raise KeyboardInterrupt()
        else:
            # Update the physical_state and the last valid one with the merged one
            self._last_valid_physical_state = dataclasses.replace(merged_state)
            self._physical_state = dataclasses.replace(merged_state)

            # Then we write to KNX for just the final values given to the updated fields
            updated_fields = self.__compare(merged_state, old_state)
            if updated_fields:
                for address, value in updated_fields:
                    # Send to KNX
                    await self.__send_value_to_knx(address, value)

                    # Notify the listeners of the change
                    await self.__notify_listeners(address)
            
            self.log_current_physical_state()

    async def __notify_listeners(self, address: str):
        """
        Notifies all the listeners (i.e. apps) of the given address, triggering their execution.
        The execution lock needs to be acquired.
        """
        if address in self.__addresses_listeners:
            await self.__run_apps(self.__addresses_listeners[address])

    def log_current_physical_state(self) -> None:
        """
        Stores the current physical state in a file named __PHYSICAL_STATE_LOG_FILE_NAME in the current folder
        """
        self._physical_state
        dct = {}

        for ga in self._physical_state.__dataclass_fields__:
            value = getattr(self._physical_state, ga)
            dpt = self.__group_address_to_dpt[self.__field_name_to_group_addr(ga)]
            this_ga = {}
            this_ga["value"] = value
            this_ga["dpt"] = f"DPT{dpt.dpt_main_number}" if isinstance(dpt, DPTBase) else "DPT1"
            dct[ga] = this_ga
        with open(self.physical_state_log_file_path, "w") as f:
            json.dump(dct, f, indent=4)