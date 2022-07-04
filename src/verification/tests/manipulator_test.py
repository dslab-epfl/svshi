import ast
import textwrap
from typing import Optional, cast
from ..manipulator import (
    DirectCallToIsolatedFunctionException,
    InvalidFileOpenModeException,
    InvalidFunctionCallException,
    InvalidGetLatestValueCallException,
    InvalidTriggerIfNotRunningCallException,
    InvalidPeriodicFunctionException,
    Manipulator,
    UnallowedArgsInIsolatedFunctionException,
    UntypedIsolatedFunctionException,
    ForbiddenModuleImported,
)
import pytest

TESTS_DIRECTORY = "tests"

app_priorities = {"first_app": 0, "second_app": 10, "third_app": 0}

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
                "DIMMER_SENSOR_INSTANCE_NAME",
                "DIMMER_ACTUATOR_INSTANCE_NAME",
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
    imports, functions, isolated_fns = manipulator.manipulate_mains(
        verification=True, app_priorities=app_priorities
    )
    names_periods = {fn.name_with_app_name: fn.period for fn in isolated_fns}
    assert names_periods == {
        "first_app_periodic_compute_bool": 5,
        "first_app_periodic_return_two": 100,
        "first_app_on_trigger_print": None,
        "second_app_periodic_float": 0,
    }

    assert len(imports) == 0
    print(functions)
    assert functions == [
        textwrap.dedent(
            '''\
            def third_app_invariant(first_app_app_state: AppState, second_app_app_state:
                AppState, third_app_app_state: AppState, physical_state: PhysicalState,
                internal_state: InternalState) ->bool:
                return THIRD_APP_HUMIDITY_SENSOR_INSTANCE_NAME.read(physical_state,
                    internal_state) < 82 and (2 <= svshi_api.get_hour_of_the_day(
                    internal_state) <= 3 and not THIRD_APP_SWITCH_INSTANCE_NAME.is_on(
                    physical_state, internal_state) or not 2 <= svshi_api.
                    get_hour_of_the_day(internal_state) <= 3)


            def third_app_iteration(first_app_app_state: AppState, second_app_app_state:
                AppState, third_app_app_state: AppState, physical_state: PhysicalState,
                internal_state: InternalState, isolated_fn_values: IsolatedFunctionsValues
                ):
                """
            pre: first_app_invariant(first_app_app_state, second_app_app_state, third_app_app_state, physical_state, internal_state)
            pre: second_app_invariant(first_app_app_state, second_app_app_state, third_app_app_state, physical_state, internal_state)
            pre: third_app_invariant(first_app_app_state, second_app_app_state, third_app_app_state, physical_state, internal_state)
            post: first_app_invariant(**__return__)
            post: second_app_invariant(**__return__)
            post: third_app_invariant(**__return__)
            """
                if THIRD_APP_HUMIDITY_SENSOR_INSTANCE_NAME.read(physical_state,
                    internal_state) > 30 and CO_TWO_SENSOR_INSTANCE_NAME.read() > 600.0:
                    another_file = 'file2.csv'
                    THIRD_APP_SWITCH_INSTANCE_NAME.on(physical_state, internal_state)
                    THIRD_APP_DIMMER_ACTUATOR_INSTANCE_NAME.set(34, physical_state,
                        internal_state)
                elif 2 <= svshi_api.get_hour_of_the_day(internal_state) <= 3:
                    t = svshi_api.get_minute_in_hour(internal_state)
                    THIRD_APP_SWITCH_INSTANCE_NAME.off(physical_state, internal_state)
                return {'first_app_app_state': first_app_app_state,
                    'second_app_app_state': second_app_app_state, 'third_app_app_state':
                    third_app_app_state, 'physical_state': physical_state,
                    'internal_state': internal_state}
            '''
        ),
        textwrap.dedent(
            '''\
            def first_app_invariant(first_app_app_state: AppState, second_app_app_state:
                AppState, third_app_app_state: AppState, physical_state: PhysicalState,
                internal_state: InternalState) ->bool:
                return FIRST_APP_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state,
                    internal_state) and FIRST_APP_TEMPERATURE_SENSOR_INSTANCE_NAME.read(
                    physical_state, internal_state) > 18 and not first_app_app_state.BOOL_1


            def first_app_iteration(first_app_app_state: AppState, second_app_app_state:
                AppState, third_app_app_state: AppState, physical_state: PhysicalState,
                internal_state: InternalState, isolated_fn_values: IsolatedFunctionsValues
                ):
                """
            pre: first_app_invariant(first_app_app_state, second_app_app_state, third_app_app_state, physical_state, internal_state)
            pre: second_app_invariant(first_app_app_state, second_app_app_state, third_app_app_state, physical_state, internal_state)
            pre: third_app_invariant(first_app_app_state, second_app_app_state, third_app_app_state, physical_state, internal_state)
            post: first_app_invariant(**__return__)
            post: second_app_invariant(**__return__)
            post: third_app_invariant(**__return__)
            """
                if (isolated_fn_values.first_app_periodic_compute_bool and not
                    first_app_app_state.BOOL_1):
                    first_app_app_state.INT_1 = 42
                    None
                else:
                    v = isolated_fn_values.first_app_periodic_return_two
                    None
                    None
                return {'first_app_app_state': first_app_app_state,
                    'second_app_app_state': second_app_app_state, 'third_app_app_state':
                    third_app_app_state, 'physical_state': physical_state,
                    'internal_state': internal_state}
            '''
        ),
        textwrap.dedent(
            '''\
            def second_app_invariant(first_app_app_state: AppState,
                second_app_app_state: AppState, third_app_app_state: AppState,
                physical_state: PhysicalState, internal_state: InternalState) ->bool:
                return SECOND_APP_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state,
                    internal_state) and SECOND_APP_SWITCH_INSTANCE_NAME.is_on(
                    physical_state, internal_state)


            def second_app_iteration(first_app_app_state: AppState,
                second_app_app_state: AppState, third_app_app_state: AppState,
                physical_state: PhysicalState, internal_state: InternalState,
                isolated_fn_values: IsolatedFunctionsValues):
                """
            pre: first_app_invariant(first_app_app_state, second_app_app_state, third_app_app_state, physical_state, internal_state)
            pre: second_app_invariant(first_app_app_state, second_app_app_state, third_app_app_state, physical_state, internal_state)
            pre: third_app_invariant(first_app_app_state, second_app_app_state, third_app_app_state, physical_state, internal_state)
            post: first_app_invariant(**__return__)
            post: second_app_invariant(**__return__)
            post: third_app_invariant(**__return__)
            """
                latest_float = isolated_fn_values.second_app_periodic_float
                if SECOND_APP_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state,
                    internal_state) and latest_float and latest_float > 2.0:
                    SECOND_APP_SWITCH_INSTANCE_NAME.on(physical_state, internal_state)
                return {'first_app_app_state': first_app_app_state,
                    'second_app_app_state': second_app_app_state, 'third_app_app_state':
                    third_app_app_state, 'physical_state': physical_state,
                    'internal_state': internal_state}
            '''
        ),
        textwrap.dedent(
            """\
            def system_behaviour(first_app_app_state: AppState, second_app_app_state:
                AppState, third_app_app_state: AppState, physical_state: PhysicalState,
                internal_state: InternalState, isolated_fn_values: IsolatedFunctionsValues
                ):
                if (isolated_fn_values.first_app_periodic_compute_bool and not
                    first_app_app_state.BOOL_1):
                    first_app_app_state.INT_1 = 42
                    None
                else:
                    v = isolated_fn_values.first_app_periodic_return_two
                    None
                    None
                if THIRD_APP_HUMIDITY_SENSOR_INSTANCE_NAME.read(physical_state,
                    internal_state) > 30 and CO_TWO_SENSOR_INSTANCE_NAME.read() > 600.0:
                    another_file = 'file2.csv'
                    THIRD_APP_SWITCH_INSTANCE_NAME.on(physical_state, internal_state)
                    THIRD_APP_DIMMER_ACTUATOR_INSTANCE_NAME.set(34, physical_state,
                        internal_state)
                elif 2 <= svshi_api.get_hour_of_the_day(internal_state) <= 3:
                    t = svshi_api.get_minute_in_hour(internal_state)
                    THIRD_APP_SWITCH_INSTANCE_NAME.off(physical_state, internal_state)
                latest_float = isolated_fn_values.second_app_periodic_float
                if SECOND_APP_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state,
                    internal_state) and latest_float and latest_float > 2.0:
                    SECOND_APP_SWITCH_INSTANCE_NAME.on(physical_state, internal_state)
                return {'first_app_app_state': first_app_app_state,
                    'second_app_app_state': second_app_app_state, 'third_app_app_state':
                    third_app_app_state, 'physical_state': physical_state,
                    'internal_state': internal_state}
            """
        ),
    ]


