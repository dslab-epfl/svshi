from abc import ABC


class Channel(ABC):
    """
    A device channel.
    """


class WriteChannel(Channel):
    pass


class ReadChannel(Channel):
    pass


class ReadWriteChannel(Channel):
    pass
