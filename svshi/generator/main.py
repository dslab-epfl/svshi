import argparse
import re
import os
from generator.parsing.parser import Parser
from generator.generation.generator import Generator

SVSHI_HOME = os.environ["SVSHI_HOME"]

DEVICES_JSON_FILENAME = f"{SVSHI_HOME}/input/app_prototypical_structure.json"
GENERATED_APPS_FOLDER_NAME = f"{SVSHI_HOME}/generated"


def parse_args():
    """
    Prepares the argument parser, parses the provided arguments and returns the retrieved information as a tuple.
    """
    parser = argparse.ArgumentParser(description="App generator.")
    parser.add_argument("app_name", type=str, help="the name of the app")
    args = parser.parse_args()

    name_regex = re.compile(r"^_*[a-z]+[a-z_]*_*$")
    app_name = args.app_name
    if not name_regex.match(app_name):
        raise ValueError(
            f"Wrong app name '{app_name}': it has to contain only lowercase letters and underscores"
        )

    return app_name


if __name__ == "__main__":
    app_name = parse_args()
    print("Welcome to the app generator!")

    print(f"Parsing the JSON file '{DEVICES_JSON_FILENAME}'...")
    parser = Parser(DEVICES_JSON_FILENAME)
    devices = parser.read_devices()

    print(f"Generating the app '{app_name}'...")
    generator = Generator(
        f"{GENERATED_APPS_FOLDER_NAME}/{app_name}", devices, DEVICES_JSON_FILENAME
    )
    generator.generate_device_instances()
    generator.generate_init_files()
    generator.copy_skeleton_to_generated_app(f"{SVSHI_HOME}/svshi/generator/skeleton")
    generator.generate_multiton_class()
    generator.move_devices_json_to_generated_app()
    generator.add_device_instances_imports_to_main()

    print("Done!")
