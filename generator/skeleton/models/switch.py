###
### DO NOT TOUCH THIS FILE!!!
###

from models.device import Device
from models.multiton import multiton
from typing import Union


@multiton
class Switch(Device):
    """
    A switch.
    """

    def __init__(self, name: str):
        super().__init__(name, "switch")

    def on(self):
        pass

    def off(self):
        pass

    def is_on(self) -> Union[bool, None]:
        pass
