from core_python.generator import Generator
from core_python.parser import Parser

if __name__ == "__main__":
    parser = Parser("generated/group_addresses.json")
    group_addresses_with_types = parser.parse_group_addresses()
    devices_instances = parser.parse_devices_instances()
    devices_classes = parser.parse_devices_classes()
    generator = Generator("verification_file.py", group_addresses_with_types, devices_instances, devices_classes)
    generator.generate_verification_file()
