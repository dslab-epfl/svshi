import filecmp
import os
from ..generator import Generator
from ..parser import Parser

TESTS_DIRECTORY = "tests"
VERIFICATION_FILE_PATH = f"{TESTS_DIRECTORY}/verification_file.py"
EXPECTED_VERIFICATION_FILE_PATH = f"{TESTS_DIRECTORY}/expected_verification_file.py"
RUNTIME_FILE_PATH = f"{TESTS_DIRECTORY}/runtime_file.py"
EXPECTED_RUNTIME_FILE_PATH = f"{TESTS_DIRECTORY}/expected_runtime_file.py"
CONDITIONS_FILE_PATH = f"{TESTS_DIRECTORY}/conditions.py"
EXPECTED_CONDITIONS_FILE_PATH = f"{TESTS_DIRECTORY}/expected_conditions.py"

parser = Parser(
    f"{TESTS_DIRECTORY}/fake_generated", f"{TESTS_DIRECTORY}/fake_app_library"
)
group_addresses_with_types = parser.parse_group_addresses()
devices_instances = parser.parse_devices_instances()
devices_classes = parser.parse_devices_classes()
app_names = parser.get_app_names()
filenames = parser.parse_filenames()
app_priorities = parser.get_app_priorities()


generator = Generator(
    VERIFICATION_FILE_PATH,
    RUNTIME_FILE_PATH,
    CONDITIONS_FILE_PATH,
    "",
    group_addresses_with_types,
    devices_instances,
    devices_classes,
    app_names,
    filenames,
)


def test_generator_generate_conditions_file():
    generator.generate_conditions_file()

    assert os.path.exists(CONDITIONS_FILE_PATH) == True
    assert (
        filecmp.cmp(
            CONDITIONS_FILE_PATH,
            EXPECTED_CONDITIONS_FILE_PATH,
            shallow=False,
        )
        == True
    )

    # Cleanup
    os.remove(CONDITIONS_FILE_PATH)


def test_generator_generate_verification_file():
    generator.generate_verification_file(app_priorities)

    with open(VERIFICATION_FILE_PATH, "r") as v:
        print(v.read())

    assert (
        filecmp.cmp(
            VERIFICATION_FILE_PATH,
            EXPECTED_VERIFICATION_FILE_PATH,
            shallow=False,
        )
        == True
    )

    # Cleanup
    os.remove(VERIFICATION_FILE_PATH)


def test_generator_generate_runtime_file():
    generator.generate_runtime_file(app_priorities)

    with open(RUNTIME_FILE_PATH, "r") as v:
        print(v.read())

    assert (
        filecmp.cmp(
            RUNTIME_FILE_PATH,
            EXPECTED_RUNTIME_FILE_PATH,
            shallow=False,
        )
        == True
    )

    # Cleanup
    os.remove(RUNTIME_FILE_PATH)
