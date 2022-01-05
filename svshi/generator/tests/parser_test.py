from ..parsing.device import Device
from ..parsing.parser import Parser, ParserException
import pytest


DEVICES_FOLDER_PATH = "tests/devices"


def test_parser_reads_devices():
    parser = Parser(f"{DEVICES_FOLDER_PATH}/devices.json")
    devices = parser.read_devices()

    assert devices[0] == Device("binary_sensor_instance_name", "BinarySensor", "binary")
    assert devices[1] == Device("switch_instance_name", "Switch", "switch")
    assert devices[2] == Device(
        "temperature_sensor_instance_name", "TemperatureSensor", "temperature"
    )
    assert devices[3] == Device(
        "humidity_sensor_instance_name", "HumiditySensor", "humidity"
    )
    assert devices[4] == Device("A_switch", "Switch", "switch")


def test_parser_on_read_devices_throws_exceptions_on_wrong_device_type():
    parser = Parser(f"{DEVICES_FOLDER_PATH}/devices_wrong_device_type.json")
    with pytest.raises(ParserException):
        parser.read_devices()


def test_parser_on_read_devices_throws_exceptions_on_wrong_device_name():
    parser = Parser(f"{DEVICES_FOLDER_PATH}/devices_wrong_device_name.json")
    with pytest.raises(ParserException):
        parser.read_devices()


def test_parser_on_read_devices_throws_exceptions_on_missing_fields():
    with pytest.raises(ParserException):
        Parser(f"{DEVICES_FOLDER_PATH}/devices_missing_fields.json")


def test_parser_on_read_devices_throws_exceptions_on_missing_devices_name_field():
    parser = Parser(f"{DEVICES_FOLDER_PATH}/devices_missing_devices_name_field.json")
    with pytest.raises(ParserException):
        parser.read_devices()


def test_parser_on_read_devices_throws_exceptions_on_missing_devices_device_type_field():
    parser = Parser(
        f"{DEVICES_FOLDER_PATH}/devices_missing_devices_device_type_field.json"
    )
    with pytest.raises(ParserException):
        parser.read_devices()


def test_parser_on_read_devices_throws_exceptions_on_wrong_timer_type():
    with pytest.raises(ParserException):
        Parser(f"{DEVICES_FOLDER_PATH}/devices_wrong_timer_type.json")


def test_parser_on_read_devices_throws_exceptions_on_wrong_permission_level_type():
    with pytest.raises(ParserException):
        Parser(f"{DEVICES_FOLDER_PATH}/devices_wrong_permission_level_type.json")


def test_parser_on_read_devices_throws_exceptions_on_wrong_files_type():
    with pytest.raises(ParserException):
        Parser(f"{DEVICES_FOLDER_PATH}/devices_wrong_files_type.json")


def test_parser_on_read_devices_throws_exceptions_on_wrong_devices_type():
    with pytest.raises(ParserException):
        Parser(f"{DEVICES_FOLDER_PATH}/devices_wrong_devices_type.json")
