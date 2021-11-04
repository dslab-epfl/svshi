import copy
from communication.channel import Channel


class Link:
    """
    A link between 2 or more channels.
    """

    def __init__(self, channel1: Channel, channel2: Channel, *channels: Channel):
        self.__channels = [channel1, channel2].extend(channels)

    @property
    def channels(self):
        return copy.deepcopy(self.__channels)


def link(channel1: Channel, channel2: Channel, *channels: Channel):
    """
    Links 2 or more channels.
    """
    return Link(channel1, channel2, *channels)
