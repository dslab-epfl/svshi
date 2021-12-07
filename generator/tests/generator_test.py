import os
import shutil
import subprocess
import filecmp
import pytest
from ..generation.generator import Generator
from ..parsing.device import Device

TESTS_DIRECTORY = "tests"
GENERATED_APP_DIRECTORY = "tests/test"
EXPECTED_FILES_DIRECTORY = "tests/expected_files"

generator = Generator(
    GENERATED_APP_DIRECTORY,
    [
        Device("binary_sensor_instance_name", "BinarySensor", "binary"),
        Device("switch_instance_name", "Switch", "switch"),
        Device("temperature_sensor_instance_name", "TemperatureSensor", "temperature"),
        Device("humidity_sensor_instance_name", "HumiditySensor", "humidity"),
    ],
    f"{TESTS_DIRECTORY}/devices.json",
)


@pytest.fixture(autouse=True)
def run_before_and_after_tests():
    """Fixture to execute setup and cleanup"""
    # Setup
    os.mkdir(GENERATED_APP_DIRECTORY)

    yield  # this is where the testing happens

    # Cleanup
    shutil.rmtree(GENERATED_APP_DIRECTORY)


def test_generator_move_devices_json_to_generated_app():
    generator.move_devices_json_to_generated_app()

    assert os.path.exists(f"{GENERATED_APP_DIRECTORY}/devices.json") == True
    assert os.path.exists("tests/devices.json") == False

    # More cleanup
    subprocess.run(
        f"mv {GENERATED_APP_DIRECTORY}/devices.json {TESTS_DIRECTORY}", shell=True
    )


def test_generator_copy_skeleton_to_generated_app():
    generator.copy_skeleton_to_generated_app("skeleton")

    assert os.path.exists(f"{GENERATED_APP_DIRECTORY}/models") == True
    assert os.path.exists(f"{GENERATED_APP_DIRECTORY}/models/__init__.py") == True
    assert os.path.exists(f"{GENERATED_APP_DIRECTORY}/models/binary.py") == True
    assert os.path.exists(f"{GENERATED_APP_DIRECTORY}/models/device.py") == True
    assert os.path.exists(f"{GENERATED_APP_DIRECTORY}/models/humidity.py") == True
    assert os.path.exists(f"{GENERATED_APP_DIRECTORY}/models/switch.py") == True
    assert os.path.exists(f"{GENERATED_APP_DIRECTORY}/models/temperature.py") == True
    assert os.path.exists(f"{GENERATED_APP_DIRECTORY}/__init__.py") == True
    assert os.path.exists(f"{GENERATED_APP_DIRECTORY}/main.py") == True
    assert os.path.exists(f"{GENERATED_APP_DIRECTORY}/requirements.txt") == True


def test_generator_generate_multiton_class():
    generator.generate_multiton_class()

    assert os.path.exists(f"{GENERATED_APP_DIRECTORY}/models") == True
    assert os.path.exists(f"{GENERATED_APP_DIRECTORY}/models/multiton.py") == True
    assert (
        filecmp.cmp(
            f"{GENERATED_APP_DIRECTORY}/models/multiton.py",
            f"{EXPECTED_FILES_DIRECTORY}/expected_multiton.py",
            shallow=False,
        )
        == True
    )


def test_generator_generate_device_instances():
    generator.generate_device_instances()

    assert os.path.exists(f"{GENERATED_APP_DIRECTORY}/devices.py") == True
    assert (
        filecmp.cmp(
            f"{GENERATED_APP_DIRECTORY}/devices.py",
            f"{EXPECTED_FILES_DIRECTORY}/expected_devices.py",
            shallow=False,
        )
        == True
    )


def test_generator_add_device_instances_imports_to_main():
    # Create a main.py for the test
    open(f"{GENERATED_APP_DIRECTORY}/main.py", "a").close()

    generator.add_device_instances_imports_to_main()

    assert (
        filecmp.cmp(
            f"{GENERATED_APP_DIRECTORY}/main.py",
            f"{EXPECTED_FILES_DIRECTORY}/expected_main.py",
            shallow=False,
        )
        == True
    )


def test_generator_generate_init_files():
    os.mkdir(f"{GENERATED_APP_DIRECTORY}/models")

    generator.generate_init_files()

    assert os.path.exists(f"{GENERATED_APP_DIRECTORY}/__init__.py") == True
    assert os.path.exists(f"{GENERATED_APP_DIRECTORY}/models/__init__.py") == True
