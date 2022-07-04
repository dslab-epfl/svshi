###
### DO NOT TOUCH THIS FILE!!!
###

from models.device import Device
from models.multiton import multiton
from typing import Optional


@multiton
class DimmerSensor(Device):
    """
    A dimmer sensor.
    """

    def __init__(self, name: str):
        super().__init__(name, "dimmerSensor")

    def read(self) -> Optional[int]:
        """
        Reads the value of the sensor. Returns None if KNX did not answer.
        """
        pass
