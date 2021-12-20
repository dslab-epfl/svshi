import dataclasses
from typing import Callable, Dict, List, Tuple, Union
from collections import defaultdict
from xknx.core.value_reader import ValueReader
from xknx.dpt.dpt import DPTArray, DPTBase, DPTBinary
from xknx.dpt.dpt_2byte_float import DPT2ByteFloat
from xknx.telegram.apci import GroupValueWrite
from xknx.telegram.telegram import Telegram
from xknx.telegram.address import GroupAddress
from xknx.xknx import XKNX
from .verification_file import PhysicalState
from .app import App


class State:
    def __init__(
        self,
        addresses_listeners: Dict[str, List[App]],
        xknx_for_initialization: XKNX,
        xknx_for_listening: XKNX,
        check_conditions_function: Callable[[PhysicalState], bool],
        group_address_to_dpt: Dict[str, DPTBase],
    ):
        self._physical_state: PhysicalState
        self.__xknx_for_initialization = xknx_for_initialization
        self.__xknx_for_listening = xknx_for_listening
        self.__xknx_for_listening.telegram_queue.register_telegram_received_cb(
            self.__telegram_received_cb
        )
        self.__addresses_listeners = addresses_listeners
        self.__addresses = list(addresses_listeners.keys())
        self.__check_conditions_function = check_conditions_function
        self.__group_address_to_dpt = group_address_to_dpt
        print(DPT2ByteFloat.to_knx(2.3))

    async def __telegram_received_cb(self, telegram: Telegram):
        """
        Updates the state once a telegram is received.
        """
        payload = telegram.payload
        if isinstance(payload, GroupValueWrite):
            v = payload.value
            if v:
                address = str(telegram.destination_address)
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
        await self.__xknx_for_listening.stop()

    async def __send_value_to_knx(self, address: str, value: Union[bool, float, int]):
        dpt = self.__group_address_to_dpt[address]
        write_content: Union[DPTBinary, DPTArray, None] = None
        if isinstance(dpt, DPTBinary):
            binary_value = 1 if value else 0
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
    ) -> Union[bool, float, int]:
        dpt = self.__group_address_to_dpt[address]
        if isinstance(dpt, DPTBinary):
            converted_value = True if value == 1 else False
        else:
            converted_value = dpt.from_knx(value)

        return converted_value

    async def initialize(self):
        """
        Initializes the system state by reading it from the KNX bus through an ephimeral connection.
        """
        # Default value is None for each field/address
        fields = defaultdict()
        async with self.__xknx_for_initialization as xknx:
            for address in self.__addresses:
                # Read from KNX the current value
                value_reader = ValueReader(
                    xknx, GroupAddress(address), timeout_in_seconds=5
                )
                telegram = await value_reader.read()
                if telegram and telegram.payload.value:
                    fields[self.__group_addr_to_field_name(address)] = await self.__from_knx(
                        address, telegram.payload.value.value
                    )

        self._physical_state = PhysicalState(**fields)

    def __group_addr_to_field_name(self, group_addr: str) -> str:
        return "GA_" + group_addr.replace("/", "_")

    def __field_name_to_group_addr(self, field: str) -> str:
        return field.replace("GA_", "").replace("_", "/")

    def __compare(
        self, new_state: PhysicalState, old_state: PhysicalState
    ) -> List[Tuple[str, Union[bool, float]]]:
        def read_fields(state: PhysicalState) -> Dict[str, str]:
            return {k: v for k, v in state.__dict__.items() if k.startswith("GA_")}

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
            updated_fields = self.__compare(state, old_state)
            for addr, value in updated_fields:
                setattr(res, self.__group_addr_to_field_name(addr), value)
        return res

    async def __notify_listeners(self, address: str):
        # We first execute all the listeners
        old_state = dataclasses.replace(self._physical_state)
        new_states = {}
        for app in self.__addresses_listeners[address]:
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
                await self.__send_value_to_knx(address, value)

                # Notify the listeners of the change
                await self.__notify_listeners(address)
