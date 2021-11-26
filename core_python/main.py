from core_python.generator import Generator
from core_python.parser import Parser

if __name__ == "__main__":
    parser = Parser("ga.json")
    group_addresses_with_types = parser.parse_group_addresses()
    generator = Generator("verification_file.py", group_addresses_with_types)
    generator.generate_verification_file()
