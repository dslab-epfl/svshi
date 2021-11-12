###
### DO NOT TOUCH THIS FILE!!!
###

from xknx import XKNX
from xknx.io.connection import ConnectionConfig, ConnectionType
from typing import Callable

KNX = XKNX()


async def with_knx_client(address: str, code: Callable[[], None]):
    """
    Connects the KNX client to the gateway with the given address and runs the given code.
    """
    KNX.connection_config = ConnectionConfig(
        connection_type=ConnectionType.AUTOMATIC, gateway_ip=address
    )
    await KNX.start()
    code()
    await KNX.stop()
