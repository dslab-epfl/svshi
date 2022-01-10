from ..manipulator import (
    InvalidFunctionCallException,
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
    },
    {
        "third_app": {"file1.json", "file2.csv"},
        "first_app": {"file3.json", "file4.csv"},
        "second_app": {"file5.json", "file6.csv"},
    },
    "",
)


def test_manipulator_manipulate_mains_verification():
    imports, functions = manipulator.manipulate_mains(verification=True)

    assert len(imports) == 0
    assert functions == [
        "def third_app_invariant(app_state: AppState, physical_state: PhysicalState\n    ) ->bool:\n    return THIRD_APP_HUMIDITY_SENSOR_INSTANCE_NAME.read(physical_state) < 82\n\n\ndef third_app_iteration(app_state: AppState, physical_state: PhysicalState):\n    \"\"\"\npre: first_app_invariant(app_state, physical_state)\npre: second_app_invariant(app_state, physical_state)\npre: third_app_invariant(app_state, physical_state)\npost: first_app_invariant(**__return__)\npost: second_app_invariant(**__return__)\npost: third_app_invariant(**__return__)\n\"\"\"\n    if THIRD_APP_HUMIDITY_SENSOR_INSTANCE_NAME.read(physical_state) > 30:\n        another_file = '/third_app/file2.csv'\n        THIRD_APP_SWITCH_INSTANCE_NAME.on(physical_state)\n    return {'app_state': app_state, 'physical_state': physical_state}\n",
        'def first_app_invariant(app_state: AppState, physical_state: PhysicalState\n    ) ->bool:\n    return FIRST_APP_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state\n        ) and FIRST_APP_TEMPERATURE_SENSOR_INSTANCE_NAME.read(physical_state\n        ) > 18\n\n\ndef first_app_iteration(app_state: AppState, physical_state: PhysicalState,\n    first_app_uncheckedcompute_bool: bool, first_app_unchecked_return_two: int\n    ):\n    """\npre: first_app_invariant(app_state, physical_state)\npre: second_app_invariant(app_state, physical_state)\npre: third_app_invariant(app_state, physical_state)\npre: __return__ == False\npre: __return__ > 0\npre: __return__ != 3\npost: first_app_invariant(**__return__)\npost: second_app_invariant(**__return__)\npost: third_app_invariant(**__return__)\n"""\n    if first_app_uncheckedcompute_bool:\n        None\n    else:\n        v = first_app_unchecked_return_two\n        None\n        None\n    return {\'app_state\': app_state, \'physical_state\': physical_state}\n',
        'def second_app_invariant(app_state: AppState, physical_state: PhysicalState\n    ) ->bool:\n    return SECOND_APP_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state\n        ) and SECOND_APP_SWITCH_INSTANCE_NAME.is_on(physical_state)\n\n\ndef second_app_iteration(app_state: AppState, physical_state: PhysicalState,\n    second_app_unchecked_time: float):\n    """\npre: first_app_invariant(app_state, physical_state)\npre: second_app_invariant(app_state, physical_state)\npre: third_app_invariant(app_state, physical_state)\npost: first_app_invariant(**__return__)\npost: second_app_invariant(**__return__)\npost: third_app_invariant(**__return__)\n"""\n    if SECOND_APP_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state\n        ) and second_app_unchecked_time > 2.0:\n        SECOND_APP_SWITCH_INSTANCE_NAME.on(physical_state)\n    return {\'app_state\': app_state, \'physical_state\': physical_state}\n',
    ]


