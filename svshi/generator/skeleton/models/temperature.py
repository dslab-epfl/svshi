###
### DO NOT TOUCH THIS FILE!!!
###

from typing import Union
from models.device import Device
from models.multiton import multiton


@multiton
class TemperatureSensor(Device):
    """
    A temperature sensor.
    """

    def __init__(self, name: str):
        super().__init__(name, "temperature")

    def read(self) -> Union[float, None]:
        pass
