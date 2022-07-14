from typing import Callable, IO, Optional, Protocol
from typing import Optional
import dataclasses
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


@dataclasses.dataclass
class PhysicalState:
    GA_0_0_1: bool
    GA_0_0_2: bool
    GA_0_0_3: float
    GA_0_0_4: float
    GA_0_0_5: int



@dataclasses.dataclass
class IsolatedFunctionsValues:
    first_app_periodic_compute_bool: Optional[bool] = None
    first_app_periodic_return_two: Optional[int] = None
    first_app_on_trigger_print: Optional[None] = None
    second_app_periodic_float: Optional[float] = None



@dataclasses.dataclass
class InternalState:
    """
    inv: 0 <= self.time_hour <= 23
    inv: 0 <= self.time_min <= 59
    inv: 1 <= self.time_day <= 31
    inv: 1 <= self.time_weekday <= 7
    inv: 1 <= self.time_month <= 12
    inv: 0 <= self.time_year
    """
    time_hour: int
    time_min: int
    time_day: int
    time_weekday: int
    time_month: int
    time_year: int
    c0: int


class Binary_sensor_first_app_binary_sensor_instance_name():
    def is_on(self, physical_state: PhysicalState, internal_state: InternalState) -> bool:
        """
        pre:
        post: physical_state.GA_0_0_1 == __return__
        """
        return physical_state.GA_0_0_1


class Switch_first_app_switch_instance_name():
    def on(self, physical_state: PhysicalState, internal_state: InternalState):
        """
        pre: 
        post: physical_state.GA_0_0_2  == True
        """
        physical_state.GA_0_0_2 = True

    def off(self, physical_state: PhysicalState, internal_state: InternalState):
        """
        pre: 
        post: physical_state.GA_0_0_2  == False
        """
        physical_state.GA_0_0_2 = False

    def is_on(self, physical_state: PhysicalState, internal_state: InternalState) -> bool:
        """
        pre: 
        post: physical_state.GA_0_0_2  == __return__
        """
        return physical_state.GA_0_0_2


class Temperature_sensor_first_app_temperature_sensor_instance_name():
    def read(self, physical_state: PhysicalState, internal_state: InternalState) -> float:
        """
        pre:
        post: physical_state.GA_0_0_3 == __return__
        """
        return physical_state.GA_0_0_3


class Humidity_sensor_first_app_humidity_sensor_instance_name():
    def read(self, physical_state: PhysicalState, internal_state: InternalState) -> float:
        """
        pre:
        post: physical_state.GA_0_0_4 == __return__
        """
        return physical_state.GA_0_0_4


class Binary_sensor_second_app_binary_sensor_instance_name():
    def is_on(self, physical_state: PhysicalState, internal_state: InternalState) -> bool:
        """
        pre:
        post: physical_state.GA_0_0_1 == __return__
        """
        return physical_state.GA_0_0_1


class Switch_second_app_switch_instance_name():
    def on(self, physical_state: PhysicalState, internal_state: InternalState):
        """
        pre: 
        post: physical_state.GA_0_0_2  == True
        """
        physical_state.GA_0_0_2 = True

    def off(self, physical_state: PhysicalState, internal_state: InternalState):
        """
        pre: 
        post: physical_state.GA_0_0_2  == False
        """
        physical_state.GA_0_0_2 = False

    def is_on(self, physical_state: PhysicalState, internal_state: InternalState) -> bool:
        """
        pre: 
        post: physical_state.GA_0_0_2  == __return__
        """
        return physical_state.GA_0_0_2


class Temperature_sensor_second_app_temperature_sensor_instance_name():
    def read(self, physical_state: PhysicalState, internal_state: InternalState) -> float:
        """
        pre:
        post: physical_state.GA_0_0_3 == __return__
        """
        return physical_state.GA_0_0_3


class Humidity_sensor_second_app_humidity_sensor_instance_name():
    def read(self, physical_state: PhysicalState, internal_state: InternalState) -> float:
        """
        pre:
        post: physical_state.GA_0_0_4 == __return__
        """
        return physical_state.GA_0_0_4


class Binary_sensor_third_app_binary_sensor_instance_name():
    def is_on(self, physical_state: PhysicalState, internal_state: InternalState) -> bool:
        """
        pre:
        post: physical_state.GA_0_0_1 == __return__
        """
        return physical_state.GA_0_0_1


class Switch_third_app_switch_instance_name():
    def on(self, physical_state: PhysicalState, internal_state: InternalState):
        """
        pre: 
        post: physical_state.GA_0_0_2  == True
        """
        physical_state.GA_0_0_2 = True

    def off(self, physical_state: PhysicalState, internal_state: InternalState):
        """
        pre: 
        post: physical_state.GA_0_0_2  == False
        """
        physical_state.GA_0_0_2 = False

    def is_on(self, physical_state: PhysicalState, internal_state: InternalState) -> bool:
        """
        pre: 
        post: physical_state.GA_0_0_2  == __return__
        """
        return physical_state.GA_0_0_2


class Temperature_sensor_third_app_temperature_sensor_instance_name():
    def read(self, physical_state: PhysicalState, internal_state: InternalState) -> float:
        """
        pre:
        post: physical_state.GA_0_0_3 == __return__
        """
        return physical_state.GA_0_0_3


class Humidity_sensor_third_app_humidity_sensor_instance_name():
    def read(self, physical_state: PhysicalState, internal_state: InternalState) -> float:
        """
        pre:
        post: physical_state.GA_0_0_4 == __return__
        """
        return physical_state.GA_0_0_4