def test_manipulator_manipulate_mains_runtime():
    imports, functions, isolated_fns = manipulator.manipulate_mains(
        verification=False, app_priorities=app_priorities
    )
    names_periods = {fn.name_with_app_name: fn.period for fn in isolated_fns}
    assert names_periods == {
        "first_app_periodic_compute_bool": 5,
        "first_app_periodic_return_two": 100,
        "first_app_on_trigger_print": None,
        "second_app_periodic_float": 0,
    }
    assert imports == [
        "from slack_sdk.web.client import WebClient",
        "from slack_sdk.web.slack_response import SlackResponse",
        "from decouple import config",
    ]

    assert functions == [
        textwrap.dedent(
            """\
            def third_app_invariant(third_app_app_state: AppState, physical_state:
                PhysicalState, internal_state: InternalState) ->bool:
                return THIRD_APP_HUMIDITY_SENSOR_INSTANCE_NAME.read(physical_state
                    ) < 82 and (2 <= svshi_api.get_hour_of_the_day(internal_state) <= 3 and
                    not THIRD_APP_SWITCH_INSTANCE_NAME.is_on(physical_state) or not 2 <=
                    svshi_api.get_hour_of_the_day(internal_state) <= 3)


            def third_app_iteration(third_app_app_state: AppState, physical_state:
                PhysicalState, internal_state: InternalState, isolated_fn_values:
                IsolatedFunctionsValues):
                if THIRD_APP_HUMIDITY_SENSOR_INSTANCE_NAME.read(physical_state
                    ) > 30 and CO_TWO_SENSOR_INSTANCE_NAME.read() > 600.0:
                    another_file = 'file2.csv'
                    THIRD_APP_SWITCH_INSTANCE_NAME.on(physical_state)
                    THIRD_APP_DIMMER_ACTUATOR_INSTANCE_NAME.set(34, physical_state)
                elif 2 <= svshi_api.get_hour_of_the_day(internal_state) <= 3:
                    t = svshi_api.get_minute_in_hour(internal_state)
                    THIRD_APP_SWITCH_INSTANCE_NAME.off(physical_state)
            """
        ),
        textwrap.dedent(
            '''\
            def first_app_invariant(first_app_app_state: AppState, physical_state:
                PhysicalState, internal_state: InternalState) ->bool:
                return FIRST_APP_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state
                    ) and FIRST_APP_TEMPERATURE_SENSOR_INSTANCE_NAME.read(physical_state
                    ) > 18 and not first_app_app_state.BOOL_1


            def first_app_iteration(first_app_app_state: AppState, physical_state:
                PhysicalState, internal_state: InternalState, isolated_fn_values:
                IsolatedFunctionsValues):
                if (isolated_fn_values.first_app_periodic_compute_bool and not
                    first_app_app_state.BOOL_1):
                    first_app_app_state.INT_1 = 42
                    svshi_api.trigger_if_not_running(first_app_on_trigger_print,
                        FIRST_APP_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state))
                else:
                    v = isolated_fn_values.first_app_periodic_return_two
                    svshi_api.trigger_if_not_running(first_app_on_trigger_print, v)
                    svshi_api.trigger_if_not_running(first_app_on_trigger_print,
                        'file4.csv')


            def first_app_periodic_compute_bool(internal_state: InternalState) ->bool:
                """
                period: 5
                """
                return False


            def first_app_periodic_return_two(internal_state: InternalState) ->int:
                """
                period: 100
                """
                p = svshi_api.get_file_path('first_app', 'file1.txt', internal_state)
                f = svshi_api.get_file_text_mode('first_app', 'file2.txt', 'w',
                    internal_state)
                f2 = svshi_api.get_file_binary_mode('first_app', 'file3.txt', 'ar',
                    internal_state)
                return 2


            def first_app_on_trigger_print(s, internal_state: InternalState) ->None:
                print(s)
            '''
        ),
        textwrap.dedent(
            '''\
            def second_app_invariant(second_app_app_state: AppState, physical_state:
                PhysicalState, internal_state: InternalState) ->bool:
                return SECOND_APP_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state
                    ) and SECOND_APP_SWITCH_INSTANCE_NAME.is_on(physical_state)


            def second_app_iteration(second_app_app_state: AppState, physical_state:
                PhysicalState, internal_state: InternalState, isolated_fn_values:
                IsolatedFunctionsValues):
                latest_float = isolated_fn_values.second_app_periodic_float
                if SECOND_APP_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state
                    ) and latest_float and latest_float > 2.0:
                    SECOND_APP_SWITCH_INSTANCE_NAME.on(physical_state)


            def second_app_periodic_float(internal_state: InternalState) ->float:
                """period: 0"""
                return 42.0
            '''
        ),
        textwrap.dedent(
            """\
            def system_behaviour(first_app_app_state: AppState, second_app_app_state:
                AppState, third_app_app_state: AppState, physical_state: PhysicalState,
                internal_state: InternalState, isolated_fn_values: IsolatedFunctionsValues
                ):
                if (isolated_fn_values.first_app_periodic_compute_bool and not
                    first_app_app_state.BOOL_1):
                    first_app_app_state.INT_1 = 42
                    svshi_api.trigger_if_not_running(first_app_on_trigger_print,
                        FIRST_APP_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state))
                else:
                    v = isolated_fn_values.first_app_periodic_return_two
                    svshi_api.trigger_if_not_running(first_app_on_trigger_print, v)
                    svshi_api.trigger_if_not_running(first_app_on_trigger_print,
                        'file4.csv')
                if THIRD_APP_HUMIDITY_SENSOR_INSTANCE_NAME.read(physical_state
                    ) > 30 and CO_TWO_SENSOR_INSTANCE_NAME.read() > 600.0:
                    another_file = 'file2.csv'
                    THIRD_APP_SWITCH_INSTANCE_NAME.on(physical_state)
                    THIRD_APP_DIMMER_ACTUATOR_INSTANCE_NAME.set(34, physical_state)
                elif 2 <= svshi_api.get_hour_of_the_day(internal_state) <= 3:
                    t = svshi_api.get_minute_in_hour(internal_state)
                    THIRD_APP_SWITCH_INSTANCE_NAME.off(physical_state)
                latest_float = isolated_fn_values.second_app_periodic_float
                if SECOND_APP_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state
                    ) and latest_float and latest_float > 2.0:
                    SECOND_APP_SWITCH_INSTANCE_NAME.on(physical_state)
            """
        ),
    ]


