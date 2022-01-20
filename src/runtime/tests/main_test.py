import os
import sys
import pytest
from contextlib import contextmanager
from io import StringIO
from pytest_mock import MockerFixture

from ..resetter import FileResetter
from ..main import cleanup, main, parse_args

LOGS_DIR = "tests/logs"
APP_LIBRARY_DIR = "tests/fake_app_library"
GROUP_ADDRESSES_PATH = "tests/fake_app_library/group_addresses.json"
CONDITIONS_FILE_PATH = "cond"
VERIFICATION_FILE_PATH = "ver"
RUNTIME_FILE_PATH = "run"
RUNTIME_FILE_MODULE = "svshi.src.runtime.tests.expected.expected_runtime_file"


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
        CONDITIONS_FILE_PATH, VERIFICATION_FILE_PATH, RUNTIME_FILE_PATH
    )
    reset_verification_file_spy = mocker.spy(file_resetter, "reset_verification_file")
    reset_runtime_file_spy = mocker.spy(file_resetter, "reset_runtime_file")
    reset_conditions_file_spy = mocker.spy(file_resetter, "reset_conditions_file")

    await cleanup(file_resetter)

    reset_verification_file_spy.assert_called_once()
    reset_runtime_file_spy.assert_called_once()
    reset_conditions_file_spy.assert_called_once()

    # Cleanup
    os.remove(CONDITIONS_FILE_PATH)
    os.remove(VERIFICATION_FILE_PATH)
    os.remove(RUNTIME_FILE_PATH)


@pytest.mark.asyncio
async def test_cleanup_uses_file_resetter_and_prints_error_message(
    mocker: MockerFixture,
):
    file_resetter = FileResetter(
        CONDITIONS_FILE_PATH, VERIFICATION_FILE_PATH, RUNTIME_FILE_PATH
    )
    reset_verification_file_spy = mocker.spy(file_resetter, "reset_verification_file")
    reset_runtime_file_spy = mocker.spy(file_resetter, "reset_runtime_file")
    reset_conditions_file_spy = mocker.spy(file_resetter, "reset_conditions_file")

    with captured_output() as (out, _):
        await cleanup(file_resetter, error=True)

    reset_verification_file_spy.assert_called_once()
    reset_runtime_file_spy.assert_called_once()
    reset_conditions_file_spy.assert_called_once()
    assert out.getvalue().strip() == "An error occurred!\nExiting... bye!"

    # Cleanup
    os.remove(CONDITIONS_FILE_PATH)
    os.remove(VERIFICATION_FILE_PATH)
    os.remove(RUNTIME_FILE_PATH)


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

    await main(
        "192.0.0.1",
        8236,
        CONDITIONS_FILE_PATH,
        VERIFICATION_FILE_PATH,
        RUNTIME_FILE_PATH,
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
