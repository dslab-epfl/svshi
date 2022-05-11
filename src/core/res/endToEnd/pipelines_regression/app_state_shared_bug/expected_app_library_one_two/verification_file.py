import dataclasses


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
class InternalState:
 """
 inv: self.time>=0
 """
 time: int #time in seconds


class Binary_sensor_test_app_one_binary_sensor_instance_name():
    def is_on(self, physical_state: PhysicalState, internal_state: InternalState) -> bool:
        """
        pre:
        post: physical_state.GA_0_0_1 == __return__
        """
        return physical_state.GA_0_0_1
    

class Switch_test_app_one_switch_instance_name():
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
    

class Temperature_sensor_test_app_two_temperature_sensor():
    def read(self, physical_state: PhysicalState, internal_state: InternalState) -> float:
        """
        pre:
        post: physical_state.GA_0_0_3 == __return__
        """
        return physical_state.GA_0_0_3
    

class SvshiApi():

    def __init__(self):
        pass

    def set_time(self, internal_state: InternalState, time: int):
        """
        pre:time>=0
        post:internal_state.time == time
        """
        internal_state.time = time

    def get_time(self, internal_state: InternalState) -> int:
        """
        pre:internal_state.time>=0
        post:internal_state.time>=0
        """
        return internal_state.time

    def get_hour_of_the_day(self, internal_state: InternalState) -> int:
        """
        post: 0 <= __return__ <= 23
        """
        time = internal_state.time
        q = time // (60 * 60)
        tmp = q // 24

        return q - tmp * 24
    


svshi_api = SvshiApi()
TEST_APP_ONE_BINARY_SENSOR_INSTANCE_NAME = Binary_sensor_test_app_one_binary_sensor_instance_name()
TEST_APP_ONE_SWITCH_INSTANCE_NAME = Switch_test_app_one_switch_instance_name()
TEST_APP_TWO_TEMPERATURE_SENSOR = Temperature_sensor_test_app_two_temperature_sensor()


def test_app_one_invariant(test_app_one_app_state: AppState,
    test_app_two_app_state: AppState, physical_state: PhysicalState,
    internal_state: InternalState) ->bool:
    return test_app_one_app_state.INT_0 != 42


def test_app_one_iteration(test_app_one_app_state: AppState,
    test_app_two_app_state: AppState, physical_state: PhysicalState,
    internal_state: InternalState):
    """
pre: test_app_one_invariant(test_app_one_app_state, test_app_two_app_state, physical_state, internal_state)
pre: test_app_two_invariant(test_app_one_app_state, test_app_two_app_state, physical_state, internal_state)
post: test_app_one_invariant(**__return__)
post: test_app_two_invariant(**__return__)
"""
    if TEST_APP_ONE_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state,
        internal_state):
        TEST_APP_ONE_SWITCH_INSTANCE_NAME.on(physical_state, internal_state)
    else:
        TEST_APP_ONE_SWITCH_INSTANCE_NAME.off(physical_state, internal_state)
    return {'test_app_one_app_state': test_app_one_app_state,
        'test_app_two_app_state': test_app_two_app_state, 'physical_state':
        physical_state, 'internal_state': internal_state}

def test_app_two_invariant(test_app_one_app_state: AppState,
    test_app_two_app_state: AppState, physical_state: PhysicalState,
    internal_state: InternalState) ->bool:
    return True


def test_app_two_iteration(test_app_one_app_state: AppState,
    test_app_two_app_state: AppState, physical_state: PhysicalState,
    internal_state: InternalState):
    """
pre: test_app_one_invariant(test_app_one_app_state, test_app_two_app_state, physical_state, internal_state)
pre: test_app_two_invariant(test_app_one_app_state, test_app_two_app_state, physical_state, internal_state)
post: test_app_one_invariant(**__return__)
post: test_app_two_invariant(**__return__)
"""
    if TEST_APP_TWO_TEMPERATURE_SENSOR.read(physical_state, internal_state
        ) != None and TEST_APP_TWO_TEMPERATURE_SENSOR.read(physical_state,
        internal_state) > 42:
        test_app_two_app_state.INT_0 = 42
    return {'test_app_one_app_state': test_app_one_app_state,
        'test_app_two_app_state': test_app_two_app_state, 'physical_state':
        physical_state, 'internal_state': internal_state}

def system_behaviour(test_app_one_app_state: AppState,
    test_app_two_app_state: AppState, physical_state: PhysicalState,
    internal_state: InternalState):
    if TEST_APP_ONE_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state,
        internal_state):
        TEST_APP_ONE_SWITCH_INSTANCE_NAME.on(physical_state, internal_state)
    else:
        TEST_APP_ONE_SWITCH_INSTANCE_NAME.off(physical_state, internal_state)
    if TEST_APP_TWO_TEMPERATURE_SENSOR.read(physical_state, internal_state
        ) != None and TEST_APP_TWO_TEMPERATURE_SENSOR.read(physical_state,
        internal_state) > 42:
        test_app_two_app_state.INT_0 = 42
    return {'test_app_one_app_state': test_app_one_app_state,
        'test_app_two_app_state': test_app_two_app_state, 'physical_state':
        physical_state, 'internal_state': internal_state}