def test_manipulator_manipulate_mains_raises_untyped_isolated_function_exception():
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

    with pytest.raises(UntypedIsolatedFunctionException):
        app_priorities = {"fourth_app": 0}
        manipulator.manipulate_mains(verification=True, app_priorities=app_priorities)


def test_manipulator_manipulate_mains_raises_invalid_function_call_exception_because_of_isolated_in_invariant_verification_true():
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
        app_priorities = {"fifth_app": 0}
        manipulator.manipulate_mains(verification=True, app_priorities=app_priorities)


def test_manipulator_manipulate_mains_raises_invalid_function_call_exception_because_of_isolated_in_invariant_verification_false():
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
        app_priorities = {"fifth_app": 0}
        manipulator.manipulate_mains(verification=False, app_priorities=app_priorities)


def test_manipulator_manipulate_mains_raises_invalid_function_call_exception_because_of_print_in_invariant_verification_true():
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
        app_priorities = {"sixth_app": 0}
        manipulator.manipulate_mains(verification=True, app_priorities=app_priorities)


def test_manipulator_manipulate_mains_raises_invalid_function_call_exception_because_of_print_in_invariant_verification_false():
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
        app_priorities = {"sixth_app": 0}
        manipulator.manipulate_mains(verification=False, app_priorities=app_priorities)


def test_manipulator_manipulate_mains_raises_invalid_function_call_exception_because_of_print_in_iteration_verification_true():
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
        app_priorities = {"seventh_app": 0}
        manipulator.manipulate_mains(verification=True, app_priorities=app_priorities)


def test_manipulator_manipulate_mains_raises_invalid_function_call_exception_because_of_print_in_iteration_verification_false():
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
        app_priorities = {"seventh_app": 0}
        manipulator.manipulate_mains(verification=False, app_priorities=app_priorities)


def test_manipulator_manipulate_mains_raises_invalid_function_call_exception_because_of_open_in_iteration_verification_true():
    manipulator = Manipulator(
        {
            (f"{TESTS_DIRECTORY}/fake_wrong_app_library", "eighth_app"): set(
                [
                    "BINARY_SENSOR_INSTANCE_NAME",
                    "SWITCH_INSTANCE_NAME",
                    "TEMPERATURE_SENSOR_INSTANCE_NAME",
                    "HUMIDITY_SENSOR_INSTANCE_NAME",
                ]
            ),
        },
        {"eighth_app": set()},
        "",
    )

    with pytest.raises(InvalidFunctionCallException):
        app_priorities = {"eighth_app": 0}
        manipulator.manipulate_mains(verification=True, app_priorities=app_priorities)


def test_manipulator_manipulate_mains_raises_invalid_function_call_exception_because_of_open_in_iteration_verification_false():
    manipulator = Manipulator(
        {
            (f"{TESTS_DIRECTORY}/fake_wrong_app_library", "eighth_app"): set(
                [
                    "BINARY_SENSOR_INSTANCE_NAME",
                    "SWITCH_INSTANCE_NAME",
                    "TEMPERATURE_SENSOR_INSTANCE_NAME",
                    "HUMIDITY_SENSOR_INSTANCE_NAME",
                ]
            ),
        },
        {"eighth_app": set()},
        "",
    )

    with pytest.raises(InvalidFunctionCallException):
        app_priorities = {"eighth_app": 0}
        manipulator.manipulate_mains(verification=False, app_priorities=app_priorities)


def test_manipulator_manipulate_mains_raises_invalid_function_call_exception_because_of_open_in_invariant_verification_true():
    manipulator = Manipulator(
        {
            (f"{TESTS_DIRECTORY}/fake_wrong_app_library", "nineth_app"): set(
                [
                    "BINARY_SENSOR_INSTANCE_NAME",
                    "SWITCH_INSTANCE_NAME",
                    "TEMPERATURE_SENSOR_INSTANCE_NAME",
                    "HUMIDITY_SENSOR_INSTANCE_NAME",
                ]
            ),
        },
        {"nineth_app": set()},
        "",
    )

    with pytest.raises(InvalidFunctionCallException):
        app_priorities = {"nineth_app": 0}
        manipulator.manipulate_mains(verification=True, app_priorities=app_priorities)


def test_manipulator_manipulate_mains_raises_invalid_function_call_exception_because_of_open_in_invariant_verification_false():
    manipulator = Manipulator(
        {
            (f"{TESTS_DIRECTORY}/fake_wrong_app_library", "nineth_app"): set(
                [
                    "BINARY_SENSOR_INSTANCE_NAME",
                    "SWITCH_INSTANCE_NAME",
                    "TEMPERATURE_SENSOR_INSTANCE_NAME",
                    "HUMIDITY_SENSOR_INSTANCE_NAME",
                ]
            ),
        },
        {"nineth_app": set()},
        "",
    )

    with pytest.raises(InvalidFunctionCallException):
        app_priorities = {"nineth_app": 0}
        manipulator.manipulate_mains(verification=False, app_priorities=app_priorities)


