from enum import Enum
from typing import Union
from abc import ABC
from xknx.devices import NumericValue
from client import KNX
from addresses import GROUP_ADDRESSES
import asyncio


class ChannelType(Enum):
    """
    The type of the channel.
    """

    IN = "IN"
    OUT = "OUT"
    IN_OUT = "IN/OUT"


class Channel(ABC):
    """
    A device channel.
    """

    def __init__(self, name: str, datatype: str, type: ChannelType):
        self.name = name
        self.datatype = datatype
        self.type = type
        self.async_loop = asyncio.get_event_loop()

    def __convert_to_value_type(self, datatype: str) -> int:
        return int(datatype.split("-")[1].split(".")[0])


class WriteChannel(Channel):
    """
    A device write channel.
    """

    def __init__(self, name: str, datatype: str, type: ChannelType = ChannelType.IN):
        super().__init__(name, datatype, type)
        self.__underlying_value = NumericValue(
            xknx=KNX,
            name=name,
            group_address=GROUP_ADDRESSES[name],
            respond_to_read=True,
            value_type=super().__convert_to_value_type(datatype),
        )

    def write(self, val: Union[float, int]):
        """
        Writes the given value to the channel.
        """
        self.async_loop.run_until_complete(self.__underlying_value.set(val))


class ReadChannel(Channel):
    """
    A device read channel.
    """

    def __init__(self, name: str, datatype: str, type: ChannelType = ChannelType.OUT):
        super().__init__(name, datatype, type)
        self.__underlying_value = NumericValue(
            xknx=KNX,
            name=name,
            group_address_state=GROUP_ADDRESSES[name],
            respond_to_read=True,
            value_type=super().__convert_to_value_type(datatype),
        )

    def read(self) -> Union[float, int, None]:
        """
        Reads from the channel.
        """
        self.async_loop.run_until_complete(
            self.__underlying_value.sync(wait_for_result=True)
        )
        return self.__underlying_value.resolve_state()


class ReadWriteChannel(Channel):
    """
    A device read/write channel.
    """

    def __init__(
        self, name: str, datatype: str, type: ChannelType = ChannelType.IN_OUT
    ):
        super().__init__(name, datatype, type)
        self.__underlying_value = NumericValue(
            xknx=KNX,
            name=name,
            group_address=GROUP_ADDRESSES[name],
            group_address_state=GROUP_ADDRESSES[name],
            respond_to_read=True,
            value_type=super().__convert_to_value_type(datatype),
        )

    def write(self, val: Union[float, int]):
        """
        Writes the given value to the channel.
        """
        self.async_loop.run_until_complete(self.__underlying_value.set(val))

    def read(self) -> Union[float, int, None]:
        """
        Reads from the channel.
        """
        self.async_loop.run_until_complete(
            self.__underlying_value.sync(wait_for_result=True)
        )
        return self.__underlying_value.resolve_state()