def test_manipulator_manipulate_mains_runtime():
    imports, functions = manipulator.manipulate_mains(verification=False)

    assert imports == [
        "from slack_sdk.web.client import WebClient",
        "from slack_sdk.web.slack_response import SlackResponse",
        "from decouple import config",
        "import time",
    ]
    assert functions == [
        "def third_app_invariant(app_state: AppState, physical_state: PhysicalState\n    ) ->bool:\n    return THIRD_APP_HUMIDITY_SENSOR_INSTANCE_NAME.read(physical_state) < 82\n\n\ndef third_app_iteration(app_state: AppState, physical_state: PhysicalState):\n    \"\"\"\npre: first_app_invariant(app_state, physical_state)\npre: second_app_invariant(app_state, physical_state)\npre: third_app_invariant(app_state, physical_state)\npost: first_app_invariant(**__return__)\npost: second_app_invariant(**__return__)\npost: third_app_invariant(**__return__)\n\"\"\"\n    if THIRD_APP_HUMIDITY_SENSOR_INSTANCE_NAME.read(physical_state) > 30:\n        another_file = '/third_app/file2.csv'\n        THIRD_APP_SWITCH_INSTANCE_NAME.on(physical_state)\n    return {'app_state': app_state, 'physical_state': physical_state}\n",
        'def first_app_invariant(app_state: AppState, physical_state: PhysicalState\n    ) ->bool:\n    return FIRST_APP_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state\n        ) and FIRST_APP_TEMPERATURE_SENSOR_INSTANCE_NAME.read(physical_state\n        ) > 18\n\n\ndef first_app_iteration(app_state: AppState, physical_state: PhysicalState):\n    """\npre: first_app_invariant(app_state, physical_state)\npre: second_app_invariant(app_state, physical_state)\npre: third_app_invariant(app_state, physical_state)\npost: first_app_invariant(**__return__)\npost: second_app_invariant(**__return__)\npost: third_app_invariant(**__return__)\n"""\n    if first_app_uncheckedcompute_bool():\n        first_app_unchecked_print(FIRST_APP_BINARY_SENSOR_INSTANCE_NAME.\n            is_on(physical_state))\n    else:\n        v = first_app_unchecked_return_two()\n        first_app_unchecked_print(v)\n        first_app_unchecked_print(\'/first_app/file4.csv\')\n    return {\'app_state\': app_state, \'physical_state\': physical_state}\n\n\ndef first_app_uncheckedcompute_bool() ->bool:\n    """\n    post: __return__ == False\n    """\n    return False\n\n\ndef first_app_unchecked_return_two() ->int:\n    """\n    pre: True\n    post: __return__ > 0\n    post: __return__ != 3\n    """\n    return 2\n\n\ndef first_app_unchecked_print(s) ->None:\n    print(s)\n',
        'def second_app_invariant(app_state: AppState, physical_state: PhysicalState\n    ) ->bool:\n    return SECOND_APP_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state\n        ) and SECOND_APP_SWITCH_INSTANCE_NAME.is_on(physical_state)\n\n\ndef second_app_iteration(app_state: AppState, physical_state: PhysicalState):\n    """\npre: first_app_invariant(app_state, physical_state)\npre: second_app_invariant(app_state, physical_state)\npre: third_app_invariant(app_state, physical_state)\npost: first_app_invariant(**__return__)\npost: second_app_invariant(**__return__)\npost: third_app_invariant(**__return__)\n"""\n    if SECOND_APP_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state\n        ) and second_app_unchecked_time() > 2.0:\n        SECOND_APP_SWITCH_INSTANCE_NAME.on(physical_state)\n    return {\'app_state\': app_state, \'physical_state\': physical_state}\n\n\ndef second_app_unchecked_time() ->float:\n    return time.time()\n',
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
        },
        {"fourth_app": set()},
        "",
    )

    with pytest.raises(UntypedUncheckedFunctionException):
        manipulator.manipulate_mains(verification=True)


def test_manipulator_manipulate_mains_raises_invalid_function_call_exception_because_of_unchecked_in_invariant():
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
        },
        {"fifth_app": set()},
        "",
    )

    with pytest.raises(InvalidFunctionCallException):
        manipulator.manipulate_mains(verification=True)


