###
### DO NOT TOUCH THIS FILE!!!
###

from xknx.devices import Switch as KnxSwitch
from communication.client import KNX
from models.addresses import GROUP_ADDRESSES
from models.device import Device
from models.multiton import multiton
from typing import Union


@multiton
class Switch(Device):
    def __init__(self, name: str):
        self.__switch = KnxSwitch(
            KNX,
            name=name,
            group_address=GROUP_ADDRESSES[name]["writeAddress"],
            group_address_state=GROUP_ADDRESSES[name]["readAddress"],
        )

    def on(self):
        self._async_loop.run_until_complete(self.__switch.set_on())

    def off(self):
        self._async_loop.run_until_complete(self.__switch.set_off())

    def is_on(self) -> Union[bool, None]:
        self._async_loop.run_until_complete(self.__switch.sync(wait_for_result=True))
        return self.__switch.state
