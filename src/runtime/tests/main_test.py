import asyncio
import os
import sys
import pytest
from contextlib import contextmanager
from io import StringIO
from pytest_mock import MockerFixture
from xknx.core.value_reader import ValueReader
from xknx import XKNX

from ..resetter import FileResetter
from ..main import cleanup, main, parse_args

LOGS_DIR = "tests/logs"
APP_LIBRARY_DIR = "tests/fake_app_library"
GROUP_ADDRESSES_PATH = "tests/fake_app_library/group_addresses.json"
CONDITIONS_FILE_PATH = "cond"
VERIFICATION_FILE_PATH = "ver"
RUNTIME_FILE_PATH = "run"
ISOLATED_FNS_FILE_PATH = "isol.json"
REAL_ISOLATED_FNS_FILE_PATH = "tests/expected/expected_isolated_fns.json"
SVSHI_HOME = os.environ["SVSHI_HOME"].replace("\\", "/")
RUNTIME_FILE_MODULE = (
    f"runtime.tests.expected.expected_runtime_file"
)


@contextmanager
def captured_output():
    new_out, new_err = StringIO(), StringIO()
    old_out, old_err = sys.stdout, sys.stderr
    try:
        sys.stdout, sys.stderr = new_out, new_err
        yield sys.stdout, sys.stderr
    finally:
        sys.stdout, sys.stderr = old_out, old_err


def test_parse_args_correctly_returns_address_port():
    expected_address = "192.0.0.1"
    expected_port = "42"
    address, port = parse_args([expected_address, expected_port])

    assert address == expected_address
    assert port == int(expected_port)


def test_parse_args_raises_exception_on_wrong_address():
    with pytest.raises(ValueError):
        parse_args(["12345.1.3.0", "42"])


def test_parse_args_raises_exception_on_wrong_port():
    with pytest.raises(ValueError):
        parse_args(["192.123.10.1", "-10"])


def test_parse_args_raises_exception_on_port_zero():
    with pytest.raises(ValueError):
        parse_args(["192.123.10.1", "0"])


@pytest.mark.asyncio
async def test_cleanup_uses_file_resetter(mocker: MockerFixture):
    file_resetter = FileResetter(
        CONDITIONS_FILE_PATH,
        VERIFICATION_FILE_PATH,
        RUNTIME_FILE_PATH,
        ISOLATED_FNS_FILE_PATH,
    )
    reset_verification_file_spy = mocker.spy(file_resetter, "reset_verification_file")
    reset_runtime_file_spy = mocker.spy(file_resetter, "reset_runtime_file")
    reset_conditions_file_spy = mocker.spy(file_resetter, "reset_conditions_file")
    reset_isolated_fns_file_spy = mocker.spy(file_resetter, "reset_isolated_fns_file")

    await cleanup(file_resetter)

    reset_verification_file_spy.assert_called_once()
    reset_runtime_file_spy.assert_called_once()
    reset_conditions_file_spy.assert_called_once()
    reset_isolated_fns_file_spy.assert_called_once()

    # Cleanup
    os.remove(CONDITIONS_FILE_PATH)
    os.remove(VERIFICATION_FILE_PATH)
    os.remove(RUNTIME_FILE_PATH)
    os.remove(ISOLATED_FNS_FILE_PATH)


@pytest.mark.asyncio
async def test_cleanup_uses_file_resetter_and_prints_error_message(
    mocker: MockerFixture,
):
    file_resetter = FileResetter(
        CONDITIONS_FILE_PATH,
        VERIFICATION_FILE_PATH,
        RUNTIME_FILE_PATH,
        ISOLATED_FNS_FILE_PATH,
    )
    reset_verification_file_spy = mocker.spy(file_resetter, "reset_verification_file")
    reset_runtime_file_spy = mocker.spy(file_resetter, "reset_runtime_file")
    reset_conditions_file_spy = mocker.spy(file_resetter, "reset_conditions_file")
    reset_isolated_fns_file_spy = mocker.spy(file_resetter, "reset_isolated_fns_file")

    with captured_output() as (out, _):
        await cleanup(file_resetter, error=True)

    reset_verification_file_spy.assert_called_once()
    reset_runtime_file_spy.assert_called_once()
    reset_conditions_file_spy.assert_called_once()
    reset_isolated_fns_file_spy.assert_called_once()
    assert out.getvalue().strip() == "An error occurred!\nExiting... bye!"

    # Cleanup
    os.remove(CONDITIONS_FILE_PATH)
    os.remove(VERIFICATION_FILE_PATH)
    os.remove(RUNTIME_FILE_PATH)
    os.remove(ISOLATED_FNS_FILE_PATH)


