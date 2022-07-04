from decouple import config
from slack_sdk.web.client import WebClient
from slack_sdk.web.slack_response import SlackResponse
from typing import Callable, IO, Optional, TypeVar
from typing import Optional
import dataclasses
import sys
if sys.version_info < (3, 10):
    from typing_extensions import ParamSpec
else:
    from typing import ParamSpec
import time


@dataclasses.dataclass
class AppState:
    INT_0: int = 0
    INT_1: int = 0
    INT_2: int = 0
    INT_3: int = 0
    FLOAT_0: float = 0.0
    FLOAT_1: float = 0.0
    FLOAT_2: float = 0.0
    FLOAT_3: float = 0.0
    BOOL_0: bool = False
    BOOL_1: bool = False
    BOOL_2: bool = False
    BOOL_3: bool = False
    STR_0: str = ""
    STR_1: str = ""
    STR_2: str = ""
    STR_3: str = ""


@dataclasses.dataclass
class PhysicalState:
    GA_0_0_1: bool
    GA_0_0_2: bool
    GA_0_0_3: float
    GA_0_0_4: float
    GA_0_0_5: int
    GA_0_0_6: int
    GA_0_0_7: int



@dataclasses.dataclass
class IsolatedFunctionsValues:
    first_app_periodic_compute_bool: Optional[bool] = None
    first_app_periodic_return_two: Optional[int] = None
    first_app_on_trigger_print: Optional[None] = None
    first_app_on_trigger_do_nothing: Optional[None] = None
    second_app_periodic_float: Optional[float] = None
    second_app_on_trigger_do_nothing: Optional[None] = None



@dataclasses.dataclass
class InternalState:
    date_time: time.struct_time # time object, at local machine time
    app_files_runtime_folder_path: str # path to the folder in which files used by apps are stored at runtime


class Binary_sensor_first_app_binary_sensor_instance_name():
    def is_on(self, physical_state: PhysicalState) -> bool:
        """
        pre:
        post: physical_state.GA_0_0_1 == __return__
        """
        return physical_state.GA_0_0_1


class Switch_first_app_switch_instance_name():
    def on(self, physical_state: PhysicalState):
        """
        pre: 
        post: physical_state.GA_0_0_2  == True
        """
        physical_state.GA_0_0_2 = True

    def off(self, physical_state: PhysicalState):
        """
        pre: 
        post: physical_state.GA_0_0_2  == False
        """
        physical_state.GA_0_0_2 = False

    def is_on(self, physical_state: PhysicalState) -> bool:
        """
        pre: 
        post: physical_state.GA_0_0_2  == __return__
        """
        return physical_state.GA_0_0_2


class Temperature_sensor_first_app_temperature_sensor_instance_name():
    def read(self, physical_state: PhysicalState) -> float:
        """
        pre:
        post: physical_state.GA_0_0_3 == __return__
        """
        return physical_state.GA_0_0_3


class Humidity_sensor_first_app_humidity_sensor_instance_name():
    def read(self, physical_state: PhysicalState) -> float:
        """
        pre:
        post: physical_state.GA_0_0_4 == __return__
        """
        return physical_state.GA_0_0_4


class Binary_sensor_second_app_binary_sensor_instance_name():
    def is_on(self, physical_state: PhysicalState) -> bool:
        """
        pre:
        post: physical_state.GA_0_0_1 == __return__
        """
        return physical_state.GA_0_0_1


class Switch_second_app_switch_instance_name():
    def on(self, physical_state: PhysicalState):
        """
        pre: 
        post: physical_state.GA_0_0_2  == True
        """
        physical_state.GA_0_0_2 = True

    def off(self, physical_state: PhysicalState):
        """
        pre: 
        post: physical_state.GA_0_0_2  == False
        """
        physical_state.GA_0_0_2 = False

    def is_on(self, physical_state: PhysicalState) -> bool:
        """
        pre: 
        post: physical_state.GA_0_0_2  == __return__
        """
        return physical_state.GA_0_0_2


class Temperature_sensor_second_app_temperature_sensor_instance_name():
    def read(self, physical_state: PhysicalState) -> float:
        """
        pre:
        post: physical_state.GA_0_0_3 == __return__
        """
        return physical_state.GA_0_0_3


class Humidity_sensor_second_app_humidity_sensor_instance_name():
    def read(self, physical_state: PhysicalState) -> float:
        """
        pre:
        post: physical_state.GA_0_0_4 == __return__
        """
        return physical_state.GA_0_0_4


class Binary_sensor_third_app_binary_sensor_instance_name():
    def is_on(self, physical_state: PhysicalState) -> bool:
        """
        pre:
        post: physical_state.GA_0_0_1 == __return__
        """
        return physical_state.GA_0_0_1


