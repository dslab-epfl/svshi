###
### DO NOT TOUCH THIS FILE!!!
###

from abc import ABC
from stomp import Connection, ConnectionListener
import json

from typing import Callable
from verifier.device import Device, get_device_from_type


class Message:
    """
    A write message received by the tracker.
    """

    def __init__(self, app_name: str, device: Device, data: str):
        self.app_name = app_name
        self.device = device
        self.data = data


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

    def on_message(self, callback: Callable[[Message], None]):
        """
        Registers a callback to execute when a message is received.
        """
        pass

    def __enter__(self):
        self.connect()
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.disconnect()


def _connect_and_subscribe(conn: Connection):
    conn.connect(username="verifier", wait=True)
    conn.subscribe(destination="/writes", id=1, ack="auto")


class _ReconnectListener(ConnectionListener):
    def __init__(self, conn):
        self.__conn = conn

    def on_disconnected(self):
        print("Disconnected, reconnecting... ", end=None)
        _connect_and_subscribe(self.__conn)
        print("done!")


class _MessageListener(ConnectionListener):
    def __init__(self, callback: Callable[[Message], None]):
        self.__callback = callback

    def on_error(self, frame):
        print('Received an error "%s"' % frame.body)

    def on_message(self, frame):
        print('Received a message "%s"' % frame.body)
        msg = json.loads(frame.body)
        self.__callback(
            Message(
                msg["app"],
                get_device_from_type(msg["deviceType"], msg["deviceName"]),
                msg["data"],
            )
        )


class StompWritesTracker(WritesTracker):
    """
    A writes tracker that uses the Stomp protocol.
    """

    def __init__(self):
        super().__init__()
        self.__conn = Connection()

    def connect(self):
        _connect_and_subscribe(self.__conn)
        self.__conn.set_listener("reconnect_listener", _ReconnectListener(self.__conn))

    def disconnect(self):
        self.__conn.disconnect()

    def on_message(self, callback: Callable[[Message], None]):
        self.__on_message_callback = callback
        self.__conn.set_listener(
            "message_listener", _MessageListener(self.__on_message_callback)
        )