def test_manipulator_manipulate_mains_raises_invalid_function_call_exception_because_of_open_in_an_isolated_verification_false():
    manipulator = Manipulator(
        {
            (f"{TESTS_DIRECTORY}/fake_wrong_app_library", "sixteenth_app"): set(
                [
                    "BINARY_SENSOR_INSTANCE_NAME",
                    "SWITCH_INSTANCE_NAME",
                    "TEMPERATURE_SENSOR_INSTANCE_NAME",
                    "HUMIDITY_SENSOR_INSTANCE_NAME",
                ]
            ),
        },
        {"sixteenth_app": set()},
        "",
    )

    with pytest.raises(InvalidFunctionCallException):
        app_priorities = {"sixteenth_app": 0}
        manipulator.manipulate_mains(verification=False, app_priorities=app_priorities)


def test_manipulator_manipulate_mains_raises_invalid_function_call_exception_because_of_svshi_api_get_file_path_in_iteration_verification_true():
    manipulator = Manipulator(
        {
            (f"{TESTS_DIRECTORY}/fake_wrong_app_library", "tenth_app"): set(
                [
                    "BINARY_SENSOR_INSTANCE_NAME",
                    "SWITCH_INSTANCE_NAME",
                    "TEMPERATURE_SENSOR_INSTANCE_NAME",
                    "HUMIDITY_SENSOR_INSTANCE_NAME",
                ]
            ),
        },
        {"tenth_app": set()},
        "",
    )

    with pytest.raises(InvalidFunctionCallException):
        app_priorities = {"tenth_app": 0}
        manipulator.manipulate_mains(verification=True, app_priorities=app_priorities)


def test_manipulator_manipulate_mains_raises_invalid_function_call_exception_because_of_svshi_api_get_file_path_in_iteration_verification_false():
    manipulator = Manipulator(
        {
            (f"{TESTS_DIRECTORY}/fake_wrong_app_library", "tenth_app"): set(
                [
                    "BINARY_SENSOR_INSTANCE_NAME",
                    "SWITCH_INSTANCE_NAME",
                    "TEMPERATURE_SENSOR_INSTANCE_NAME",
                    "HUMIDITY_SENSOR_INSTANCE_NAME",
                ]
            ),
        },
        {"tenth_app": set()},
        "",
    )

    with pytest.raises(InvalidFunctionCallException):
        app_priorities = {"tenth_app": 0}
        manipulator.manipulate_mains(verification=False, app_priorities=app_priorities)


def test_manipulator_manipulate_mains_raises_invalid_function_call_exception_because_of_svshi_api_get_file_path_in_invariant_verification_true():
    manipulator = Manipulator(
        {
            (f"{TESTS_DIRECTORY}/fake_wrong_app_library", "eleventh_app"): set(
                [
                    "BINARY_SENSOR_INSTANCE_NAME",
                    "SWITCH_INSTANCE_NAME",
                    "TEMPERATURE_SENSOR_INSTANCE_NAME",
                    "HUMIDITY_SENSOR_INSTANCE_NAME",
                ]
            ),
        },
        {"eleventh_app": set()},
        "",
    )

    with pytest.raises(InvalidFunctionCallException):
        app_priorities = {"eleventh_app": 0}
        manipulator.manipulate_mains(verification=True, app_priorities=app_priorities)


def test_manipulator_manipulate_mains_raises_invalid_function_call_exception_because_of_svshi_api_get_file_path_in_invariant_verification_false():
    manipulator = Manipulator(
        {
            (f"{TESTS_DIRECTORY}/fake_wrong_app_library", "eleventh_app"): set(
                [
                    "BINARY_SENSOR_INSTANCE_NAME",
                    "SWITCH_INSTANCE_NAME",
                    "TEMPERATURE_SENSOR_INSTANCE_NAME",
                    "HUMIDITY_SENSOR_INSTANCE_NAME",
                ]
            ),
        },
        {"eleventh_app": set()},
        "",
    )

    with pytest.raises(InvalidFunctionCallException):
        app_priorities = {"eleventh_app": 0}
        manipulator.manipulate_mains(verification=False, app_priorities=app_priorities)


def test_manipulator_manipulate_mains_raises_invalid_function_call_exception_because_of_svshi_api_get_file_text_in_iteration_verification_true():
    manipulator = Manipulator(
        {
            (f"{TESTS_DIRECTORY}/fake_wrong_app_library", "twelfth_app"): set(
                [
                    "BINARY_SENSOR_INSTANCE_NAME",
                    "SWITCH_INSTANCE_NAME",
                    "TEMPERATURE_SENSOR_INSTANCE_NAME",
                    "HUMIDITY_SENSOR_INSTANCE_NAME",
                ]
            ),
        },
        {"twelfth_app": set()},
        "",
    )

    with pytest.raises(InvalidFunctionCallException):
        app_priorities = {"twelfth_app": 0}
        manipulator.manipulate_mains(verification=True, app_priorities=app_priorities)


def test_manipulator_manipulate_mains_raises_invalid_function_call_exception_because_of_svshi_api_get_file_text_in_iteration_verification_false():
    manipulator = Manipulator(
        {
            (f"{TESTS_DIRECTORY}/fake_wrong_app_library", "twelfth_app"): set(
                [
                    "BINARY_SENSOR_INSTANCE_NAME",
                    "SWITCH_INSTANCE_NAME",
                    "TEMPERATURE_SENSOR_INSTANCE_NAME",
                    "HUMIDITY_SENSOR_INSTANCE_NAME",
                ]
            ),
        },
        {"twelfth_app": set()},
        "",
    )

    with pytest.raises(InvalidFunctionCallException):
        app_priorities = {"twelfth_app": 0}
        manipulator.manipulate_mains(verification=False, app_priorities=app_priorities)


def test_manipulator_manipulate_mains_raises_invalid_function_call_exception_because_of_svshi_api_get_file_text_in_invariant_verification_true():
    manipulator = Manipulator(
        {
            (f"{TESTS_DIRECTORY}/fake_wrong_app_library", "thirteenth_app"): set(
                [
                    "BINARY_SENSOR_INSTANCE_NAME",
                    "SWITCH_INSTANCE_NAME",
                    "TEMPERATURE_SENSOR_INSTANCE_NAME",
                    "HUMIDITY_SENSOR_INSTANCE_NAME",
                ]
            ),
        },
        {"thirteenth_app": set()},
        "",
    )

    with pytest.raises(InvalidFunctionCallException):
        app_priorities = {"thirteenth_app": 0}
        manipulator.manipulate_mains(verification=True, app_priorities=app_priorities)


