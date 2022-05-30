from typing import Tuple
from datetime import datetime
from xknx.io.connection import ConnectionConfig, ConnectionType
from xknx.xknx import XKNX
from .app import get_addresses_listeners, get_apps
from .joint_apps import get_joint_apps
from .resetter import FileResetter
from .state import State
from .conditions import check_conditions
from .parser import GroupAddressesParser
import argparse
import asyncio
import os
import re
import sys
import logging

logging.basicConfig(level=logging.ERROR)

SVSHI_HOME = os.environ["SVSHI_HOME"].replace("\\", "/")
SVSHI_SRC_FOLDER = f"{SVSHI_HOME}/src"

APP_LIBRARY_DIR = f"{SVSHI_SRC_FOLDER}/app_library"
GROUP_ADDRESSES_PATH = f"{APP_LIBRARY_DIR}/group_addresses.json"
CONDITIONS_FILE_PATH = f"{SVSHI_SRC_FOLDER}/runtime/conditions.py"
VERIFICATION_FILE_PATH = f"{SVSHI_SRC_FOLDER}/runtime/verification_file.py"
RUNTIME_FILE_PATH = f"{SVSHI_SRC_FOLDER}/runtime/runtime_file.py"
RUNTIME_FILE_MODULE = "runtime.runtime_file"
LOGS_DIR_NAME = str(datetime.now()).replace(" ", "__").replace(":", "_")
LOGS_DIR = f"{SVSHI_HOME}/logs/{LOGS_DIR_NAME}"


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
    print(f"{error_message}Exiting... ", end="", flush=True)
    file_resetter.reset_verification_file()
    file_resetter.reset_runtime_file()
    file_resetter.reset_conditions_file()
    print("bye!", flush=True)


async def main(
    knx_address: str,
    knx_port: int,
    conditions_file_path: str,
    verification_file_path: str,
    runtime_file_path: str,
    app_library_path: str,
    group_addresses_path: str,
    runtime_file_module: str,
    logs_dir: str,
):
    file_resetter = FileResetter(
        conditions_file_path, verification_file_path, runtime_file_path
    )
    try:
        print("Initializing listeners...", flush=True)
        apps = get_apps(app_library_path, runtime_file_module)
        addresses_listeners = get_addresses_listeners(apps)

        connection_config = ConnectionConfig(
            route_back=True,  # To enable connection through the docker
            connection_type=ConnectionType.TUNNELING,
            gateway_ip=knx_address,
            gateway_port=knx_port,
        )
        xknx_for_initialization = XKNX(connection_config=connection_config)
        xknx_for_listening = XKNX(daemon_mode=True, connection_config=connection_config)

        parser = GroupAddressesParser(group_addresses_path)
        group_addresses_dpt = parser.read_group_addresses_dpt()
        joint_apps = get_joint_apps(runtime_file_module)

        state = State(
            addresses_listeners,
            joint_apps,
            xknx_for_initialization,
            xknx_for_listening,
            check_conditions,
            group_addresses_dpt,
            logs_dir,
        )
        print("Initializing state...", flush=True)
        await state.initialize()

        print("Connecting to KNX and listening to telegrams...", flush=True)
        await state.listen()

        print("Disconnecting from KNX... ", end="", flush=True)
        await state.stop()
        print("done!", flush=True)

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
            LOGS_DIR,
        )
    )
