from ..parsing.device import Device
from ..parsing.parser import Parser, ParserException
import pytest


def test_parser_reads_devices():
    parser = Parser("tests/devices.json")
    devices = parser.read_devices()

    assert devices[0] == Device("binary_sensor_instance_name", "BinarySensor", "binary")
    assert devices[1] == Device("switch_instance_name", "Switch", "switch")
    assert devices[2] == Device(
        "temperature_sensor_instance_name", "TemperatureSensor", "temperature"
    )
    assert devices[3] == Device(
        "humidity_sensor_instance_name", "HumiditySensor", "humidity"
    )
    assert devices[4] == Device(
        "A_switch", "Switch", "switch"
    )


def test_parser_on_read_devices_throws_exceptions_on_wrong_type():
    parser = Parser("tests/devices_wrong_type.json")
    with pytest.raises(ParserException):
        parser.read_devices()

def test_parser_on_read_devices_throws_exceptions_on_wrong_name():
    parser = Parser("tests/devices_wrong_name.json")
    with pytest.raises(ParserException):
        parser.read_devices()
