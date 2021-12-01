###
### DO NOT TOUCH THIS FILE!!!
###

from xknx.devices import Switch as KnxSwitch
from communication.client import KNX
from models.addresses import GROUP_ADDRESSES
from models.device import Device
from models.multiton import multiton
from typing import Union
from asgiref.sync import async_to_sync


@multiton
class Switch(Device):
    """
    A switch.
    """

    def __init__(self, name: str):
        super().__init__(name, "switch")
        self.__group_address_write = GROUP_ADDRESSES[name]["writeAddress"]
        self.__group_address_read = GROUP_ADDRESSES[name]["readAddress"]
        self.__switch = KnxSwitch(
            KNX,
            name=name,
            group_address=self.__group_address_write,
            group_address_state=self.__group_address_read,
        )

    def on(self):
        self._tracker.save(self.__group_address_write, True)
        async_to_sync(self.__switch.set_on)

    def off(self):
        self._tracker.save(self.__group_address_write, False)
        async_to_sync(self.__switch.set_off)

    def is_on(self) -> Union[bool, None]:
        async_to_sync(self.__switch.sync)(wait_for_result=True)
        state = self.__switch.state
        if state:
            self._tracker.save(self.__group_address_read, state)
        return state
