from xknx.telegram.telegram import Telegram
from runtime.app import get_addresses_listeners, get_apps
from runtime.conditions import generate_conditions_file, reset_conditions_file
from runtime.state import State
from xknx import XKNX
import asyncio

state: State


async def telegram_received_cb(telegram: Telegram):
    """
    Updates the state once a telegram is received.
    """
    v = telegram.payload.value
    if v:
        state.update(str(telegram.destination_address), v.value)


async def cleanup(xknx: XKNX):
    print("Exiting... ", end="")
    reset_conditions_file()
    await xknx.stop()
    print("bye!")


async def main():
    xknx = XKNX(daemon_mode=True)
    try:
        print("Connecting to KNX... ", end="")
        xknx.telegram_queue.register_telegram_received_cb(telegram_received_cb)
        await xknx.start()
        print("done!")
        print("Initializing state and listeners... ", end="")
        generate_conditions_file()
        addresses_listeners = get_addresses_listeners()
        apps = get_apps()
        [app.install_requirements() for app in apps]
        state = State(addresses_listeners, apps)
        await state.initialize(xknx)
        print("done!")

        await cleanup(xknx)

    except KeyboardInterrupt:
        await cleanup(xknx)


if __name__ == "__main__":
    asyncio.run(main())