def test_manipulator_manipulate_mains_raises_invalid_function_call_exception_because_of_svshi_api_get_file_text_in_invariant_verification_false():
    manipulator = Manipulator(
        {
            (f"{TESTS_DIRECTORY}/fake_wrong_app_library", "thirteenth_app"): set(
                [
                    "BINARY_SENSOR_INSTANCE_NAME",
                    "SWITCH_INSTANCE_NAME",
                    "TEMPERATURE_SENSOR_INSTANCE_NAME",
                    "HUMIDITY_SENSOR_INSTANCE_NAME",
                ]
            ),
        },
        {"thirteenth_app": set()},
        "",
    )

    with pytest.raises(InvalidFunctionCallException):
        app_priorities = {"thirteenth_app": 0}
        manipulator.manipulate_mains(verification=False, app_priorities=app_priorities)


def test_manipulator_manipulate_mains_raises_invalid_function_call_exception_because_of_svshi_api_get_file_binary_in_iteration_verification_true():
    manipulator = Manipulator(
        {
            (f"{TESTS_DIRECTORY}/fake_wrong_app_library", "fourteenth_app"): set(
                [
                    "BINARY_SENSOR_INSTANCE_NAME",
                    "SWITCH_INSTANCE_NAME",
                    "TEMPERATURE_SENSOR_INSTANCE_NAME",
                    "HUMIDITY_SENSOR_INSTANCE_NAME",
                ]
            ),
        },
        {"fourteenth_app": set()},
        "",
    )

    with pytest.raises(InvalidFunctionCallException):
        app_priorities = {"fourteenth_app": 0}
        manipulator.manipulate_mains(verification=True, app_priorities=app_priorities)


def test_manipulator_manipulate_mains_raises_invalid_function_call_exception_because_of_svshi_api_get_file_binary_in_iteration_verification_false():
    manipulator = Manipulator(
        {
            (f"{TESTS_DIRECTORY}/fake_wrong_app_library", "fourteenth_app"): set(
                [
                    "BINARY_SENSOR_INSTANCE_NAME",
                    "SWITCH_INSTANCE_NAME",
                    "TEMPERATURE_SENSOR_INSTANCE_NAME",
                    "HUMIDITY_SENSOR_INSTANCE_NAME",
                ]
            ),
        },
        {"fourteenth_app": set()},
        "",
    )

    with pytest.raises(InvalidFunctionCallException):
        app_priorities = {"fourteenth_app": 0}
        manipulator.manipulate_mains(verification=False, app_priorities=app_priorities)


def test_manipulator_manipulate_mains_raises_invalid_function_call_exception_because_of_svshi_api_get_file_binary_in_invariant_verification_true():
    manipulator = Manipulator(
        {
            (f"{TESTS_DIRECTORY}/fake_wrong_app_library", "fifteenth_app"): set(
                [
                    "BINARY_SENSOR_INSTANCE_NAME",
                    "SWITCH_INSTANCE_NAME",
                    "TEMPERATURE_SENSOR_INSTANCE_NAME",
                    "HUMIDITY_SENSOR_INSTANCE_NAME",
                ]
            ),
        },
        {"fifteenth_app": set()},
        "",
    )

    with pytest.raises(InvalidFunctionCallException):
        app_priorities = {"fifteenth_app": 0}
        manipulator.manipulate_mains(verification=True, app_priorities=app_priorities)


def test_manipulator_manipulate_mains_raises_invalid_function_call_exception_because_of_svshi_api_get_file_binary_in_invariant_verification_false():
    manipulator = Manipulator(
        {
            (f"{TESTS_DIRECTORY}/fake_wrong_app_library", "fifteenth_app"): set(
                [
                    "BINARY_SENSOR_INSTANCE_NAME",
                    "SWITCH_INSTANCE_NAME",
                    "TEMPERATURE_SENSOR_INSTANCE_NAME",
                    "HUMIDITY_SENSOR_INSTANCE_NAME",
                ]
            ),
        },
        {"fifteenth_app": set()},
        "",
    )

    with pytest.raises(InvalidFunctionCallException):
        app_priorities = {"fifteenth_app": 0}
        manipulator.manipulate_mains(verification=False, app_priorities=app_priorities)


def test_manipulator_manipulate_mains_raises_invalid_function_call_exception_because_of_svshi_api_get_file_binary_with_wrong_mode_argument():
    manipulator = Manipulator(
        {
            (f"{TESTS_DIRECTORY}/fake_wrong_app_library", "seventeenth_app"): set(
                [
                    "BINARY_SENSOR_INSTANCE_NAME",
                    "SWITCH_INSTANCE_NAME",
                    "TEMPERATURE_SENSOR_INSTANCE_NAME",
                    "HUMIDITY_SENSOR_INSTANCE_NAME",
                ]
            ),
        },
        {"seventeenth_app": set()},
        "",
    )

    with pytest.raises(InvalidFileOpenModeException):
        app_priorities = {"seventeenth_app": 0}
        manipulator.manipulate_mains(verification=False, app_priorities=app_priorities)


def test_manipulator_manipulate_mains_raises_invalid_function_call_exception_because_of_svshi_api_get_file_text_with_wrong_mode_argument():
    manipulator = Manipulator(
        {
            (f"{TESTS_DIRECTORY}/fake_wrong_app_library", "eighteenth_app"): set(
                [
                    "BINARY_SENSOR_INSTANCE_NAME",
                    "SWITCH_INSTANCE_NAME",
                    "TEMPERATURE_SENSOR_INSTANCE_NAME",
                    "HUMIDITY_SENSOR_INSTANCE_NAME",
                ]
            ),
        },
        {"eighteenth_app": set()},
        "",
    )

    with pytest.raises(InvalidFileOpenModeException):
        app_priorities = {"eighteenth_app": 0}
        manipulator.manipulate_mains(verification=False, app_priorities=app_priorities)


def test_manipulator_manipulate_mains_raises_invalid_trigger_if_not_running_because_of_on_trigger_function_not_existing():
    manipulator = Manipulator(
        {
            (f"{TESTS_DIRECTORY}/fake_wrong_app_library", "nineteenth_app"): set(
                [
                    "BINARY_SENSOR_INSTANCE_NAME",
                    "SWITCH_INSTANCE_NAME",
                    "TEMPERATURE_SENSOR_INSTANCE_NAME",
                    "HUMIDITY_SENSOR_INSTANCE_NAME",
                ]
            ),
        },
        {"nineteenth_app": set()},
        "",
    )

    with pytest.raises(
        InvalidTriggerIfNotRunningCallException,
        match="Incorrect call to svshi_api.trigger_if_not_running: on_trigger function on_trigger_unknown does not exist!",
    ):
        app_priorities = {"nineteenth_app": 0}
        manipulator.manipulate_mains(verification=False, app_priorities=app_priorities)


