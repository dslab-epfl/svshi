from ..manipulator import (
    InvalidUncheckedFunctionCallException,
    Manipulator,
    UntypedUncheckedFunctionException,
)
import pytest

TESTS_DIRECTORY = "tests"

manipulator = Manipulator(
    {
        (f"{TESTS_DIRECTORY}/fake_generated", "first_app"): set(
            [
                "BINARY_SENSOR_INSTANCE_NAME",
                "SWITCH_INSTANCE_NAME",
                "TEMPERATURE_SENSOR_INSTANCE_NAME",
                "HUMIDITY_SENSOR_INSTANCE_NAME",
            ]
        ),
        (f"{TESTS_DIRECTORY}/fake_generated", "second_app"): set(
            [
                "BINARY_SENSOR_INSTANCE_NAME",
                "SWITCH_INSTANCE_NAME",
                "TEMPERATURE_SENSOR_INSTANCE_NAME",
                "HUMIDITY_SENSOR_INSTANCE_NAME",
            ]
        ),
        (f"{TESTS_DIRECTORY}/fake_app_library", "third_app"): set(
            [
                "BINARY_SENSOR_INSTANCE_NAME",
                "SWITCH_INSTANCE_NAME",
                "TEMPERATURE_SENSOR_INSTANCE_NAME",
                "HUMIDITY_SENSOR_INSTANCE_NAME",
            ]
        ),
    }
)


def test_manipulator_manipulate_mains_verification():
    imports, functions = manipulator.manipulate_mains(verification=True)

    assert imports == ["import time"]
    assert functions == [
        'def third_app_precond(physical_state: PhysicalState) ->bool:\n    return THIRD_APP_HUMIDITY_SENSOR_INSTANCE_NAME.read(physical_state) < 82\n\n\ndef third_app_iteration(physical_state: PhysicalState):\n    """\npre: first_app_precond(physical_state)\npre: second_app_precond(physical_state)\npre: third_app_precond(physical_state)\npost: first_app_precond(__return__)\npost: second_app_precond(__return__)\npost: third_app_precond(__return__)\n"""\n    if THIRD_APP_HUMIDITY_SENSOR_INSTANCE_NAME.read(physical_state) > 30:\n        THIRD_APP_SWITCH_INSTANCE_NAME.on(physical_state)\n    return physical_state\n',
        'def first_app_precond(physical_state: PhysicalState) ->bool:\n    return FIRST_APP_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state\n        ) and FIRST_APP_TEMPERATURE_SENSOR_INSTANCE_NAME.read(physical_state\n        ) > 18\n\n\ndef first_app_iteration(physical_state: PhysicalState,\n    first_app_unchecked_compute_bool: bool, first_app_unchecked_return_two: int\n    ):\n    """\npre: first_app_precond(physical_state)\npre: second_app_precond(physical_state)\npre: third_app_precond(physical_state)\npre: first_app_unchecked_compute_bool == False\npre: first_app_unchecked_return_two > 0\npre: first_app_unchecked_return_two != 3\npost: first_app_precond(__return__)\npost: second_app_precond(__return__)\npost: third_app_precond(__return__)\n"""\n    if first_app_unchecked_compute_bool:\n        print(FIRST_APP_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state))\n    else:\n        v = first_app_unchecked_return_two\n        print(v)\n    return physical_state\n',
        'def second_app_precond(physical_state: PhysicalState) ->bool:\n    return SECOND_APP_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state\n        ) and SECOND_APP_SWITCH_INSTANCE_NAME.is_on(physical_state)\n\n\ndef second_app_iteration(physical_state: PhysicalState):\n    """\npre: first_app_precond(physical_state)\npre: second_app_precond(physical_state)\npre: third_app_precond(physical_state)\npost: first_app_precond(__return__)\npost: second_app_precond(__return__)\npost: third_app_precond(__return__)\n"""\n    print(SECOND_APP_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state))\n    time.sleep(2)\n    return physical_state\n',
    ]


