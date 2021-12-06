import os
import filecmp
import pytest
from ..generator import ConditionsGenerator

CONDITIONS_FILE = "tests/conditions.py"
EXPECTED_CONDITIONS_FILE = "tests/expected_conditions.py"
EXPECTED_DEFAULT_CONDITIONS_FILE = "tests/expected_default_conditions.py"

generator = ConditionsGenerator("tests/fake_app_library", "tests/conditions.py")


@pytest.fixture(autouse=True)
def run_before_and_after_tests():
    """Fixture to execute setup and cleanup"""
    yield  # this is where the testing happens

    # Cleanup
    os.remove("tests/conditions.py")


def test_generator_generate_conditions_file():
    generator.generate_conditions_file()

    assert os.path.exists(CONDITIONS_FILE) == True
    assert (
        filecmp.cmp(
            CONDITIONS_FILE,
            EXPECTED_CONDITIONS_FILE,
            shallow=False,
        )
        == True
    )


def test_generator_reset_conditions_file():
    generator.generate_conditions_file()
    generator.reset_conditions_file()

    assert os.path.exists(CONDITIONS_FILE) == True
    assert (
        filecmp.cmp(
            CONDITIONS_FILE,
            EXPECTED_DEFAULT_CONDITIONS_FILE,
            shallow=False,
        )
        == True
    )