def test_manipulator_manipulate_mains_raises_invalid_trigger_if_not_running_because_of_no_function_given():
    manipulator = Manipulator(
        {
            (f"{TESTS_DIRECTORY}/fake_wrong_app_library", "twentieth_app"): set(
                [
                    "BINARY_SENSOR_INSTANCE_NAME",
                    "SWITCH_INSTANCE_NAME",
                    "TEMPERATURE_SENSOR_INSTANCE_NAME",
                    "HUMIDITY_SENSOR_INSTANCE_NAME",
                ]
            ),
        },
        {"twentieth_app": set()},
        "",
    )

    with pytest.raises(
        InvalidTriggerIfNotRunningCallException,
        match="Incorrect call to svshi_api.trigger_if_not_running: no arguments provided.",
    ):
        app_priorities = {"twentieth_app": 0}
        manipulator.manipulate_mains(verification=False, app_priorities=app_priorities)


def test_manipulator_manipulate_mains_raises_invalid_trigger_if_not_running_because_of_not_calling_on_trigger_function():
    manipulator = Manipulator(
        {
            (f"{TESTS_DIRECTORY}/fake_wrong_app_library", "twenty_second_app"): set(
                [
                    "BINARY_SENSOR_INSTANCE_NAME",
                    "SWITCH_INSTANCE_NAME",
                    "TEMPERATURE_SENSOR_INSTANCE_NAME",
                    "HUMIDITY_SENSOR_INSTANCE_NAME",
                ]
            ),
        },
        {"twenty_second_app": set()},
        "",
    )

    with pytest.raises(
        InvalidTriggerIfNotRunningCallException,
        match="Incorrect call to svshi_api.trigger_if_not_running: should only be called on `on_trigger` functions. Found: some_func",
    ):
        app_priorities = {"twenty_second_app": 0}
        manipulator.manipulate_mains(verification=False, app_priorities=app_priorities)


def test_manipulator_manipulate_mains_raises_invalid_get_latest_value_because_of_too_many_args_given():
    manipulator = Manipulator(
        {
            (f"{TESTS_DIRECTORY}/fake_wrong_app_library", "twenty_first_app"): set(
                [
                    "BINARY_SENSOR_INSTANCE_NAME",
                    "SWITCH_INSTANCE_NAME",
                    "TEMPERATURE_SENSOR_INSTANCE_NAME",
                    "HUMIDITY_SENSOR_INSTANCE_NAME",
                ]
            ),
        },
        {"twenty_first_app": set()},
        "",
    )

    with pytest.raises(
        InvalidGetLatestValueCallException,
        match="Incorrect call to svshi_api.get_latest_value: Expecting exactly one argument. Found: 2",
    ):
        app_priorities = {"twenty_first_app": 0}
        manipulator.manipulate_mains(verification=False, app_priorities=app_priorities)


def test_manipulator_manipulate_mains_raises_invalid_get_latest_value_because_of_not_an_isolated_function_given():
    manipulator = Manipulator(
        {
            (f"{TESTS_DIRECTORY}/fake_wrong_app_library", "twenty_third_app"): set(
                [
                    "BINARY_SENSOR_INSTANCE_NAME",
                    "SWITCH_INSTANCE_NAME",
                    "TEMPERATURE_SENSOR_INSTANCE_NAME",
                    "HUMIDITY_SENSOR_INSTANCE_NAME",
                ]
            ),
        },
        {"twenty_third_app": set()},
        "",
    )

    with pytest.raises(
        InvalidGetLatestValueCallException,
        match="Incorrect call to svshi_api.get_latest_value: shoudl only be called on `on_trigger` and `periodic` functions. Found some_func",
    ):
        app_priorities = {"twenty_third_app": 0}
        manipulator.manipulate_mains(verification=False, app_priorities=app_priorities)


def test_manipulator_manipulate_mains_raises_direct_call_to_isolated_function_exception():
    manipulator = Manipulator(
        {
            (f"{TESTS_DIRECTORY}/fake_wrong_app_library", "twenty_fourth_app"): set(
                [
                    "BINARY_SENSOR_INSTANCE_NAME",
                    "SWITCH_INSTANCE_NAME",
                    "TEMPERATURE_SENSOR_INSTANCE_NAME",
                    "HUMIDITY_SENSOR_INSTANCE_NAME",
                ]
            ),
        },
        {"twenty_fourth_app": set()},
        "",
    )

    with pytest.raises(DirectCallToIsolatedFunctionException):
        app_priorities = {"twenty_fourth_app": 0}
        manipulator.manipulate_mains(verification=False, app_priorities=app_priorities)


def test_manipulator_manipulate_mains_raises_malformed_periodic_function_exception_because_no_period():
    manipulator = Manipulator(
        {
            (f"{TESTS_DIRECTORY}/fake_wrong_app_library", "twenty_fifth_app"): set(
                [
                    "BINARY_SENSOR_INSTANCE_NAME",
                    "SWITCH_INSTANCE_NAME",
                    "TEMPERATURE_SENSOR_INSTANCE_NAME",
                    "HUMIDITY_SENSOR_INSTANCE_NAME",
                ]
            ),
        },
        {"twenty_fifth_app": set()},
        "",
    )

    with pytest.raises(
        InvalidPeriodicFunctionException,
        match="Periodic function has no period specified in the docstring. If you wish to execute as often as possible, use `period: 0`.",
    ):
        app_priorities = {"twenty_fifth_app": 0}
        manipulator.manipulate_mains(verification=False, app_priorities=app_priorities)


def test_manipulator_manipulate_mains_raises_malformed_periodic_function_exception_because_multiple_periods():
    manipulator = Manipulator(
        {
            (f"{TESTS_DIRECTORY}/fake_wrong_app_library", "twenty_sixth_app"): set(
                [
                    "BINARY_SENSOR_INSTANCE_NAME",
                    "SWITCH_INSTANCE_NAME",
                    "TEMPERATURE_SENSOR_INSTANCE_NAME",
                    "HUMIDITY_SENSOR_INSTANCE_NAME",
                ]
            ),
        },
        {"twenty_sixth_app": set()},
        "",
    )

    with pytest.raises(
        InvalidPeriodicFunctionException,
        match="Periodic function has multiple periods defined in the docstring. Only one is allowed.",
    ):
        app_priorities = {"twenty_sixth_app": 0}
        manipulator.manipulate_mains(verification=False, app_priorities=app_priorities)


def test_manipulator_manipulate_mains_raises_malformed_periodic_function_exception_because_it_has_args():
    manipulator = Manipulator(
        {
            (f"{TESTS_DIRECTORY}/fake_wrong_app_library", "twenty_seventh_app"): set(
                [
                    "BINARY_SENSOR_INSTANCE_NAME",
                    "SWITCH_INSTANCE_NAME",
                    "TEMPERATURE_SENSOR_INSTANCE_NAME",
                    "HUMIDITY_SENSOR_INSTANCE_NAME",
                ]
            ),
        },
        {"twenty_seventh_app": set()},
        "",
    )

    with pytest.raises(
        InvalidPeriodicFunctionException,
        match="Function periodic_func is periodic and is not allowed to have any argument.",
    ):
        app_priorities = {"twenty_seventh_app": 0}
        manipulator.manipulate_mains(verification=False, app_priorities=app_priorities)


