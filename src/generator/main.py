import argparse
import re
import os
import sys
from typing import Tuple, Final
from .parsing.parser import Parser
from .generation.generator import Generator

SVSHI_HOME: Final = os.environ["SVSHI_HOME"]
GENERATED_APPS_FOLDER_NAME: Final = f"{SVSHI_HOME}/generated"
SVSHI_SRC_FOLDER = f"{SVSHI_HOME}/src"
SKELETON_PATH: Final = f"{SVSHI_SRC_FOLDER}/generator/skeleton"


def parse_args(args) -> Tuple[str, str]:
    """
    Prepares the argument parser, parses the provided arguments and returns the retrieved information as a tuple.
    """
    parser = argparse.ArgumentParser(description="App generator.")
    parser.add_argument("app_name", type=str, help="the name of the app")
    parser.add_argument(
        "devices_json",
        type=str,
        help="the devices prototypical structure JSON file of the app",
    )
    args = parser.parse_args(args)

    name_regex = re.compile(r"^_*[a-z]+[a-z_]*_*$")
    app_name = args.app_name
    if not name_regex.match(app_name):
        raise ValueError(
            f"Wrong app name '{app_name}': it has to contain only lowercase letters and underscores"
        )

    return app_name, args.devices_json


def main(
    app_name: str, devices_json: str, generated_folder_name: str, skeleton_path: str
):
    print("Welcome to the app generator!")

    print(f"Parsing the JSON file '{devices_json}'...")
    parser = Parser(devices_json)
    devices = parser.read_devices()

    print(f"Generating the app '{app_name}'...")
    generator = Generator(f"{generated_folder_name}/{app_name}", devices, devices_json)
    generator.generate_instances()
    generator.generate_init_files()
    generator.copy_skeleton_to_generated_app(skeleton_path)
    generator.generate_multiton_class()
    generator.move_devices_json_to_generated_app()
    generator.add_instances_imports_to_main()

    print("Done!")


if __name__ == "__main__":
    app_name, devices_json = parse_args(sys.argv[1:])
    main(app_name, devices_json, GENERATED_APPS_FOLDER_NAME, SKELETON_PATH)
