###
### DO NOT TOUCH THIS FILE!!!
###

from typing import Union
from models.device import Device
from models.multiton import multiton


@multiton
class HumiditySensor(Device):
    """
    A humidity sensor.
    """

    def __init__(self, name: str):
        super().__init__(name, "humidity")

    def read(self) -> Union[float, None]:
        pass
