from xknx.telegram.telegram import Telegram
from runtime.app import load_apps
from runtime.generation import generate_conditions_file, reset_conditions_file
from runtime.runner import AppRunner
from verification_file import PhysicalState
from runtime.verifier.verifier import Verifier
from runtime.verifier.tracker import StompWritesTracker
from xknx import XKNX
from xknx.core.value_reader import ValueReader
from xknx.telegram import GroupAddress
import json
import asyncio

GROUP_ADDRESSES_FILE_PATH = "app_library/group_addresses.json"


async def initialize_state(xknx: XKNX) -> PhysicalState:
    """
    Initializes the system state by reading it from the KNX bus.
    """
    # There are 6 __something__ values in the dict that we do not care about
    nb_fields = len(PhysicalState.__dict__) - 6
    n = nb_fields * [None]
    # Default value is None for each field/address
    state = PhysicalState(*n)
    with open(GROUP_ADDRESSES_FILE_PATH) as addresses_file:
        addresses_dict = json.load(addresses_file)
        for address_and_type in addresses_dict["addresses"]:
            # Read from KNX the current value
            address = address_and_type[0]
            value_reader = ValueReader(
                xknx, GroupAddress(address), timeout_in_seconds=5
            )
            telegram = await value_reader.read()
            if telegram and telegram.payload.value:
                setattr(
                    state,
                    f"GA_{address.replace('/', '_')}",
                    telegram.payload.value.value,
                )

    return state


async def telegram_received_cb(telegram: Telegram):
    """
    Updates the state once a telegram is received.
    """
    v = telegram.payload.value
    if v:
        setattr(
            state, f"GA_{str(telegram.destination_address).replace('/', '_')}", v.value
        )


async def cleanup(xknx: XKNX):
    print("Exiting... ", end="")
    reset_conditions_file()
    await xknx.stop()
    print("bye!")


state: PhysicalState


async def main():
    print("Welcome to the Pistis runtime verifier!")
    xknx = XKNX(daemon_mode=True)
    try:
        print("Connecting to KNX... ", end="")
        xknx.telegram_queue.register_telegram_received_cb(telegram_received_cb)
        await xknx.start()
        print("done!")
        print("Connecting to the tracker... ", end="")
        with StompWritesTracker() as tracker:
            print("done!")
            print("Initializing verifier... ", end="")
            generate_conditions_file()
            state = await initialize_state(xknx)
            apps = load_apps()
            app_runner = AppRunner(apps)
            verifier = Verifier(app_runner, state)
            print("done!")

            # Register on message callback to verify writes
            tracker.on_message(verifier.verify_write)

            # Run the apps in an infinite loop
            print("Starting the apps...")
            app_runner.run_all()

            await cleanup(xknx)

    except KeyboardInterrupt:
        await cleanup(xknx)


if __name__ == "__main__":
    asyncio.run(main())
