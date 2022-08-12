###
### DO NOT TOUCH THIS FILE!!!
###

from models.device import Device
from models.multiton import multiton
from typing import Optional


@multiton
class DimmerActuator(Device):
    """
    A dimmer actuator.
    """

    def __init__(self, name: str):
        super().__init__(name, "dimmerActuator")

    def set(self, value: int):
        """
        Sets the value of the dimmer actuator.
        """
        pass

    def read(self) -> Optional[bool]:
        """
        Reads the value of the sensor. Returns None if KNX did not answer.
        """
        pass