class Switch_third_app_switch_instance_name():
    def on(self, physical_state: PhysicalState):
        """
        pre: 
        post: physical_state.GA_0_0_2  == True
        """
        physical_state.GA_0_0_2 = True

    def off(self, physical_state: PhysicalState):
        """
        pre: 
        post: physical_state.GA_0_0_2  == False
        """
        physical_state.GA_0_0_2 = False

    def is_on(self, physical_state: PhysicalState) -> bool:
        """
        pre: 
        post: physical_state.GA_0_0_2  == __return__
        """
        return physical_state.GA_0_0_2


class Temperature_sensor_third_app_temperature_sensor_instance_name():
    def read(self, physical_state: PhysicalState) -> float:
        """
        pre:
        post: physical_state.GA_0_0_3 == __return__
        """
        return physical_state.GA_0_0_3


class Humidity_sensor_third_app_humidity_sensor_instance_name():
    def read(self, physical_state: PhysicalState) -> float:
        """
        pre:
        post: physical_state.GA_0_0_4 == __return__
        """
        return physical_state.GA_0_0_4


class CO2_sensor_third_app_co_two_sensor_instance_name():
    def read(self, physical_state: PhysicalState) -> float:
        """
        pre:
        post: physical_state.GA_0_0_5 == __return__
        """
        return physical_state.GA_0_0_5


class Dimmer_Sensor_third_app_dimmer_sensor_instance_name():
    def read(self, physical_state: PhysicalState) -> int:
        """
        pre:
        post: physical_state.GA_0_0_6 == __return__
        """
        return physical_state.GA_0_0_6


class Dimmer_Actuator_third_app_dimmer_actuator_instance_name():
    def set(self, value: int, physical_state: PhysicalState):
        """
        pre: 
        post: physical_state.GA_0_0_7  == value
        """
        physical_state.GA_0_0_7 = value

    def read(self, physical_state: PhysicalState) -> int:
        """
        pre: 
        post: physical_state.GA_0_0_7  == __return__
        """
        return physical_state.GA_0_0_7


class SvshiApi():

    def __init__(self):
        pass

    def get_hour_of_the_day(self, internal_state: InternalState) -> int:
        """
        post: 0 <= __return__ <= 23
        """
        return internal_state.date_time.tm_hour

    def get_minute_in_hour(self, internal_state: InternalState) -> int:
        """
        post: 0 <= __return__ <= 59
        """
        return internal_state.date_time.tm_min

    def get_day_of_week(self, internal_state: InternalState) -> int:
        """
        post: 1 <= __return__ <= 7
        """
        return internal_state.date_time.tm_wday

    def get_day_of_month(self, internal_state: InternalState) -> int:
        """
        post: 1 <= __return__ <= 31
        """
        return internal_state.date_time.tm_mday

    def get_month_in_year(self, internal_state: InternalState) -> int:
        """
        post: 1 <= __return__ <= 12
        """
        return internal_state.date_time.tm_mon

    def get_year(self, internal_state: InternalState) -> int:
        """
        post: 0 <= __return__
        """
        return internal_state.date_time.tm_year

    _T = TypeVar("_T")
    _P = ParamSpec("_P")
    # Type alias for the on_trigger consumer.
    OnTriggerConsumer = Callable[[Callable[_P, _T]], Callable[_P, None]]

    def __not_implemented_consumer(self, fn: Callable[_P, _T]) -> Callable[_P, None]:
        raise NotImplementedError(
            "on_trigger_consumer was called before being initialized."
        )

    __on_trigger_consumer: OnTriggerConsumer = __not_implemented_consumer

    def register_on_trigger_consumer(self, on_trigger_consumer: OnTriggerConsumer):
        self.__on_trigger_consumer = on_trigger_consumer

    def trigger_if_not_running(
        self, on_trigger_function: Callable[_P, _T]
    ) -> Callable[_P, None]:
        return self.__on_trigger_consumer(on_trigger_function)

    def get_file_text_mode(self, app_name: str, file_name: str, mode: str, internal_state: InternalState) -> Optional[IO[str]]:
        try:
            return open(self.get_file_path(app_name, file_name, internal_state), mode)
        except:
            return None

    def get_file_binary_mode(self, app_name: str, file_name: str, mode: str, internal_state: InternalState) -> Optional[IO[bytes]]:
        try:
            return open(self.get_file_path(app_name, file_name, internal_state), f"{mode}b")
        except:
            return None

    def get_file_path(self, app_name: str, file_name: str, internal_state: InternalState) -> str:
        return f"{internal_state.app_files_runtime_folder_path}/{app_name}/{file_name}"



