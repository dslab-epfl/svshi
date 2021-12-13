import os

from verification.generator import Generator
from verification.parser import Parser

SVSHI_HOME = os.environ["SVSHI_HOME"]

if __name__ == "__main__":
    parser = Parser(f"{SVSHI_HOME}/generated", f"{SVSHI_HOME}/svshi/app_library")
    group_addresses_with_types = parser.parse_group_addresses()
    devices_instances = parser.parse_devices_instances()
    devices_classes = parser.parse_devices_classes()

    output_file_name = f"{SVSHI_HOME}/svshi/verification/verification_file.py"
    generator = Generator(
        output_file_name, group_addresses_with_types, devices_instances, devices_classes
    )
    generator.generate_verification_file()
    print(output_file_name)
