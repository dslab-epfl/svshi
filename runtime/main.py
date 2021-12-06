from xknx.telegram.telegram import Telegram
from runtime.app import get_addresses_listeners, get_apps
from runtime.generator import ConditionsGenerator
from runtime.state import State
from xknx import XKNX
import asyncio

APP_LIBRARY_DIR = "app_library"


state: State


async def telegram_received_cb(telegram: Telegram):
    """
    Updates the state once a telegram is received.
    """
    v = telegram.payload.value
    if v:
        await state.update(str(telegram.destination_address), v.value)


async def cleanup(xknx: XKNX, generator: ConditionsGenerator):
    print("Exiting... ", end="")
    generator.reset_conditions_file()
    await xknx.stop()
    print("bye!")


async def main():
    xknx = XKNX(daemon_mode=True)
    conditions_generator = ConditionsGenerator("app_library", "runtime/conditions.py")
    try:
        print("Connecting to KNX... ", end="")
        xknx.telegram_queue.register_telegram_received_cb(telegram_received_cb)
        await xknx.start()
        print("done!")
        print("Initializing state and listeners... ", end="")
        conditions_generator.generate_conditions_file()
        addresses_listeners = get_addresses_listeners()
        apps = get_apps()
        [app.install_requirements() for app in apps]
        state = State(xknx, addresses_listeners, apps)
        await state.initialize()
        print("done!")

        await cleanup(xknx, conditions_generator)

    except KeyboardInterrupt:
        await cleanup(xknx, conditions_generator)


if __name__ == "__main__":
    asyncio.run(main())
