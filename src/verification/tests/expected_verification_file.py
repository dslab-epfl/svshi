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
 GA_0_0_1: bool
 GA_0_0_2: bool
 GA_0_0_3: float
 GA_0_0_4: float
 GA_0_0_5: int



@dataclasses.dataclass
class InternalState:
 """
 inv: self.time>=0
 """
 time: int #time in seconds


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
        get_hour_of_the_day(internal_state) <= 3)


def third_app_iteration(first_app_app_state: AppState, second_app_app_state:
    AppState, third_app_app_state: AppState, physical_state: PhysicalState,
    internal_state: InternalState):
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
        another_file = '/third_app/file2.csv'
        THIRD_APP_SWITCH_INSTANCE_NAME.on(physical_state, internal_state)
    elif 2 <= svshi_api.get_hour_of_the_day(internal_state) <= 3:
        t = svshi_api.get_time(internal_state)
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
    internal_state: InternalState, first_app_uncheckedcompute_bool: bool,
    first_app_unchecked_return_two: int):
    """
pre: first_app_invariant(first_app_app_state, second_app_app_state, third_app_app_state, physical_state, internal_state)
pre: second_app_invariant(first_app_app_state, second_app_app_state, third_app_app_state, physical_state, internal_state)
pre: third_app_invariant(first_app_app_state, second_app_app_state, third_app_app_state, physical_state, internal_state)
pre: first_app_uncheckedcompute_bool == False
pre: first_app_unchecked_return_two > 0
pre: first_app_unchecked_return_two != 3
post: first_app_invariant(**__return__)
post: second_app_invariant(**__return__)
post: third_app_invariant(**__return__)
"""
    if first_app_uncheckedcompute_bool and not first_app_app_state.BOOL_1:
        first_app_app_state.INT_1 = 42
        None
    else:
        v = first_app_unchecked_return_two
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
        physical_state, internal_state)


def second_app_iteration(first_app_app_state: AppState,
    second_app_app_state: AppState, third_app_app_state: AppState,
    physical_state: PhysicalState, internal_state: InternalState,
    second_app_unchecked_time: float):
    """
pre: first_app_invariant(first_app_app_state, second_app_app_state, third_app_app_state, physical_state, internal_state)
pre: second_app_invariant(first_app_app_state, second_app_app_state, third_app_app_state, physical_state, internal_state)
pre: third_app_invariant(first_app_app_state, second_app_app_state, third_app_app_state, physical_state, internal_state)
post: first_app_invariant(**__return__)
post: second_app_invariant(**__return__)
post: third_app_invariant(**__return__)
"""
    if SECOND_APP_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state,
        internal_state) and second_app_unchecked_time > 2.0:
        SECOND_APP_SWITCH_INSTANCE_NAME.on(physical_state, internal_state)
    return {'first_app_app_state': first_app_app_state,
        'second_app_app_state': second_app_app_state, 'third_app_app_state':
        third_app_app_state, 'physical_state': physical_state,
        'internal_state': internal_state}

def system_behaviour(first_app_app_state: AppState, second_app_app_state:
    AppState, third_app_app_state: AppState, physical_state: PhysicalState,
    internal_state: InternalState, first_app_uncheckedcompute_bool: bool,
    first_app_unchecked_return_two: int, second_app_unchecked_time: float):
    if first_app_uncheckedcompute_bool and not first_app_app_state.BOOL_1:
        first_app_app_state.INT_1 = 42
        None
    else:
        v = first_app_unchecked_return_two
        None
        None
    if THIRD_APP_HUMIDITY_SENSOR_INSTANCE_NAME.read(physical_state,
        internal_state) > 30 and THIRD_APP_CO_TWO_SENSOR_INSTANCE_NAME.read(
        physical_state, internal_state) > 600.0:
        another_file = '/third_app/file2.csv'
        THIRD_APP_SWITCH_INSTANCE_NAME.on(physical_state, internal_state)
    elif 2 <= svshi_api.get_hour_of_the_day(internal_state) <= 3:
        t = svshi_api.get_time(internal_state)
        THIRD_APP_SWITCH_INSTANCE_NAME.off(physical_state, internal_state)
    if SECOND_APP_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state,
        internal_state) and second_app_unchecked_time > 2.0:
        SECOND_APP_SWITCH_INSTANCE_NAME.on(physical_state, internal_state)
    return {'first_app_app_state': first_app_app_state,
        'second_app_app_state': second_app_app_state, 'third_app_app_state':
        third_app_app_state, 'physical_state': physical_state,
        'internal_state': internal_state}
