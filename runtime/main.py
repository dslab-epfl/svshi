from verifier.verifier import Verifier
from verifier.tracker import StompWritesTracker
from xknx import XKNX
from xknx.core.value_reader import ValueReader
from xknx.telegram import GroupAddress
import argparse
import time
import json
import asyncio


def parse_args():
    """
    Parses the arguments, returning the list of apps process ids.
    """
    parser = argparse.ArgumentParser(description="Runtime verifier.")
    parser.add_argument(
        "-l", "--list", nargs="+", help="A list of '<app_name>/<pid>'", required=True
    )
    args = parser.parse_args()
    return args.list


async def initialize_state() -> dict:
    """
    Initializes the system state by reading it from the KNX bus.
    """
    state = {}
    with open("../app-library/group_addresses.json") as addresses_file:
        async with XKNX() as xknx:
            addresses_dict = json.load(addresses_file)
            for address in addresses_dict["addresses"]:
                # TODO Read from KNX the current value
                value_reader = ValueReader(xknx, GroupAddress(address))
                telegram = await value_reader.read()
                if telegram:
                    state[address] = telegram.payload.value.value
                else:
                    state[address] = None

    return state


async def main():
    apps_pids = parse_args()
    print("Welcome to the Pistis runtime verifier!")

    try:
        apps = {s.split("/")[0]: s.split("/")[1] for s in apps_pids}
        print("Connecting to the tracker... ", end="")
        with StompWritesTracker() as tracker:
            print("done!")
            print("Initializing verifier... ", end="")
            # TODO: preconditions_check
            state = await initialize_state()
            preconditions_check = lambda s: True
            verifier = Verifier(apps, state, preconditions_check)
            print("done!")

            # Register on message callback to verify writes
            tracker.on_message(verifier.verify_write)

            # Infinite loop for listening to messages
            while True:
                time.sleep(1)

    except KeyboardInterrupt:
        print("Exiting...")


if __name__ == "__main__":
    asyncio.run(main())
