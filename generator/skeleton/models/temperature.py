###
### DO NOT TOUCH THIS FILE!!!
###

from xknx.devices import Sensor as KnxSensor
from communication.client import KNX
from models.addresses import GROUP_ADDRESSES
from models.device import Device
from models.multiton import multiton


@multiton
class TemperatureSensor(Device):
    """
    A temperature sensor.
    """

    def __init__(self, name: str):
        self.__sensor = KnxSensor(
            KNX,
            name=name,
            group_address_state=GROUP_ADDRESSES[name]["address"],
            value_type="temperature",
        )

    def read(self):
        self._async_loop.run_until_complete(self.__sensor.sync(wait_for_result=True))
        return self.__sensor.resolve_state()
