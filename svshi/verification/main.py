import os
from typing import Final

from verification.generator import Generator
from verification.parser import Parser

SVSHI_HOME: Final = os.environ["SVSHI_HOME"]
APP_LIBRARY: Final = f"{SVSHI_HOME}/svshi/app_library"
VERIFICATION_MODULE_PATH = f"{SVSHI_HOME}/svshi/verification"

if __name__ == "__main__":
    parser = Parser(f"{SVSHI_HOME}/generated", APP_LIBRARY)
    group_addresses_with_types = parser.parse_group_addresses()
    devices_instances = parser.parse_devices_instances()
    devices_classes = parser.parse_devices_classes()
    app_names = parser.get_app_names()
    filenames = parser.parse_filenames()

    verification_filename = f"{VERIFICATION_MODULE_PATH}/verification_file.py"
    runtime_filename = f"{VERIFICATION_MODULE_PATH}/runtime_file.py"
    conditions_filename = f"{VERIFICATION_MODULE_PATH}/conditions.py"
    generator = Generator(
        verification_filename,
        runtime_filename,
        conditions_filename,
        group_addresses_with_types,
        devices_instances,
        devices_classes,
        app_names,
        filenames,
    )
    generator.generate_verification_file()
    generator.generate_runtime_file()
    generator.generate_conditions_file()
    print(verification_filename)
