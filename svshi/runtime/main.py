from typing import Tuple
from xknx.io.connection import ConnectionConfig, ConnectionType
from xknx.xknx import XKNX
from runtime.app import get_addresses_listeners, get_apps
from runtime.generator import ConditionsGenerator
from runtime.state import State
from runtime.conditions import check_conditions
import argparse
import asyncio
import os
import re

SVSHI_HOME = os.environ["SVSHI_HOME"]
SVSHI_FOLDER = f"{SVSHI_HOME}/svshi"

APP_LIBRARY_DIR = f"{SVSHI_FOLDER}/app_library"
CONDITIONS_FILE_PATH = f"{SVSHI_FOLDER}/runtime/conditions.py"
VERIFICATION_FILE_PATH = f"{SVSHI_FOLDER}/runtime/verification_file.py"
RUNTIME_FILE_PATH = f"{SVSHI_FOLDER}/runtime/runtime_file.py"
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
        raise ValueError(
            f"Wrong IP address '{address}': it has to be a valid IPv4 address"
        )

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
    generator.reset_runtime_file()
    generator.reset_conditions_file()
    print("bye!")


async def main():
    knx_address, knx_port = parse_args()
    conditions_generator = ConditionsGenerator(
        CONDITIONS_FILE_PATH, VERIFICATION_FILE_PATH, RUNTIME_FILE_PATH
    )
    try:
        print("Initializing state and listeners... ", end="")
        apps = get_apps(APP_LIBRARY_DIR, "runtime_file")
        addresses_listeners = get_addresses_listeners(apps)
        [app.install_requirements() for app in apps]

        connection_config = ConnectionConfig(
            connection_type=ConnectionType.TUNNELING,
            gateway_ip=knx_address,
            gateway_port=knx_port,
        )
        xknx_for_initialization = XKNX(connection_config=connection_config)
        xknx_for_listening = XKNX(daemon_mode=True, connection_config=connection_config)
        state = State(
            addresses_listeners,
            xknx_for_initialization,
            xknx_for_listening,
            check_conditions,
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