def test_manipulator_manipulate_mains_raises_untyped_isolated_function_exception_because_incorrect_return_type():
    manipulator = Manipulator(
        {
            (f"{TESTS_DIRECTORY}/fake_wrong_app_library", "twenty_eighth_app"): set(
                [
                    "BINARY_SENSOR_INSTANCE_NAME",
                    "SWITCH_INSTANCE_NAME",
                    "TEMPERATURE_SENSOR_INSTANCE_NAME",
                    "HUMIDITY_SENSOR_INSTANCE_NAME",
                ]
            ),
        },
        {"twenty_eighth_app": set()},
        "",
    )

    with pytest.raises(
        UntypedIsolatedFunctionException,
        match="The function 'periodic_func' has a return type which is not allowed. Allowed return types:",
    ):
        app_priorities = {"twenty_eighth_app": 0}
        manipulator.manipulate_mains(verification=False, app_priorities=app_priorities)


def test_manipulator_manipulate_mains_raises_unallowed_args_in_isolated_function_exception_because_default_value():
    manipulator = Manipulator(
        {
            (f"{TESTS_DIRECTORY}/fake_wrong_app_library", "twenty_nineth_app"): set(
                [
                    "BINARY_SENSOR_INSTANCE_NAME",
                    "SWITCH_INSTANCE_NAME",
                    "TEMPERATURE_SENSOR_INSTANCE_NAME",
                    "HUMIDITY_SENSOR_INSTANCE_NAME",
                ]
            ),
        },
        {"twenty_nineth_app": set()},
        "",
    )

    with pytest.raises(
        UnallowedArgsInIsolatedFunctionException,
        match=r"Function on_trigger_func is invalid: \*args, \*\*kwargs and default values are not allowed for `periodic` and `on_trigger` functions.",
    ):
        app_priorities = {"twenty_nineth_app": 0}
        manipulator.manipulate_mains(verification=False, app_priorities=app_priorities)


def test_manipulator_manipulate_mains_raises_unallowed_args_in_isolated_function_exception_because_kwargs():
    manipulator = Manipulator(
        {
            (f"{TESTS_DIRECTORY}/fake_wrong_app_library", "thirtieth_app"): set(
                [
                    "BINARY_SENSOR_INSTANCE_NAME",
                    "SWITCH_INSTANCE_NAME",
                    "TEMPERATURE_SENSOR_INSTANCE_NAME",
                    "HUMIDITY_SENSOR_INSTANCE_NAME",
                ]
            ),
        },
        {"thirtieth_app": set()},
        "",
    )

    with pytest.raises(
        UnallowedArgsInIsolatedFunctionException,
        match=r"Function on_trigger_func is invalid: \*args, \*\*kwargs and default values are not allowed for `periodic` and `on_trigger` functions.",
    ):
        app_priorities = {"thirtieth_app": 0}
        manipulator.manipulate_mains(verification=False, app_priorities=app_priorities)


def test_manipulator_manipulate_mains_raises_forbidden_module_when_using_time_module():
    manipulator = Manipulator(
        {
            (f"{TESTS_DIRECTORY}/fake_wrong_app_library", "thirty_first_app"): set(
                [
                    "BINARY_SENSOR_INSTANCE_NAME",
                    "SWITCH_INSTANCE_NAME",
                    "TEMPERATURE_SENSOR_INSTANCE_NAME",
                    "HUMIDITY_SENSOR_INSTANCE_NAME",
                ]
            ),
        },
        {"thirty_first_app": set()},
        "",
    )

    with pytest.raises(ForbiddenModuleImported):
        app_priorities = {"thirty_first_app": 0}
        manipulator.manipulate_mains(verification=False, app_priorities=app_priorities)


