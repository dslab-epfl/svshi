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
    


TEST_APP_ONE_BINARY_SENSOR_INSTANCE_NAME = Binary_sensor_test_app_one_binary_sensor_instance_name()
TEST_APP_ONE_SWITCH_INSTANCE_NAME = Switch_test_app_one_switch_instance_name()
TEST_APP_TWO_TEMPERATURE_SENSOR = Temperature_sensor_test_app_two_temperature_sensor()


def test_app_two_invariant(app_state: AppState, physical_state: PhysicalState
    ) ->bool:
    return True


def test_app_two_iteration(app_state: AppState, physical_state: PhysicalState):
    """
pre: test_app_one_invariant(app_state, physical_state)
pre: test_app_two_invariant(app_state, physical_state)
post: test_app_one_invariant(**__return__)
post: test_app_two_invariant(**__return__)
"""
    if TEST_APP_TWO_TEMPERATURE_SENSOR.read(physical_state
        ) != None and TEST_APP_TWO_TEMPERATURE_SENSOR.read(physical_state
        ) > 22:
        test_app_two_unchecked_send_notif()
    return {'app_state': app_state, 'physical_state': physical_state}


def test_app_two_unchecked_send_notif() ->None:
    a = 1 + 1

def test_app_one_invariant(app_state: AppState, physical_state: PhysicalState
    ) ->bool:
    return (TEST_APP_ONE_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state) or
        app_state.INT_0 == 42) and TEST_APP_ONE_SWITCH_INSTANCE_NAME.is_on(
        physical_state) or not (TEST_APP_ONE_BINARY_SENSOR_INSTANCE_NAME.
        is_on(physical_state) or app_state.INT_0 == 42
        ) and not TEST_APP_ONE_SWITCH_INSTANCE_NAME.is_on(physical_state)


def test_app_one_iteration(app_state: AppState, physical_state: PhysicalState):
    """
pre: test_app_one_invariant(app_state, physical_state)
pre: test_app_two_invariant(app_state, physical_state)
post: test_app_one_invariant(**__return__)
post: test_app_two_invariant(**__return__)
"""
    if TEST_APP_ONE_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state
        ) or app_state.INT_0 == 42:
        test_app_one_unchecked_send_email('test@test.com')
        TEST_APP_ONE_SWITCH_INSTANCE_NAME.on(physical_state)
    else:
        TEST_APP_ONE_SWITCH_INSTANCE_NAME.off(physical_state)
    return {'app_state': app_state, 'physical_state': physical_state}


def test_app_one_unchecked_send_email(addr: str) ->None:
    a = 1 + 1