svshi_api = SvshiApi()
FIRST_APP_BINARY_SENSOR_INSTANCE_NAME = Binary_sensor_first_app_binary_sensor_instance_name()
FIRST_APP_HUMIDITY_SENSOR_INSTANCE_NAME = Humidity_sensor_first_app_humidity_sensor_instance_name()
FIRST_APP_SWITCH_INSTANCE_NAME = Switch_first_app_switch_instance_name()
FIRST_APP_TEMPERATURE_SENSOR_INSTANCE_NAME = Temperature_sensor_first_app_temperature_sensor_instance_name()
SECOND_APP_BINARY_SENSOR_INSTANCE_NAME = Binary_sensor_second_app_binary_sensor_instance_name()
SECOND_APP_HUMIDITY_SENSOR_INSTANCE_NAME = Humidity_sensor_second_app_humidity_sensor_instance_name()
SECOND_APP_SWITCH_INSTANCE_NAME = Switch_second_app_switch_instance_name()
SECOND_APP_TEMPERATURE_SENSOR_INSTANCE_NAME = Temperature_sensor_second_app_temperature_sensor_instance_name()
THIRD_APP_BINARY_SENSOR_INSTANCE_NAME = Binary_sensor_third_app_binary_sensor_instance_name()
THIRD_APP_CO_TWO_SENSOR_INSTANCE_NAME = CO2_sensor_third_app_co_two_sensor_instance_name()
THIRD_APP_DIMMER_ACTUATOR_INSTANCE_NAME = Dimmer_Actuator_third_app_dimmer_actuator_instance_name()
THIRD_APP_DIMMER_SENSOR_INSTANCE_NAME = Dimmer_Sensor_third_app_dimmer_sensor_instance_name()
THIRD_APP_HUMIDITY_SENSOR_INSTANCE_NAME = Humidity_sensor_third_app_humidity_sensor_instance_name()
THIRD_APP_SWITCH_INSTANCE_NAME = Switch_third_app_switch_instance_name()
THIRD_APP_TEMPERATURE_SENSOR_INSTANCE_NAME = Temperature_sensor_third_app_temperature_sensor_instance_name()


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
        ) > 30 and THIRD_APP_CO_TWO_SENSOR_INSTANCE_NAME.read(physical_state
        ) > 600.0:
        another_file = 'file2.csv'
        THIRD_APP_SWITCH_INSTANCE_NAME.on(physical_state)
        THIRD_APP_DIMMER_ACTUATOR_INSTANCE_NAME.set(34, physical_state)
    elif 2 <= svshi_api.get_hour_of_the_day(internal_state) <= 3:
        t = svshi_api.get_minute_in_hour(internal_state)
        THIRD_APP_SWITCH_INSTANCE_NAME.off(physical_state)

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
        svshi_api.trigger_if_not_running(first_app_on_trigger_print)(
            FIRST_APP_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state))
    else:
        v = isolated_fn_values.first_app_periodic_return_two
        svshi_api.trigger_if_not_running(first_app_on_trigger_print)(v)
        svshi_api.trigger_if_not_running(first_app_on_trigger_print)(
            'file4.csv')
        svshi_api.trigger_if_not_running(first_app_on_trigger_do_nothing)()


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


def first_app_on_trigger_do_nothing(internal_state: InternalState) ->None:
    return None

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
        svshi_api.trigger_if_not_running(second_app_on_trigger_do_nothing)()


def second_app_periodic_float(internal_state: InternalState) ->float:
    """period: 0"""
    return 42.0


def second_app_on_trigger_do_nothing(internal_state: InternalState) ->None:
    return None

def system_behaviour(first_app_app_state: AppState, second_app_app_state:
    AppState, third_app_app_state: AppState, physical_state: PhysicalState,
    internal_state: InternalState, isolated_fn_values: IsolatedFunctionsValues
    ):
    if (isolated_fn_values.first_app_periodic_compute_bool and not
        first_app_app_state.BOOL_1):
        first_app_app_state.INT_1 = 42
        svshi_api.trigger_if_not_running(first_app_on_trigger_print)(
            FIRST_APP_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state))
    else:
        v = isolated_fn_values.first_app_periodic_return_two
        svshi_api.trigger_if_not_running(first_app_on_trigger_print)(v)
        svshi_api.trigger_if_not_running(first_app_on_trigger_print)(
            'file4.csv')
        svshi_api.trigger_if_not_running(first_app_on_trigger_do_nothing)()
    if THIRD_APP_HUMIDITY_SENSOR_INSTANCE_NAME.read(physical_state
        ) > 30 and THIRD_APP_CO_TWO_SENSOR_INSTANCE_NAME.read(physical_state
        ) > 600.0:
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
        svshi_api.trigger_if_not_running(second_app_on_trigger_do_nothing)()
