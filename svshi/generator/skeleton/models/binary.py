###
### DO NOT TOUCH THIS FILE!!!
###

from models.device import Device
from models.multiton import multiton
from typing import Optional


@multiton
class BinarySensor(Device):
    """
    A binary sensor.
    """

    def __init__(self, name: str):
        super().__init__(name, "binary")

    def is_on(self) -> Optional[bool]:
        pass
