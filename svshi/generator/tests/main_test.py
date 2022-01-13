import os
import shutil
import pytest
from ..main import main, parse_args

APP_NAME = "app"
TESTS_DIRECTORY = "tests"
GENERATED_APP_DIRECTORY = f"{TESTS_DIRECTORY}/{APP_NAME}"
DEVICES_FILE = f"{TESTS_DIRECTORY}/devices/devices.json"


def test_parse_args():
    expected_app_name = APP_NAME
    expected_devices_json = "devices.json"
    app_name, devices_json = parse_args([expected_app_name, expected_devices_json])
    assert app_name == expected_app_name
    assert devices_json == expected_devices_json


def test_parse_args_raises_exception_on_wrong_app_name():
    with pytest.raises(ValueError):
        parse_args(["a Namev3ry wrong", "devices.json"])


def test_main():
    """
    More in depth tests are performed by the end-to-end tests in `core` scala project
    """
    main(APP_NAME, DEVICES_FILE, TESTS_DIRECTORY, "skeleton")

    assert os.path.exists(f"{GENERATED_APP_DIRECTORY}/models") == True
    assert os.path.exists(f"{GENERATED_APP_DIRECTORY}/models/__init__.py") == True
    assert os.path.exists(f"{GENERATED_APP_DIRECTORY}/models/binary.py") == True
    assert os.path.exists(f"{GENERATED_APP_DIRECTORY}/models/device.py") == True
    assert os.path.exists(f"{GENERATED_APP_DIRECTORY}/models/humidity.py") == True
    assert os.path.exists(f"{GENERATED_APP_DIRECTORY}/models/switch.py") == True
    assert os.path.exists(f"{GENERATED_APP_DIRECTORY}/models/state.py") == True
    assert os.path.exists(f"{GENERATED_APP_DIRECTORY}/models/temperature.py") == True
    assert os.path.exists(f"{GENERATED_APP_DIRECTORY}/models/multiton.py") == True
    assert os.path.exists(f"{GENERATED_APP_DIRECTORY}/__init__.py") == True
    assert os.path.exists(f"{GENERATED_APP_DIRECTORY}/main.py") == True
    assert os.path.exists(f"{GENERATED_APP_DIRECTORY}/instances.py") == True
    assert os.path.exists(f"{GENERATED_APP_DIRECTORY}/requirements.txt") == True
    assert (
        os.path.exists(f"{GENERATED_APP_DIRECTORY}/app_prototypical_structure.json")
        == True
    )

    # Cleanup
    shutil.move(
        f"{GENERATED_APP_DIRECTORY}/app_prototypical_structure.json",
        DEVICES_FILE,
    )
    shutil.rmtree(GENERATED_APP_DIRECTORY)
