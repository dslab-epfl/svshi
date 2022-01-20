import os
import sys
from contextlib import contextmanager
from io import StringIO
from ..main import main

TESTS_DIRECTORY = "tests"

FAKE_GENERATED_PATH = f"{TESTS_DIRECTORY}/fake_generated"
FAKE_APP_LIBRARY_PATH = f"{TESTS_DIRECTORY}/fake_app_library"

VERIFICATION_FILE_PATH = f"{TESTS_DIRECTORY}/verification_file.py"
RUNTIME_FILE_PATH = f"{TESTS_DIRECTORY}/runtime_file.py"
CONDITIONS_FILE_PATH = f"{TESTS_DIRECTORY}/conditions.py"


@contextmanager
def captured_output():
    new_out, new_err = StringIO(), StringIO()
    old_out, old_err = sys.stdout, sys.stderr
    try:
        sys.stdout, sys.stderr = new_out, new_err
        yield sys.stdout, sys.stderr
    finally:
        sys.stdout, sys.stderr = old_out, old_err


def test_main_generates_files():
    with captured_output() as (out, _):
        main(
            FAKE_GENERATED_PATH,
            FAKE_APP_LIBRARY_PATH,
            TESTS_DIRECTORY,
            "",
        )

    assert os.path.exists(CONDITIONS_FILE_PATH) == True
    assert os.path.exists(VERIFICATION_FILE_PATH) == True
    assert os.path.exists(RUNTIME_FILE_PATH) == True
    assert out.getvalue().strip() == VERIFICATION_FILE_PATH

    # Cleanup
    os.remove(CONDITIONS_FILE_PATH)
    os.remove(VERIFICATION_FILE_PATH)
    os.remove(RUNTIME_FILE_PATH)
