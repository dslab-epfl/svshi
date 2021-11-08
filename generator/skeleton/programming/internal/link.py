###
### DO NOT TOUCH THIS FILE!!!
###

import copy
from communication.channel import ReadChannel, WriteChannel, ReadWriteChannel
from typing import Union


class Link:
    """
    A link between 2 or more channels.
    """

    def __init__(
        self,
        producer_channel: ReadChannel,
        consumer_channel: Union[WriteChannel, ReadWriteChannel],
        *consumer_channels: Union[WriteChannel, ReadWriteChannel]
    ):
        self.__channels = [producer_channel, consumer_channel].extend(consumer_channels)

    @property
    def channels(self):
        return copy.deepcopy(self.__channels)


def link(
    producer_channel: ReadChannel,
    consumer_channel: Union[WriteChannel, ReadWriteChannel],
    *consumer_channels: Union[WriteChannel, ReadWriteChannel]
):
    """
    Links 2 or more channels.
    """
    return Link(producer_channel, consumer_channel, *consumer_channels)
