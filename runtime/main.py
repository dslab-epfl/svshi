from runtime.app import get_addresses_listeners, get_apps
from runtime.generator import ConditionsGenerator
from runtime.state import State
import asyncio

APP_LIBRARY_DIR = "app_library"
CONDITIONS_FILE_PATH = "runtime/conditions.py"
VERIFICATION_FILE_PATH = "runtime/verification_file.py"
VERIFICATION_MODULE_PATH = "verification"


async def cleanup(generator: ConditionsGenerator):
    """
    Resets the verification and the conditions files, and stops XKNX.
    """
    print("Exiting... ", end="")
    generator.reset_verification_file()
    generator.reset_conditions_file()
    print("bye!")


async def main():
    conditions_generator = ConditionsGenerator(
        APP_LIBRARY_DIR, CONDITIONS_FILE_PATH, VERIFICATION_FILE_PATH
    )
    try:
        print("Initializing state and listeners... ", end="")
        conditions_generator.copy_verification_file_from_verification_module(
            VERIFICATION_MODULE_PATH
        )
        conditions_generator.generate_conditions_file()
        apps = get_apps(APP_LIBRARY_DIR, "verification_file")
        addresses_listeners = get_addresses_listeners(apps)
        [app.install_requirements() for app in apps]
        state = State(addresses_listeners)
        await state.initialize()
        print("done!")

        print("Connecting to KNX and listening to telegrams...")
        await state.listen()

        print("Disconnecting from KNX...", end="")
        await state.stop()
        print("done!")

        await cleanup(conditions_generator)

    except KeyboardInterrupt:
        await cleanup(conditions_generator)


if __name__ == "__main__":
    asyncio.run(main())