def test_manipulator_manipulate_mains_raises_invalid_function_call_exception_because_of_print_in_invariant():
    manipulator = Manipulator(
        {
            (f"{TESTS_DIRECTORY}/fake_wrong_app_library", "sixth_app"): set(
                [
                    "BINARY_SENSOR_INSTANCE_NAME",
                    "SWITCH_INSTANCE_NAME",
                    "TEMPERATURE_SENSOR_INSTANCE_NAME",
                    "HUMIDITY_SENSOR_INSTANCE_NAME",
                ]
            ),
        },
        {"sixth_app": set()},
        "",
    )

    with pytest.raises(InvalidFunctionCallException):
        manipulator.manipulate_mains(verification=True)


def test_manipulator_manipulate_mains_raises_invalid_function_call_exception_because_of_print_in_iteration():
    manipulator = Manipulator(
        {
            (f"{TESTS_DIRECTORY}/fake_wrong_app_library", "seventh_app"): set(
                [
                    "BINARY_SENSOR_INSTANCE_NAME",
                    "SWITCH_INSTANCE_NAME",
                    "TEMPERATURE_SENSOR_INSTANCE_NAME",
                    "HUMIDITY_SENSOR_INSTANCE_NAME",
                ]
            ),
        },
        {"seventh_app": set()},
        "",
    )

    with pytest.raises(InvalidFunctionCallException):
        manipulator.manipulate_mains(verification=True)


def test_manipulator_manipulate_mains_edge_cases():
    manipulator = Manipulator(
        {
            (f"{TESTS_DIRECTORY}/edge_cases_library", "edges"): set(
                ["BINARY_SENSOR_INSTANCE_NAME", "SWITCH_INSTANCE_NAME"]
            ),
        },
        {"edges": set()},
        "",
    )

    imports, functions = manipulator.manipulate_mains(verification=True)

    assert len(imports) == 0
    assert functions == [
        "def edges_invariant(app_state: AppState, physical_state: PhysicalState) ->bool:\n    return (EDGES_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state) or \n        app_state.INT_0 == 42) and EDGES_SWITCH_INSTANCE_NAME.is_on(\n        physical_state) or not (EDGES_BINARY_SENSOR_INSTANCE_NAME.is_on(\n        physical_state) or app_state.INT_0 == 42\n        ) and not EDGES_SWITCH_INSTANCE_NAME.is_on(physical_state)\n\n\ndef edges_iteration(app_state: AppState, physical_state: PhysicalState,\n    edges_unchecked_return_int: int):\n    \"\"\"\npre: edges_invariant(app_state, physical_state)\npost: edges_invariant(**__return__)\n\"\"\"\n\n    def yield_fun() ->Iterable[bool]:\n        yield not SWITCH_INSTANCE_NAME.is_on()\n\n    def return_fun() ->bool:\n        return not SWITCH_INSTANCE_NAME.is_on()\n    if EDGES_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state\n        ) or app_state.INT_0 == 42:\n        None\n        EDGES_SWITCH_INSTANCE_NAME.on(physical_state)\n    else:\n        EDGES_SWITCH_INSTANCE_NAME.off(physical_state)\n    a = app_state.INT_0 + 1\n    if edges_unchecked_return_int == 42:\n        b = [edges_unchecked_return_int, 2]\n        g = [x for x in b]\n    else:\n        c = (lambda d: d + 1)(a)\n    y = not edges_unchecked_return_int == 31\n    d = {'a': EDGES_SWITCH_INSTANCE_NAME.is_on(physical_state)}\n    {k: v for k, v in d.items()}\n    s = set(EDGES_SWITCH_INSTANCE_NAME.is_on(physical_state))\n    string = (\n        f'this is a beautiful string {EDGES_SWITCH_INSTANCE_NAME.is_on(physical_state)}'\n        )\n    None\n    return {'app_state': app_state, 'physical_state': physical_state}\n"
    ]
