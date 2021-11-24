from xknx.telegram.telegram import Telegram
from verifier.verifier import Verifier
from verifier.tracker import StompWritesTracker
from xknx import XKNX
from xknx.core.value_reader import ValueReader
from xknx.telegram import GroupAddress
import argparse
import time
import json
import asyncio
import os


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


async def initialize_state(xknx: XKNX) -> dict:
    """
    Initializes the system state by reading it from the KNX bus.
    """
    state = {}
    with open("../app_library/group_addresses.json") as addresses_file:
        addresses_dict = json.load(addresses_file)
        for address in addresses_dict["addresses"]:
            # Read from KNX the current value
            value_reader = ValueReader(xknx, GroupAddress(address))
            telegram = await value_reader.read()
            if telegram and telegram.payload.value:
                state[address] = telegram.payload.value.value
            else:
                state[address] = None

    return state


def generate_conditions_file():
    """
    Generates the conditions file given the conditions of all the apps installed in the library.
    """
    # TODO: replace devices usages with the verification ones, same with the function calls with the physical state
    apps_dirs = [
        f.name
        for f in os.scandir("../app_library")
        if f.is_dir() and f.name != "__pycache__"
    ]
    imports = ""
    imports_code = []
    for app in apps_dirs:
        import_statement = f"import app_library.{app}.main\n"
        if import_statement not in imports:
            imports += import_statement
            imports_code.append(f"app_library.{app}.main")

    check_conditions_body = ""
    nb_imports = len(imports_code)
    for i, import_code in enumerate(imports_code):
        suffix = " and " if i < nb_imports - 1 else ""
        check_conditions_body += f"{import_code}.precond(state){suffix}"

    file = f"""
{imports}
def check_conditions(state: dict) -> bool:
  return {check_conditions_body}
        """.strip()

    output_filename = f"verifier/conditions.py"
    os.makedirs(os.path.dirname(output_filename), exist_ok=True)
    with open(output_filename, "w+") as output_file:
        output_file.write(file)


def reset_conditions_file():
    """
    Resets the conditions file.
    """
    file = f"""
def check_conditions(state: dict) -> bool:
  return True    
    """.strip()

    output_filename = f"verifier/conditions.py"
    os.makedirs(os.path.dirname(output_filename), exist_ok=True)
    with open(output_filename, "w+") as output_file:
        output_file.write(file)


async def telegram_received_cb(telegram: Telegram):
    """Do something with the received telegram."""
    v = telegram.payload.value
    if v:
        state[str(telegram.destination_address)] = v.value


state = {}


async def main():
    apps_pids = parse_args()
    print("Welcome to the Pistis runtime verifier!")
    xknx = XKNX(daemon_mode=True)
    try:
        print("Connecting to KNX... ", end="")
        xknx.telegram_queue.register_telegram_received_cb(telegram_received_cb)
        await xknx.start()
        print("done!")
        apps = {s.split("/")[0]: s.split("/")[1] for s in apps_pids}
        print("Connecting to the tracker... ", end="")
        with StompWritesTracker() as tracker:
            print("done!")
            print("Initializing verifier... ", end="")
            generate_conditions_file()
            state = await initialize_state(xknx)
            verifier = Verifier(apps, state)
            print("done!")

            # Register on message callback to verify writes
            tracker.on_message(verifier.verify_write)

            # Infinite loop for listening to messages
            while True:
                time.sleep(1)

    except KeyboardInterrupt:
        print("Exiting...")
        reset_conditions_file()
        await xknx.stop()


if __name__ == "__main__":
    asyncio.run(main())
