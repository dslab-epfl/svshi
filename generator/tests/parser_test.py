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


def test_parser_on_read_devices_throws_exceptions():
    parser = Parser("tests/devices_wrong.json")
    with pytest.raises(ParserException):
        parser.read_devices()
