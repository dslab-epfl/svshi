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


def test_parser_parse_group_addresses():
    group_addresses = parser.parse_group_addresses()

    assert group_addresses == [
        GroupAddress("0/0/1", "bool"),
        GroupAddress("0/0/2", "bool"),
        GroupAddress("0/0/3", "float"),
        GroupAddress("0/0/4", "float"),
        GroupAddress("0/0/5", "int"),
    ]


def test_parser_parse_filenames():
    filenames = parser.parse_filenames()

    assert filenames == {
        "third_app": {"file1.json", "file2.csv"},
        "first_app": {"file3.json", "file4.csv"},
        "second_app": {"file5.json", "file6.csv"},
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
        ],
        key=sorting_function,
    )