def test_manipulator_manipulate_mains_runtime():
    imports, functions = manipulator.manipulate_mains(verification=False)

    assert imports == ["import time"]
    assert functions == [
        'def third_app_precond(physical_state: PhysicalState) ->bool:\n    return THIRD_APP_HUMIDITY_SENSOR_INSTANCE_NAME.read(physical_state) < 82\n\n\ndef third_app_iteration(physical_state: PhysicalState):\n    """\npre: first_app_precond(physical_state)\npre: second_app_precond(physical_state)\npre: third_app_precond(physical_state)\npost: first_app_precond(__return__)\npost: second_app_precond(__return__)\npost: third_app_precond(__return__)\n"""\n    if THIRD_APP_HUMIDITY_SENSOR_INSTANCE_NAME.read(physical_state) > 30:\n        THIRD_APP_SWITCH_INSTANCE_NAME.on(physical_state)\n    return physical_state\n',
        'def first_app_precond(physical_state: PhysicalState) ->bool:\n    return FIRST_APP_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state\n        ) and FIRST_APP_TEMPERATURE_SENSOR_INSTANCE_NAME.read(physical_state\n        ) > 18\n\n\ndef first_app_iteration(physical_state: PhysicalState):\n    """\npre: first_app_precond(physical_state)\npre: second_app_precond(physical_state)\npre: third_app_precond(physical_state)\npost: first_app_precond(__return__)\npost: second_app_precond(__return__)\npost: third_app_precond(__return__)\n"""\n    if first_app_unchecked_compute_bool():\n        print(FIRST_APP_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state))\n    else:\n        v = first_app_unchecked_return_two()\n        print(v)\n    return physical_state\n\n\ndef first_app_unchecked_compute_bool() ->bool:\n    """\n    post: __return__ == False\n    """\n    return False\n\n\ndef first_app_unchecked_return_two() ->int:\n    """\n    pre: True\n    post: __return__ > 0\n    post: __return__ != 3\n    """\n    return 2\n',
        'def second_app_precond(physical_state: PhysicalState) ->bool:\n    return SECOND_APP_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state\n        ) and SECOND_APP_SWITCH_INSTANCE_NAME.is_on(physical_state)\n\n\ndef second_app_iteration(physical_state: PhysicalState):\n    """\npre: first_app_precond(physical_state)\npre: second_app_precond(physical_state)\npre: third_app_precond(physical_state)\npost: first_app_precond(__return__)\npost: second_app_precond(__return__)\npost: third_app_precond(__return__)\n"""\n    print(SECOND_APP_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state))\n    time.sleep(2)\n    return physical_state\n',
    ]


def test_manipulator_manipulate_mains_raises_untyped_unchecked_function_exception():
    manipulator = Manipulator(
        {
            (f"{TESTS_DIRECTORY}/fake_wrong_app_library", "fourth_app"): set(
                [
                    "BINARY_SENSOR_INSTANCE_NAME",
                    "SWITCH_INSTANCE_NAME",
                    "TEMPERATURE_SENSOR_INSTANCE_NAME",
                    "HUMIDITY_SENSOR_INSTANCE_NAME",
                ]
            ),
        }
    )

    with pytest.raises(UntypedUncheckedFunctionException):
        manipulator.manipulate_mains(verification=True)


def test_manipulator_manipulate_mains_raises_invalid_unchecked_function_call_exception():
    manipulator = Manipulator(
        {
            (f"{TESTS_DIRECTORY}/fake_wrong_app_library", "fifth_app"): set(
                [
                    "BINARY_SENSOR_INSTANCE_NAME",
                    "SWITCH_INSTANCE_NAME",
                    "TEMPERATURE_SENSOR_INSTANCE_NAME",
                    "HUMIDITY_SENSOR_INSTANCE_NAME",
                ]
            ),
        }
    )

    with pytest.raises(InvalidUncheckedFunctionCallException):
        manipulator.manipulate_mains(verification=True)
