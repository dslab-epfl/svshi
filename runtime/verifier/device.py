from abc import ABC


class Device(ABC):
    """
    A device that writes.
    """

    def __init__(self, name: str):
        super().__init__()
        self.name = name


class Switch(Device):
    """
    A switch.
    """

    def __init__(self, name: str):
        super().__init__(name)


def get_device_from_type(device_type: str, device_name: str) -> Device:
    """
    Gets the right device given the type and the name.
    """
    if device_type == "switch":
        return Switch(device_name)
    else:
        raise ValueError(f"Unknown device type '{device_type}'")
