###
### DO NOT TOUCH THIS FILE!!!
###

from xknx.devices import BinarySensor as KnxBinarySensor
from communication.client import KNX
from models.addresses import GROUP_ADDRESSES
from models.device import Device
from models.multiton import multiton
from typing import Union
from asgiref.sync import async_to_sync


@multiton
class BinarySensor(Device):
    """
    A binary sensor.
    """

    def __init__(self, name: str):
        super().__init__(name, "binary")
        self.__sensor = KnxBinarySensor(
            KNX,
            name=name,
            group_address_state=GROUP_ADDRESSES[name]["address"],
        )

    def is_on(self) -> Union[bool, None]:
        async_to_sync(self.__sensor.sync)(wait_for_result=True)
        return self.__sensor.state