def test_manipulator_manipulate_mains_raises_forbidden_module_when_using_time_module_import_from():
    manipulator = Manipulator(
        {
            (f"{TESTS_DIRECTORY}/fake_wrong_app_library", "thirty_second_app"): set(
                [
                    "BINARY_SENSOR_INSTANCE_NAME",
                    "SWITCH_INSTANCE_NAME",
                    "TEMPERATURE_SENSOR_INSTANCE_NAME",
                    "HUMIDITY_SENSOR_INSTANCE_NAME",
                ]
            ),
        },
        {"thirty_second_app": set()},
        "",
    )

    with pytest.raises(ForbiddenModuleImported):
        app_priorities = {"thirty_second_app": 0}
        manipulator.manipulate_mains(verification=False, app_priorities=app_priorities)


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

    app_priorities = {"edges": 0}
    imports, functions, isolated_fns = manipulator.manipulate_mains(
        verification=True, app_priorities=app_priorities
    )
    names_periods = {fn.name_with_app_name: fn.period for fn in isolated_fns}
    print(names_periods)
    assert names_periods == {
        "edges_periodic_iteration": 5,
        "edges_on_trigger_send_email": None,
        "edges_periodic_return_int": 10,
    }
    assert len(imports) == 0

    assert functions == [
        textwrap.dedent(
            '''\
            def edges_invariant(edges_app_state: AppState, physical_state:
                PhysicalState, internal_state: InternalState) ->bool:
                return (EDGES_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state,
                    internal_state) or edges_app_state.INT_0 == 42
                    ) and EDGES_SWITCH_INSTANCE_NAME.is_on(physical_state, internal_state
                    ) or not (EDGES_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state,
                    internal_state) or edges_app_state.INT_0 == 42
                    ) and not EDGES_SWITCH_INSTANCE_NAME.is_on(physical_state,
                    internal_state)


            def edges_iteration(edges_app_state: AppState, physical_state:
                PhysicalState, internal_state: InternalState, isolated_fn_values:
                IsolatedFunctionsValues):
                """
            pre: edges_invariant(edges_app_state, physical_state, internal_state)
            post: edges_invariant(**__return__)
            """

                def yield_fun() ->Iterable[bool]:
                    yield not SWITCH_INSTANCE_NAME.is_on()

                def return_fun() ->bool:
                    return not SWITCH_INSTANCE_NAME.is_on()
                if EDGES_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state, internal_state
                    ) or edges_app_state.INT_0 == 42:
                    None
                    EDGES_SWITCH_INSTANCE_NAME.on(physical_state, internal_state)
                else:
                    EDGES_SWITCH_INSTANCE_NAME.off(physical_state, internal_state)
                a = edges_app_state.INT_0 + 1
                latest_int = isolated_fn_values.edges_periodic_return_int
                if latest_int == 42:
                    b = [latest_int, 2]
                    g: list = [x for x in b]
                    edges_app_state.INT_2 += 5
                else:
                    c = (lambda d: d + 1)(a)
                stuff = [[(y := 2), x / y] for x in range(5)]
                y = not isolated_fn_values.edges_periodic_return_int == 31
                d = {'a': EDGES_SWITCH_INSTANCE_NAME.is_on(physical_state, internal_state)}
                {k: v for k, v in d.items()}
                s = set(EDGES_SWITCH_INSTANCE_NAME.is_on(physical_state, internal_state))
                string = (
                    f'this is a beautiful string {EDGES_SWITCH_INSTANCE_NAME.is_on(physical_state, internal_state)}'
                    )
                None
                return {'edges_app_state': edges_app_state, 'physical_state':
                    physical_state, 'internal_state': internal_state}
            '''
        ),
        textwrap.dedent(
            """\
            def system_behaviour(edges_app_state: AppState, physical_state:
                PhysicalState, internal_state: InternalState, isolated_fn_values:
                IsolatedFunctionsValues):

                def yield_fun() ->Iterable[bool]:
                    yield not SWITCH_INSTANCE_NAME.is_on()

                def return_fun() ->bool:
                    return not SWITCH_INSTANCE_NAME.is_on()
                if EDGES_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state, internal_state
                    ) or edges_app_state.INT_0 == 42:
                    None
                    EDGES_SWITCH_INSTANCE_NAME.on(physical_state, internal_state)
                else:
                    EDGES_SWITCH_INSTANCE_NAME.off(physical_state, internal_state)
                a = edges_app_state.INT_0 + 1
                latest_int = isolated_fn_values.edges_periodic_return_int
                if latest_int == 42:
                    b = [latest_int, 2]
                    g: list = [x for x in b]
                    edges_app_state.INT_2 += 5
                else:
                    c = (lambda d: d + 1)(a)
                stuff = [[(y := 2), x / y] for x in range(5)]
                y = not isolated_fn_values.edges_periodic_return_int == 31
                d = {'a': EDGES_SWITCH_INSTANCE_NAME.is_on(physical_state, internal_state)}
                {k: v for k, v in d.items()}
                s = set(EDGES_SWITCH_INSTANCE_NAME.is_on(physical_state, internal_state))
                string = (
                    f'this is a beautiful string {EDGES_SWITCH_INSTANCE_NAME.is_on(physical_state, internal_state)}'
                    )
                None
                return {'edges_app_state': edges_app_state, 'physical_state':
                    physical_state, 'internal_state': internal_state}
            """
        ),
    ]


def _run_check_coherent_fn_call_to_fn_def(
    test_fn: str, test_call: str, expected: Optional[str]
):
    """
    Calls manipulator._check_coherent_fn_call_to_fn_def with the two given functions.
    If expected is provided, expects InvalidTriggerIfNotRunningCallException with
    the given string.
    """
    call_ast = cast(ast.Call, ast.parse(test_call).body[0].value)
    def_args = ast.parse(test_fn).body[0].args
    if expected:
        with pytest.raises(
            InvalidTriggerIfNotRunningCallException,
            match=expected,
        ):
            manipulator._check_coherent_fn_call_to_fn_def(
                call_ast.args, call_ast.keywords, def_args, "fn_name"
            )
    else:
        manipulator._check_coherent_fn_call_to_fn_def(
            call_ast.args, call_ast.keywords, def_args, "fn_name"
        )


def test_manipulator_check_coherent_fn_call_to_fn_def_raises_missing_kw_only():
    test_fn = textwrap.dedent(
        """\
        def on_trigger_fn(arg1: float, *, kw1: int) -> int:
            return 2
        """
    )
    test_call = textwrap.dedent(
        """\
        trigger(2.1, 3)
        """
    )
    expected = "Trigger to fn_name is missing some keyword-only arguments: {'kw1'}"
    _run_check_coherent_fn_call_to_fn_def(test_fn, test_call, expected)


def test_manipulator_check_coherent_fn_call_to_fn_def_raises_unknown_kwarg():
    test_fn = textwrap.dedent(
        """\
        def on_trigger_fn(arg1: float, *, kw1: int) -> int:
            return 2
        """
    )
    test_call = textwrap.dedent(
        """\
        trigger(2.1, kw1=3, kw_unknown=4)
        """
    )
    expected = (
        "Trigger to fn_name is supplied unknown keyword arguments: {'kw_unknown'}"
    )
    _run_check_coherent_fn_call_to_fn_def(test_fn, test_call, expected)


def test_manipulator_check_coherent_fn_call_to_fn_def_raises_missing_pos_args():
    test_fn = textwrap.dedent(
        """\
        def on_trigger_fn(arg1: float, arg2: int) -> int:
            return 2
        """
    )
    test_call = textwrap.dedent(
        """\
        trigger(2.1)
        """
    )
    expected = "Trigger to fn_name is missing some positional arguments."
    _run_check_coherent_fn_call_to_fn_def(test_fn, test_call, expected)


def test_manipulator_check_coherent_fn_call_to_fn_def_raises_too_many_pos_args():
    test_fn = textwrap.dedent(
        """\
        def on_trigger_fn(arg1: float, arg2: int) -> int:
            return 2
        """
    )
    test_call = textwrap.dedent(
        """\
        trigger(2.1, 4, 5)
        """
    )
    expected = "Trigger to fn_name has too many positional arguments."
    _run_check_coherent_fn_call_to_fn_def(test_fn, test_call, expected)


def test_manipulator_check_coherent_fn_call_to_fn_def_raises_unallowed_vararg():
    test_fn = textwrap.dedent(
        """\
        def on_trigger_fn(arg1: float, arg2: int, arg3: int) -> int:
            return 2
        """
    )
    test_call = textwrap.dedent(
        """\
        trigger(*args)
        """
    )
    expected = r"You are giving \*args or \*\*kwargs when triggering function fn_name, which is not allowed. Provide all arguments separately instead."
    _run_check_coherent_fn_call_to_fn_def(test_fn, test_call, expected)


def test_manipulator_check_coherent_fn_call_to_fn_def_raises_unallowed_kwargs():
    test_fn = textwrap.dedent(
        """\
        def on_trigger_fn(arg1: float, arg2: int, arg3: int) -> int:
            return 2
        """
    )
    test_call = textwrap.dedent(
        """\
        trigger(**kwargs)
        """
    )
    expected = r"You are giving \*args or \*\*kwargs when triggering function fn_name, which is not allowed. Provide all arguments separately instead."
    _run_check_coherent_fn_call_to_fn_def(test_fn, test_call, expected)
