import filecmp
import os
from ..generator import Generator
from ..parser import Parser

TESTS_DIRECTORY = "tests"


def test_generator_generate_verification_file():
    parser = Parser(
        f"{TESTS_DIRECTORY}/fake_generated", f"{TESTS_DIRECTORY}/fake_app_library"
    )
    group_addresses_with_types = parser.parse_group_addresses()
    devices_instances = parser.parse_devices_instances()
    devices_classes = parser.parse_devices_classes()

    verification_file_path = f"{TESTS_DIRECTORY}/verification_file.py"
    expected_verification_file_path = f"{TESTS_DIRECTORY}/expected_verification_file.py"

    generator = Generator(
        verification_file_path,
        group_addresses_with_types,
        devices_instances,
        devices_classes,
    )
    generator.generate_verification_file()

    assert (
        filecmp.cmp(
            verification_file_path,
            expected_verification_file_path,
            shallow=False,
        )
        == True
    )

    # Cleanup
    os.remove(verification_file_path)
