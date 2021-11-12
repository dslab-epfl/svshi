###
### DO NOT TOUCH THIS FILE!!!
###

from xknx.devices import BinarySensor as KnxBinarySensor
from communication.client import KNX
from models.addresses import GROUP_ADDRESSES
from models.device import Device
from models.multiton import multiton
from typing import Union


@multiton
class BinarySensor(Device):
    def __init__(self, name: str):
        self.__sensor = KnxBinarySensor(
            KNX,
            name=name,
            group_address_state=GROUP_ADDRESSES[name]["address"],
        )

    def is_on(self) -> Union[bool, None]:
        self._async_loop.run_until_complete(self.__sensor.sync(wait_for_result=True))
        return self.__sensor.state
