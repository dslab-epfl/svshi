import os
import filecmp
import pytest
from ..resetter import FileResetter

CONDITIONS_FILE = "tests/conditions.py"
VERIFICATION_FILE = "tests/verification_file.py"
RUNTIME_FILE = "tests/runtime_file.py"
EXPECTED_DEFAULT_CONDITIONS_FILE = "tests/expected/expected_default_conditions.py"
EXPECTED_DEFAULT_VERIFICATION_FILE = (
    "tests/expected/expected_default_verification_file.py"
)
EXPECTED_DEFAULT_RUNTIME_FILE = "tests/expected/expected_default_runtime_file.py"

resetter = FileResetter(CONDITIONS_FILE, VERIFICATION_FILE, RUNTIME_FILE)


@pytest.fixture(autouse=True)
def run_before_and_after_tests():
    """Fixture to execute setup and cleanup"""
    yield  # this is where the testing happens

    # Cleanup
    if os.path.exists(CONDITIONS_FILE):
        os.remove(CONDITIONS_FILE)

    if os.path.exists(VERIFICATION_FILE):
        os.remove(VERIFICATION_FILE)

    if os.path.exists(RUNTIME_FILE):
        os.remove(RUNTIME_FILE)


def test_resetter_reset_conditions_file():
    resetter.reset_conditions_file()

    assert os.path.exists(CONDITIONS_FILE) == True
    assert (
        filecmp.cmp(
            CONDITIONS_FILE,
            EXPECTED_DEFAULT_CONDITIONS_FILE,
            shallow=False,
        )
        == True
    )


def test_resetter_reset_verification_file():
    resetter.reset_verification_file()

    assert os.path.exists(VERIFICATION_FILE) == True
    assert (
        filecmp.cmp(
            VERIFICATION_FILE,
            EXPECTED_DEFAULT_VERIFICATION_FILE,
            shallow=False,
        )
        == True
    )


def test_resetter_reset_runtime_file():
    resetter.reset_runtime_file()

    assert os.path.exists(RUNTIME_FILE) == True
    assert (
        filecmp.cmp(
            RUNTIME_FILE,
            EXPECTED_DEFAULT_RUNTIME_FILE,
            shallow=False,
        )
        == True
    )
