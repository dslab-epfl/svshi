from core_python.generator import Generator
from core_python.parser import Parser

if __name__ == "__main__":
    parser = Parser()
    group_addresses_with_types = parser.parse_group_addresses()
    devices_instances = parser.parse_devices_instances()
    devices_classes = parser.parse_devices_classes()
    
    output_file_name = "verification_file.py"
    generator = Generator(output_file_name, group_addresses_with_types, devices_instances, devices_classes)
    generator.generate_verification_file()
