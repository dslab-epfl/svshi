from enum import Enum


class ChannelType(Enum):
    """
    The type of the channel.
    """

    IN = "IN"
    OUT = "OUT"
    IN_OUT = "IN/OUT"


class Channel:
    """
    A device channel.
    """

    def __init__(self, name: str, datatype: str, type: ChannelType):
        self.name = name
        self.datatype = datatype
        self.type = type
