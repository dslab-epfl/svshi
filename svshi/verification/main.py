import os
from typing import Final

from verification.generator import Generator
from verification.parser import Parser

SVSHI_HOME: Final = os.environ["SVSHI_HOME"]
APP_LIBRARY: Final = f"{SVSHI_HOME}/svshi/app_library"

if __name__ == "__main__":
    parser = Parser(f"{SVSHI_HOME}/generated", APP_LIBRARY)
    group_addresses_with_types = parser.parse_group_addresses()
    devices_instances = parser.parse_devices_instances()
    devices_classes = parser.parse_devices_classes()
    app_names = parser.get_app_names()

    verification_filename = f"{SVSHI_HOME}/svshi/verification/verification_file.py"
    runtime_filename = f"{SVSHI_HOME}/svshi/verification/runtime_file.py"
    conditions_filename = f"{SVSHI_HOME}/svshi/verification/conditions.py"
    generator = Generator(
        verification_filename,
        runtime_filename,
        conditions_filename,
        group_addresses_with_types,
        devices_instances,
        devices_classes,
        app_names,
    )
    generator.generate_verification_file()
    generator.generate_runtime_file()
    generator.generate_conditions_file()
    print(verification_filename)
