import json
from typing import Dict, List, Union
from xknx.core.value_reader import ValueReader
from xknx.telegram.address import GroupAddress
from xknx.xknx import XKNX
from verification_file import PhysicalState
from runtime.app import App


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

    def __notify_listeners(self, address: str):
        [
            app.notify(self.__physical_state)
            for app in self.__addresses_listeners[address]
            if app.should_run
        ]

    def update(self, address: str, value: Union[str, bool, float]):
        setattr(self.__physical_state, f"GA_{address.replace('/', '_')}", value)
        self.__notify_listeners(address)
