from typing import Callable, IO, Optional, TypeVar
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
    STR_0: str = ""
    STR_1: str = ""
    STR_2: str = ""
    STR_3: str = ""


@dataclasses.dataclass
class PhysicalState:
    GA_0_0_3: float
    GA_0_0_1: bool
    GA_0_0_2: bool



@dataclasses.dataclass
class IsolatedFunctionsValues:
    test_app_one_on_trigger_send_email: Optional[None] = None



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



class Binary_sensor_test_app_one_binary_sensor_instance_name():
    def is_on(self, physical_state: PhysicalState) -> bool:
        """
        pre:
        post: physical_state.GA_0_0_1 == __return__
        """
        return physical_state.GA_0_0_1


class Switch_test_app_one_switch_instance_name():
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


class Temperature_sensor_test_app_two_temperature_sensor():
    def read(self, physical_state: PhysicalState) -> float:
        """
        pre:
        post: physical_state.GA_0_0_3 == __return__
        """
        return physical_state.GA_0_0_3


class Switch_test_app_two_switch():
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



TEST_APP_ONE_BINARY_SENSOR_INSTANCE_NAME = Binary_sensor_test_app_one_binary_sensor_instance_name()
TEST_APP_ONE_SWITCH_INSTANCE_NAME = Switch_test_app_one_switch_instance_name()
TEST_APP_TWO_SWITCH = Switch_test_app_two_switch()
TEST_APP_TWO_TEMPERATURE_SENSOR = Temperature_sensor_test_app_two_temperature_sensor()


def test_app_two_invariant(test_app_one_state: AppState, test_app_two_state: AppState, physical_state: PhysicalState, internal_state: InternalState
    ) ->bool:
    return True


def test_app_two_iteration(test_app_one_state: AppState, test_app_two_state: AppState, physical_state: PhysicalState, internal_state: InternalState, isolated_fn_values: IsolatedFunctionsValues):
    """
pre: test_app_one_invariant(test_app_one_state, test_app_two_state, physical_state, internal_state)
pre: test_app_two_invariant(test_app_one_state, test_app_two_state, physical_state, internal_state)
post: test_app_one_invariant(**__return__)
post: test_app_two_invariant(**__return__)
"""
    pass
    return {'test_app_one_state': test_app_one_state, 'test_app_two_state': test_app_two_state, 'physical_state': physical_state, 'internal_state': internal_state}

def test_app_one_invariant(test_app_one_state: AppState, test_app_two_state: AppState, physical_state: PhysicalState, internal_state: InternalState
    ) ->bool:
    return (TEST_APP_ONE_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state) or
        test_app_one_state.INT_0 == 42) and TEST_APP_ONE_SWITCH_INSTANCE_NAME.is_on(
        physical_state) or not (TEST_APP_ONE_BINARY_SENSOR_INSTANCE_NAME.
        is_on(physical_state) or test_app_one_state.INT_0 == 42
        ) and not TEST_APP_ONE_SWITCH_INSTANCE_NAME.is_on(physical_state)


def test_app_one_iteration(test_app_one_state: AppState, test_app_two_state: AppState, physical_state: PhysicalState, internal_state: InternalState, isolated_fn_values: IsolatedFunctionsValues):
    """
pre: test_app_one_invariant(test_app_one_state, test_app_two_state, physical_state, internal_state)
pre: test_app_two_invariant(test_app_one_state, test_app_two_state, physical_state, internal_state)
post: test_app_one_invariant(**__return__)
post: test_app_two_invariant(**__return__)
"""
    if TEST_APP_ONE_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state
        ) or test_app_one_state.INT_0 == 42:
        None
        TEST_APP_ONE_SWITCH_INSTANCE_NAME.on(physical_state)
    else:
        TEST_APP_ONE_SWITCH_INSTANCE_NAME.off(physical_state)
    return {'test_app_one_state': test_app_one_state, 'test_app_two_state': test_app_two_state, 'physical_state': physical_state, 'internal_state': internal_state}


def system_behaviour(test_app_one_state: AppState, test_app_two_state: AppState, physical_state: PhysicalState, internal_state: InternalState, isolated_fn_values: IsolatedFunctionsValues):
    if TEST_APP_ONE_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state
        ) or test_app_one_state.INT_0 == 42:
        None
        TEST_APP_ONE_SWITCH_INSTANCE_NAME.on(physical_state)
    else:
        TEST_APP_ONE_SWITCH_INSTANCE_NAME.off(physical_state)
    pass
    return {'test_app_one_state': test_app_one_state, 'test_app_two_state': test_app_two_state, 'physical_state': physical_state, 'internal_state': internal_state}
