import os
import filecmp
import pytest
from ..generator import ConditionsGenerator

CONDITIONS_FILE = "tests/conditions.py"
VERIFICATION_FILE = "tests/verification_file.py"
EXPECTED_CONDITIONS_FILE = "tests/expected/expected_conditions.py"
EXPECTED_DEFAULT_CONDITIONS_FILE = "tests/expected/expected_default_conditions.py"
EXPECTED_VERIFICATION_FILE = "tests/expected/expected_verification_file.py"
EXPECTED_DEFAULT_VERIFICATION_FILE = (
    "tests/expected/expected_default_verification_file.py"
)

generator = ConditionsGenerator(
    "tests/fake_app_library", CONDITIONS_FILE, VERIFICATION_FILE
)


@pytest.fixture(autouse=True)
def run_before_and_after_tests():
    """Fixture to execute setup and cleanup"""
    yield  # this is where the testing happens

    # Cleanup
    if os.path.exists(CONDITIONS_FILE):
        os.remove(CONDITIONS_FILE)

    if os.path.exists(VERIFICATION_FILE):
        os.remove(VERIFICATION_FILE)


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


def test_generator_reset_verification_file():
    generator.reset_verification_file()

    assert os.path.exists(VERIFICATION_FILE) == True
    assert (
        filecmp.cmp(
            VERIFICATION_FILE,
            EXPECTED_VERIFICATION_FILE,
            shallow=False,
        )
        == True
    )
