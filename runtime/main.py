from xknx.telegram.telegram import Telegram
from runtime.app import get_addresses_listeners, get_apps
from runtime.generator import ConditionsGenerator
from runtime.state import State
from xknx import XKNX
import asyncio

APP_LIBRARY_DIR = "app_library"
CONDITIONS_FILE_PATH = "runtime/conditions.py"
VERIFICATION_FILE_PATH = "runtime/verification_file.py"
VERIFICATION_MODULE_PATH = "verification"


state: State


async def telegram_received_cb(telegram: Telegram):
    """
    Updates the state once a telegram is received.
    """
    v = telegram.payload.value
    if v:
        await state.update(str(telegram.destination_address), v.value)


async def cleanup(xknx: XKNX, generator: ConditionsGenerator):
    """
    Resets the verification and the conditions files, and stops XKNX.
    """
    print("Exiting... ", end="")
    generator.reset_verification_file()
    generator.reset_conditions_file()
    await xknx.stop()
    print("bye!")


async def main():
    xknx = XKNX(daemon_mode=True)
    conditions_generator = ConditionsGenerator(
        APP_LIBRARY_DIR, CONDITIONS_FILE_PATH, VERIFICATION_FILE_PATH
    )
    try:
        print("Connecting to KNX... ", end="")
        xknx.telegram_queue.register_telegram_received_cb(telegram_received_cb)
        print("done!")
        print("Initializing state and listeners... ", end="")
        conditions_generator.copy_verification_file_from_verification_module(
            VERIFICATION_MODULE_PATH
        )
        conditions_generator.generate_conditions_file()
        apps = get_apps(APP_LIBRARY_DIR, "verification_file")
        addresses_listeners = get_addresses_listeners(apps)
        [app.install_requirements() for app in apps]
        await State.create(xknx, addresses_listeners)
        print("done!")

        print("Listening to KNX telegrams...")
        await xknx.start()

        await cleanup(xknx, conditions_generator)

    except KeyboardInterrupt:
        await cleanup(xknx, conditions_generator)


if __name__ == "__main__":
    asyncio.run(main())
