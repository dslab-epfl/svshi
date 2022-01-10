from typing import Tuple
from xknx.io.connection import ConnectionConfig, ConnectionType
from xknx.xknx import XKNX
from .app import get_addresses_listeners, get_apps
from .resetter import FileResetter
from .state import State
from .conditions import check_conditions
from .parser import GroupAddressesParser
import argparse
import asyncio
import os
import re
import sys

SVSHI_HOME = os.environ["SVSHI_HOME"]
SVSHI_FOLDER = f"{SVSHI_HOME}/svshi"

APP_LIBRARY_DIR = f"{SVSHI_FOLDER}/app_library"
GROUP_ADDRESSES_PATH = f"{APP_LIBRARY_DIR}/group_addresses.json"
CONDITIONS_FILE_PATH = f"{SVSHI_FOLDER}/runtime/conditions.py"
VERIFICATION_FILE_PATH = f"{SVSHI_FOLDER}/runtime/verification_file.py"
RUNTIME_FILE_PATH = f"{SVSHI_FOLDER}/runtime/runtime_file.py"
RUNTIME_FILE_MODULE = "runtime.runtime_file"


def parse_args(args) -> Tuple[str, int]:
    """
    Prepares the argument parser, parses the provided arguments and returns the retrieved information as a tuple.
    """
    parser = argparse.ArgumentParser(description="Runtime module.")
    parser.add_argument("ip_address", type=str, help="the KNX IP address to use")
    parser.add_argument("port", type=int, help="the KNX port to use", default=3671)
    args = parser.parse_args(args)

    address_regex = re.compile(r"^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$")
    address = args.ip_address
    if not address_regex.match(address):
        raise ValueError(
            f"Wrong IP address '{address}': it has to be a valid IPv4 address"
        )

    port = int(args.port)
    if port <= 0:
        raise ValueError(f"Wrong port '{port}': it has to be a valid port")

    return address, port


async def cleanup(file_resetter: FileResetter, error: bool = False):
    """
    Resets the verification and the conditions files.
    """
    error_message = "An error occurred!\n" if error else ""
    print(f"{error_message}Exiting... ", end="")
    file_resetter.reset_verification_file()
    file_resetter.reset_runtime_file()
    file_resetter.reset_conditions_file()
    print("bye!")


async def main(
    knx_address: str,
    knx_port: int,
    conditions_file_path: str,
    verification_file_path: str,
    runtime_file_path: str,
    app_library_path: str,
    group_addresses_path: str,
    runtime_file_module: str,
):
    file_resetter = FileResetter(
        conditions_file_path, verification_file_path, runtime_file_path
    )
    try:
        print("Initializing state and listeners... ", end="")
        apps = get_apps(app_library_path, runtime_file_module)
        addresses_listeners = get_addresses_listeners(apps)
        for app in apps:
            app.install_requirements()

        connection_config = ConnectionConfig(
            connection_type=ConnectionType.TUNNELING,
            gateway_ip=knx_address,
            gateway_port=knx_port,
        )
        xknx_for_initialization = XKNX(connection_config=connection_config)
        xknx_for_listening = XKNX(daemon_mode=True, connection_config=connection_config)

        parser = GroupAddressesParser(group_addresses_path)
        group_addresses_dpt = parser.read_group_addresses_dpt()

        state = State(
            addresses_listeners,
            xknx_for_initialization,
            xknx_for_listening,
            check_conditions,
            group_addresses_dpt,
        )
        await state.initialize()
        print("done!")

        print("Connecting to KNX and listening to telegrams...")
        await state.listen()

        print("Disconnecting from KNX... ", end="")
        await state.stop()
        print("done!")

        await cleanup(file_resetter)

    except KeyboardInterrupt:
        await cleanup(file_resetter)
    except BaseException as e:
        await cleanup(file_resetter, error=True)
        raise e


if __name__ == "__main__":
    knx_address, knx_port = parse_args(sys.argv[1:])
    asyncio.run(
        main(
            knx_address,
            knx_port,
            CONDITIONS_FILE_PATH,
            VERIFICATION_FILE_PATH,
            RUNTIME_FILE_PATH,
            APP_LIBRARY_DIR,
            GROUP_ADDRESSES_PATH,
            RUNTIME_FILE_MODULE,
        )
    )
