from ..parser import (
    App,
    DeviceClass,
    DeviceInstance,
    GroupAddress,
    Parser,
)

TESTS_DIRECTORY = "tests"

parser = Parser(
    f"{TESTS_DIRECTORY}/fake_generated", f"{TESTS_DIRECTORY}/fake_app_library"
)


def test_parser_no_apps():
    parser = Parser("no", "no")
    assert len(parser.get_app_names()) == 0
    assert len(parser.get_app_priorities().keys()) == 0


def test_parser_parse_group_addresses():
    group_addresses = parser.parse_group_addresses()

    assert group_addresses == [
        GroupAddress("0/0/1", "bool"),
        GroupAddress("0/0/2", "bool"),
        GroupAddress("0/0/3", "float"),
        GroupAddress("0/0/4", "float"),
        GroupAddress("0/0/5", "int"),
        GroupAddress("0/0/6", "int"),
        GroupAddress("0/0/7", "int"),
    ]


def test_parser_parse_filenames():
    filenames = parser.get_filenames()

    assert filenames == {
        "third_app": {"file1.json", "file2.csv"},
        "first_app": {"file3.json", "file4.csv"},
        "second_app": {"file5.json", "file6.csv"},
    }

def test_parser_get_names():
    app_names = parser.get_app_names()

    assert app_names == [
        "first_app",
        "second_app",
        "third_app",
    ]

def test_parser_get_app_priority():
    priorities = parser.get_app_priorities()

    assert priorities == {
        "third_app": Parser.NOT_PRIVILEGED_PRIORITY_LEVEL,
        "first_app": Parser.NOT_PRIVILEGED_PRIORITY_LEVEL,
        "second_app": Parser.PRIVILEGED_PRIORITY_LEVEL,
    }


def test_parser_parse_devices_instances():
    instances = parser.parse_devices_instances()

    sorting_function = lambda d: d.name

    assert sorted(instances, key=sorting_function) == sorted(
        [
            DeviceInstance("first_app_binary_sensor_instance_name", "binary"),
            DeviceInstance("first_app_switch_instance_name", "switch"),
            DeviceInstance("first_app_temperature_sensor_instance_name", "temperature"),
            DeviceInstance("first_app_humidity_sensor_instance_name", "humidity"),
            DeviceInstance("second_app_binary_sensor_instance_name", "binary"),
            DeviceInstance("second_app_switch_instance_name", "switch"),
            DeviceInstance(
                "second_app_temperature_sensor_instance_name", "temperature"
            ),
            DeviceInstance("second_app_humidity_sensor_instance_name", "humidity"),
            DeviceInstance("third_app_binary_sensor_instance_name", "binary"),
            DeviceInstance("third_app_switch_instance_name", "switch"),
            DeviceInstance("third_app_temperature_sensor_instance_name", "temperature"),
            DeviceInstance("third_app_humidity_sensor_instance_name", "humidity"),
            DeviceInstance("third_app_co_two_sensor_instance_name", "co2"),
            DeviceInstance("third_app_dimmer_sensor_instance_name", "dimmerSensor"),
            DeviceInstance("third_app_dimmer_actuator_instance_name", "dimmerActuator"),
        ],
        key=sorting_function,
    )


def test_parser_parse_devices_classes():
    classes = parser.parse_devices_classes()

    sorting_function = lambda d: d.app.name

    first_app = App("first_app", f"{TESTS_DIRECTORY}/fake_generated")
    second_app = App("second_app", f"{TESTS_DIRECTORY}/fake_generated")
    third_app = App("third_app", f"{TESTS_DIRECTORY}/fake_app_library")

    assert sorted(classes, key=sorting_function) == sorted(
        [
            DeviceClass(first_app, "binary_sensor_instance_name", "binary", "0/0/1"),
            DeviceClass(first_app, "switch_instance_name", "switch", "0/0/2"),
            DeviceClass(
                first_app,
                "temperature_sensor_instance_name",
                "temperature",
                "0/0/3",
            ),
            DeviceClass(
                first_app,
                "humidity_sensor_instance_name",
                "humidity",
                "0/0/4",
            ),
            DeviceClass(second_app, "binary_sensor_instance_name", "binary", "0/0/1"),
            DeviceClass(second_app, "switch_instance_name", "switch", "0/0/2"),
            DeviceClass(
                second_app,
                "temperature_sensor_instance_name",
                "temperature",
                "0/0/3",
            ),
            DeviceClass(
                second_app,
                "humidity_sensor_instance_name",
                "humidity",
                "0/0/4",
            ),
            DeviceClass(third_app, "binary_sensor_instance_name", "binary", "0/0/1"),
            DeviceClass(third_app, "switch_instance_name", "switch", "0/0/2"),
            DeviceClass(
                third_app,
                "temperature_sensor_instance_name",
                "temperature",
                "0/0/3",
            ),
            DeviceClass(
                third_app,
                "humidity_sensor_instance_name",
                "humidity",
                "0/0/4",
            ),
            DeviceClass(
                third_app,
                "co_two_sensor_instance_name",
                "co2",
                "0/0/5",
            ),
            DeviceClass(
                third_app,
                "dimmer_sensor_instance_name",
                "dimmerSensor",
                "0/0/6",
            ),
            DeviceClass(
                third_app,
                "dimmer_actuator_instance_name",
                "dimmerActuator",
                "0/0/7",
            ),
        ],
        key=sorting_function,
    )
