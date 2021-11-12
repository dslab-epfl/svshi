import argparse
from generator.parsing.parser import Parser
from generator.generation.generator import Generator


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
    print("Welcome to the app generator!")

    print(f"Parsing the JSON file '{devices_json}'...")
    parser = Parser(devices_json)
    devices = parser.read_devices()

    print(f"Generating the app '{app_name}'...")
    generator = Generator(app_name)
    generator.generate_device_instances(devices)
    generator.generate_init_files()
    generator.copy_skeleton_to_generated_app()

    print("Done!")