@pytest.mark.asyncio
async def test_main_listens_to_KNX_then_stops_and_resets_files(
    mocker: MockerFixture,
):
    from ..parser import GroupAddressesParser
    from ..state import State

    mocker.patch("xknx.xknx.XKNX", autospec=True)

    parser_spy = mocker.spy(GroupAddressesParser, "read_group_addresses_dpt")

    state_initialize_spy = mocker.patch.object(State, "initialize")
    state_listen_spy = mocker.patch.object(State, "listen")
    state_stop_spy = mocker.patch.object(State, "stop")

    reset_verification_file_spy = mocker.patch.object(
        FileResetter, "reset_verification_file"
    )
    reset_runtime_file_spy = mocker.patch.object(FileResetter, "reset_runtime_file")
    reset_conditions_file_spy = mocker.patch.object(
        FileResetter, "reset_conditions_file"
    )
    reset_isolated_fns_file_spy = mocker.patch.object(
        FileResetter, "reset_isolated_fns_file"
    )

    await main(
        "192.0.0.1",
        8236,
        CONDITIONS_FILE_PATH,
        VERIFICATION_FILE_PATH,
        RUNTIME_FILE_PATH,
        REAL_ISOLATED_FNS_FILE_PATH,
        APP_LIBRARY_DIR,
        GROUP_ADDRESSES_PATH,
        RUNTIME_FILE_MODULE,
        LOGS_DIR,
    )

    parser_spy.assert_called_once()

    state_initialize_spy.assert_called_once()
    state_listen_spy.assert_called_once()
    state_stop_spy.assert_called_once()

    reset_verification_file_spy.assert_called_once()
    reset_runtime_file_spy.assert_called_once()
    reset_conditions_file_spy.assert_called_once()
    reset_isolated_fns_file_spy.assert_called_once()


@pytest.mark.asyncio
async def test_isolated_functions_are_triggered(
    mocker: MockerFixture,
):
    """Test that the periodic and on_trigger functions are correctly executed."""
    from ..state import State

    TARGET_TXT_FILE_1 = "app_tmp.txt"
    TARGET_TXT_FILE_2 = "another_app_tmp.txt"
    TARGET_TEXT_1 = "123\n"
    TARGET_VAL_1 = 123
    TARGET_TEXT_2 = "Hello, world!\n"
    TARGET_VAL_2 = 3.2

    if os.path.exists(TARGET_TXT_FILE_1):
        os.remove(TARGET_TXT_FILE_1)
    if os.path.exists(TARGET_TXT_FILE_2):
        os.remove(TARGET_TXT_FILE_2)

    # Patch some methods to allow executing state.initialize
    mocker.patch.object(XKNX, "start")
    mocker.patch.object(ValueReader, "read", return_value=None)

    # Make the listen function last for 0.6 second to allow the periodic
    # function to run twice.
    async def patched_listen(self: State):
        await asyncio.sleep(0.6)

    mocker.patch.object(State, "listen", new=patched_listen)

    # At the end, ensure that the returned values of both functions are stored in the
    # IsolatedFunctionsValues object.
    async def patched_stop(self: State):
        assert TARGET_VAL_1 == getattr(self._isolated_fn_values, "app_on_trigger_write")
        assert TARGET_VAL_2 == getattr(
            self._isolated_fn_values, "another_app_periodic_write"
        )

    mocker.patch.object(State, "stop", new=patched_stop)

    mocker.patch.object(FileResetter, "reset_verification_file")
    mocker.patch.object(FileResetter, "reset_runtime_file")
    mocker.patch.object(FileResetter, "reset_conditions_file")
    mocker.patch.object(FileResetter, "reset_isolated_fns_file")

    await main(
        "192.0.0.1",
        8236,
        CONDITIONS_FILE_PATH,
        VERIFICATION_FILE_PATH,
        RUNTIME_FILE_PATH,
        REAL_ISOLATED_FNS_FILE_PATH,
        APP_LIBRARY_DIR,
        GROUP_ADDRESSES_PATH,
        RUNTIME_FILE_MODULE,
        LOGS_DIR,
    )

    # The on_trigger function should have written 1 line to this file.
    with open(TARGET_TXT_FILE_1, "r") as f:
        lines = f.readlines()
    assert len(lines) == 1
    assert TARGET_TEXT_1 in lines

    # The periodic function should have written 2 lines to this file.
    with open(TARGET_TXT_FILE_2, "r") as f:
        lines = f.readlines()
    assert len(lines) == 2
    assert TARGET_TEXT_2 in lines

    # Cleanup
    os.remove(TARGET_TXT_FILE_1)
    os.remove(TARGET_TXT_FILE_2)
