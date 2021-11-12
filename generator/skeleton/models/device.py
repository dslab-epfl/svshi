###
### DO NOT TOUCH THIS FILE!!!
###

import asyncio
from abc import ABC


class Device(ABC):
    def __init__(self):
        super().__init__()
        self._async_loop = asyncio.get_event_loop()
