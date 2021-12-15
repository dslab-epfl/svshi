from typing import Tuple
from runtime.app import get_addresses_listeners, get_apps
from runtime.generator import ConditionsGenerator
from runtime.state import State
import argparse
import asyncio
import os
import re

SVSHI_HOME = os.environ["SVSHI_HOME"]
SVSHI_FOLDER = f"{SVSHI_HOME}/svshi"

APP_LIBRARY_DIR = f"{SVSHI_FOLDER}/app_library"
GROUP_ADDRESSES_FILE_PATH = f"{APP_LIBRARY_DIR}/group_addresses.json"
CONDITIONS_FILE_PATH = f"{SVSHI_FOLDER}/runtime/conditions.py"
VERIFICATION_FILE_PATH = f"{SVSHI_FOLDER}/runtime/verification_file.py"
VERIFICATION_MODULE_PATH = f"{SVSHI_FOLDER}/verification"


def parse_args() -> Tuple[str, int]:
    """
    Prepares the argument parser, parses the provided arguments and returns the retrieved information as a tuple.
    """
    parser = argparse.ArgumentParser(description="Runtime module.")
    parser.add_argument("ip_address", type=str, help="the KNX IP address to use")
    parser.add_argument("port", type=int, help="the KNX port to use")
    args = parser.parse_args()

    address_regex = re.compile(r"^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$")
    address = args.ip_address
    if not address_regex.match(address):
        raise ValueError(f"Wrong IP address '{address}': it has to be a valid address")

    port = args.port
    if port <= 0:
        raise ValueError(f"Wrong port '{port}': it has to be a valid port")

    return address, port


async def cleanup(generator: ConditionsGenerator, error: bool = False):
    """
    Resets the verification and the conditions files.
    """
    error_message = "An error occurred!\n" if error else ""
    print(f"{error_message}Exiting... ", end="")
    generator.reset_verification_file()
    generator.reset_conditions_file()
    print("bye!")


async def main():
    knx_address, knx_port = parse_args()
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
        state = State(
            GROUP_ADDRESSES_FILE_PATH, addresses_listeners, knx_address, knx_port
        )
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
    except BaseException as e:
        await cleanup(conditions_generator, error=True)
        raise e


if __name__ == "__main__":
    asyncio.run(main())
