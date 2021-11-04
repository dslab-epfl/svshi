import argparse
import subprocess
from generator.parsing.parser import read_device_instances, read_device_types
from generator.generation.generator import (
    generate_device_instances,
    generate_device_class,
    generate_multiton_class,
    generate_init_files,
)


def parse_args():
    """
    Prepares the argument parser, parses the provided arguments and returns the retrieved information as a tuple.
    """
    parser = argparse.ArgumentParser(description="App generator.")
    parser.add_argument(
        "devices_json", type=str, help="the name of the devices JSON file"
    )
    parser.add_argument("app_name", type=str, help="the name of the app")
    args = parser.parse_args()
    return args.devices_json, args.app_name


if __name__ == "__main__":
    devices_json, app_name = parse_args()
    device_types = read_device_types(devices_json)
    device_types_map = {dt.type: dt for dt in device_types}
    device_instances = read_device_instances(devices_json, device_types_map)

    generate_multiton_class(app_name)

    for dt in device_types:
        generate_device_class(dt, app_name)

    generate_device_instances(device_instances, app_name)

    generate_init_files(app_name)

    # Copy the skeleton to the newly generated app
    subprocess.run(f"cp -r generator/skeleton/* {app_name}", shell=True)
