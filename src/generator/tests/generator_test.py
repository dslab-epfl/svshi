import os
from os.path import exists
import shutil
import filecmp
import pytest
from ..generation.generator import Generator
from ..parsing.device import Device

TESTS_DIRECTORY = "tests"
GENERATED_APP_DIRECTORY = f"{TESTS_DIRECTORY}/test"
EXPECTED_FILES_DIRECTORY = f"{TESTS_DIRECTORY}/expected_files"
DEVICES_FILE = f"{TESTS_DIRECTORY}/devices/devices.json"
DEVICES_BACKUP_FILE = f"{TESTS_DIRECTORY}/devices/devices_backup.json"

generator = Generator(
    GENERATED_APP_DIRECTORY,
    [
        Device("binary_sensor_instance_name", "BinarySensor", "binarySensor"),
        Device("switch_instance_name", "Switch", "switch"),
        Device("temperature_sensor_instance_name", "TemperatureSensor", "temperatureSensor"),
        Device("humidity_sensor_instance_name", "HumiditySensor", "humiditySensor"),
        Device("co_two_sensor_instance_name", "CO2Sensor", "co2Sensor"),
    ],
    DEVICES_FILE,
)


@pytest.fixture(autouse=True)
def run_before_and_after_tests():
    """Fixture to execute setup and cleanup"""
    # Setup
    if exists(GENERATED_APP_DIRECTORY):
        shutil.rmtree(GENERATED_APP_DIRECTORY)
    os.mkdir(GENERATED_APP_DIRECTORY)
    if exists(DEVICES_BACKUP_FILE):
        os.remove(DEVICES_BACKUP_FILE)
    shutil.copyfile(DEVICES_FILE, DEVICES_BACKUP_FILE)

    yield  # this is where the testing happens

    # Cleanup
    shutil.rmtree(GENERATED_APP_DIRECTORY)
    if exists(DEVICES_FILE):
        os.remove(DEVICES_FILE)
    shutil.copyfile(DEVICES_BACKUP_FILE, DEVICES_FILE)
    if exists(DEVICES_BACKUP_FILE):
        os.remove(DEVICES_BACKUP_FILE)


def test_generator_move_devices_json_to_generated_app():
    generator.move_devices_json_to_generated_app()

    assert (
        os.path.exists(f"{GENERATED_APP_DIRECTORY}/app_prototypical_structure.json")
        == True
    )
    assert os.path.exists(DEVICES_FILE) == False

    # More cleanup
    shutil.move(
        f"{GENERATED_APP_DIRECTORY}/app_prototypical_structure.json",
        DEVICES_FILE,
    )


def test_generator_copy_skeleton_to_generated_app():
    generator.copy_skeleton_to_generated_app("skeleton")

    assert os.path.exists(f"{GENERATED_APP_DIRECTORY}/models") == True
    assert os.path.exists(f"{GENERATED_APP_DIRECTORY}/models/__init__.py") == True
    assert os.path.exists(f"{GENERATED_APP_DIRECTORY}/models/binarySensor.py") == True
    assert os.path.exists(f"{GENERATED_APP_DIRECTORY}/models/device.py") == True
    assert os.path.exists(f"{GENERATED_APP_DIRECTORY}/models/humiditySensor.py") == True
    assert os.path.exists(f"{GENERATED_APP_DIRECTORY}/models/co2Sensor.py") == True
    assert os.path.exists(f"{GENERATED_APP_DIRECTORY}/models/switch.py") == True
    assert os.path.exists(f"{GENERATED_APP_DIRECTORY}/models/state.py") == True
    assert os.path.exists(f"{GENERATED_APP_DIRECTORY}/models/temperatureSensor.py") == True
    assert os.path.exists(f"{GENERATED_APP_DIRECTORY}/models/dimmerSensor.py") == True
    assert os.path.exists(f"{GENERATED_APP_DIRECTORY}/models/dimmerActuator.py") == True
    assert os.path.exists(f"{GENERATED_APP_DIRECTORY}/models/SvshiApi.py") == True
    assert os.path.exists(f"{GENERATED_APP_DIRECTORY}/__init__.py") == True
    assert os.path.exists(f"{GENERATED_APP_DIRECTORY}/main.py") == True
    assert os.path.exists(f"{GENERATED_APP_DIRECTORY}/requirements.txt") == True
    assert os.path.exists(f"{GENERATED_APP_DIRECTORY}/files") == True

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


def test_generator_generate_instances():
    generator.generate_instances()

    assert os.path.exists(f"{GENERATED_APP_DIRECTORY}/instances.py") == True
    assert (
        filecmp.cmp(
            f"{GENERATED_APP_DIRECTORY}/instances.py",
            f"{EXPECTED_FILES_DIRECTORY}/expected_instances.py",
            shallow=False,
        )
        == True
    )


def test_generator_add_instances_imports_to_main():
    # Create a main.py for the test
    open(f"{GENERATED_APP_DIRECTORY}/main.py", "a").close()

    generator.add_instances_imports_to_main()

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
