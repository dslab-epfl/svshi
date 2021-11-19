###
### DO NOT TOUCH THIS FILE!!!
###

from abc import ABC
import stomp
import json

from typing import TYPE_CHECKING
from pathlib import Path

if TYPE_CHECKING:
    from models.device import Device


class WritesTracker(ABC):
    """
    A generic writes tracker.
    """

    def __init__(self):
        super().__init__()

    def connect(self):
        """
        Connects to the tracker.
        """
        pass

    def disconnect(self):
        """
        Disconnects from the tracker.
        """
        pass

    def save(self, device: "Device", data: str):
        """
        Saves in the tracker the write from the given device with the given data.
        """
        pass


class StompWritesTracker(WritesTracker):
    """
    A writes tracker that uses the Stomp protocol.
    """

    __APP_NAME = Path(__file__).parent.parent.name
    __QUEUE_NAME = "/writes"

    def __init__(self):
        super().__init__()
        self.__conn = stomp.Connection()

    def connect(self):
        self.__conn.connect(username=self.__APP_NAME, wait=True)

    def disconnect(self):
        self.__conn.disconnect()

    def save(self, device: "Device", data: str):
        msg = {
            "app": self.__APP_NAME,
            "deviceType": device.type,
            "deviceName": device.name,
            "data": data,
        }
        self.__conn.send(self.__QUEUE_NAME, json.dumps(msg))
