from xknx import XKNX

KNX = XKNX(state_updater=True)


async def with_knx_client(code):
    await KNX.start()
    code()
    await KNX.stop()