class CO2_sensor_third_app_co_two_sensor_instance_name():
    def read(self, physical_state: PhysicalState, internal_state: InternalState) -> float:
        """
        pre:
        post: physical_state.GA_0_0_5 == __return__
        """
        return physical_state.GA_0_0_5


class SvshiApi():
    def __init__(self):
        pass

    def set_hour_of_the_day(self, internal_state: InternalState, time: int):
        """
        pre: 0 <= time <= 23
        post:internal_state.time_hour == time
        """
        internal_state.time_hour = time

    def get_hour_of_the_day(self, internal_state: InternalState) -> int:
        """
        post: 0 <= __return__ <= 23
        """
        return internal_state.time_hour

    def get_minute_in_hour(self, internal_state: InternalState) -> int:
        """
        post: 0 <= __return__ <= 59
        """
        return internal_state.time_min

    def set_minutes(self, internal_state: InternalState, time: int):
        """
        pre: 0 <= time <= 59
        post:internal_state.time_min == time
        """
        internal_state.time_min = time

    def get_day_of_week(self, internal_state: InternalState) -> int:
        """
        post: 1 <= __return__ <= 7
        """
        return internal_state.time_weekday

    def set_day_of_week(self, internal_state: InternalState, wday: int) -> int:
        """
        pre: 1 <= wday <= 7
        post: internal_state.time_weekday == wday
        """
        internal_state.time_weekday = wday

    def set_day(self, internal_state: InternalState, day: int):
        """
        pre: 1 <= day <= 31
        post: internal_state.time_day == day
        """
        internal_state.time_day = day 

    def get_day_of_month(self, internal_state: InternalState) -> int:
        """
        post: 1 <= __return__ <= 31
        """
        return internal_state.time_day

    def set_month(self, internal_state: InternalState, month: int):
        """
        pre: 1 <= month <= 12
        post:internal_state.time_month == month
        """
        internal_state.time_month = month

    def get_month_in_year(self, internal_state: InternalState) -> int:
        """
        post: 1 <= __return__ <= 12
        """
        return internal_state.time_month

    def set_year(self, internal_state: InternalState, year: int):
        """
        post:internal_state.time_year == year
        """
        internal_state.time_year = year

    def get_year(self, internal_state: InternalState) -> int:
        """
        post: 0 <= __return__
        """
        return internal_state.time_year

    class Hour:
        def __init__(self, value: int, internal_state: InternalState):
            self.value = value

    class Minute:
        def __init__(self, value: int, internal_state: InternalState):
            self.value = value

    class Day:
        def __init__(self, value: int, internal_state: InternalState):
            self.value = value

    class Week:
        def __init__(self, value: int, internal_state: InternalState):
            self.value = value

    class Month:
        def __init__(self, value: int, internal_state: InternalState):
            self.value = value

    def check_time_property(self, frequency, duration, condition: bool, internal_state: InternalState) -> bool:
        ...

    def dummy_check(self,i: InternalState,v:int) -> bool:
        return i.c0 == v



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
THIRD_APP_HUMIDITY_SENSOR_INSTANCE_NAME = Humidity_sensor_third_app_humidity_sensor_instance_name()
THIRD_APP_SWITCH_INSTANCE_NAME = Switch_third_app_switch_instance_name()
THIRD_APP_TEMPERATURE_SENSOR_INSTANCE_NAME = Temperature_sensor_third_app_temperature_sensor_instance_name()


def third_app_invariant(first_app_app_state: AppState, second_app_app_state:
    AppState, third_app_app_state: AppState, physical_state: PhysicalState,
    internal_state: InternalState) ->bool:
    return THIRD_APP_HUMIDITY_SENSOR_INSTANCE_NAME.read(physical_state,
        internal_state) < 82 and (2 <= svshi_api.get_hour_of_the_day(
        internal_state) <= 3 and not THIRD_APP_SWITCH_INSTANCE_NAME.is_on(
        physical_state, internal_state) or not 2 <= svshi_api.
        get_hour_of_the_day(internal_state) <= 3
        ) and svshi_api.check_time_property(svshi_api.Week(2), svshi_api.
        Day(2), THIRD_APP_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state,
        internal_state))


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
        internal_state) > 30 and THIRD_APP_CO_TWO_SENSOR_INSTANCE_NAME.read(
        physical_state, internal_state) > 600.0:
        another_file = 'file2.csv'
        THIRD_APP_SWITCH_INSTANCE_NAME.on(physical_state, internal_state)
    elif 2 <= svshi_api.get_hour_of_the_day(internal_state) <= 3:
        t = svshi_api.get_minute_in_hour(internal_state)
        THIRD_APP_SWITCH_INSTANCE_NAME.off(physical_state, internal_state)
    return {'first_app_app_state': first_app_app_state,
        'second_app_app_state': second_app_app_state, 'third_app_app_state':
        third_app_app_state, 'physical_state': physical_state,
        'internal_state': internal_state}

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

def second_app_invariant(first_app_app_state: AppState,
    second_app_app_state: AppState, third_app_app_state: AppState,
    physical_state: PhysicalState, internal_state: InternalState) ->bool:
    return SECOND_APP_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state,
        internal_state) and SECOND_APP_SWITCH_INSTANCE_NAME.is_on(
        physical_state, internal_state) and svshi_api.check_time_property(
        svshi_api.Day(1), svshi_api.Hour(1),
        SECOND_APP_SWITCH_INSTANCE_NAME.is_on(physical_state, internal_state))


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
        internal_state) > 30 and THIRD_APP_CO_TWO_SENSOR_INSTANCE_NAME.read(
        physical_state, internal_state) > 600.0:
        another_file = 'file2.csv'
        THIRD_APP_SWITCH_INSTANCE_NAME.on(physical_state, internal_state)
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
