###
### DO NOT TOUCH THIS FILE!!!
###

from abc import ABC

from models.tracker import WritesTracker, StompWritesTracker


class Device(ABC):
    """
    A KNX device.
    """

    def __init__(self, name: str, type: str):
        super().__init__()
        self.name = name
        self.type = type
        self._tracker: WritesTracker = self.__init_writes_tracker()

    def __init_writes_tracker(self) -> WritesTracker:
        tracker = StompWritesTracker()
        tracker.connect()
        return tracker
