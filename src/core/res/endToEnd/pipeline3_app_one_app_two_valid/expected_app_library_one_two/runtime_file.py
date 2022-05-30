from typing import IO, Optional
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
class InternalState:
    date_time: time.struct_time # time object, at local machine time
    app_files_runtime_folder_path: str # path to the folder in which files used by apps are stored at runtime


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
TEST_APP_ONE_BINARY_SENSOR_INSTANCE_NAME = Binary_sensor_test_app_one_binary_sensor_instance_name()
TEST_APP_ONE_SWITCH_INSTANCE_NAME = Switch_test_app_one_switch_instance_name()
TEST_APP_TWO_TEMPERATURE_SENSOR = Temperature_sensor_test_app_two_temperature_sensor()


def test_app_two_invariant(test_app_two_app_state: AppState, physical_state:
    PhysicalState, internal_state: InternalState) ->bool:
    return True


def test_app_two_iteration(test_app_two_app_state: AppState, physical_state:
    PhysicalState, internal_state: InternalState):
    if TEST_APP_TWO_TEMPERATURE_SENSOR.read(physical_state
        ) != None and TEST_APP_TWO_TEMPERATURE_SENSOR.read(physical_state
        ) > 22:
        test_app_two_unchecked_send_notif(internal_state)


def test_app_two_unchecked_send_notif(internal_state: InternalState) ->None:
    a = 1 + 1

def test_app_one_invariant(test_app_one_app_state: AppState, physical_state:
    PhysicalState, internal_state: InternalState) ->bool:
    return (TEST_APP_ONE_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state) or
        test_app_one_app_state.INT_0 == 42
        ) and TEST_APP_ONE_SWITCH_INSTANCE_NAME.is_on(physical_state) or not (
        TEST_APP_ONE_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state) or 
        test_app_one_app_state.INT_0 == 42
        ) and not TEST_APP_ONE_SWITCH_INSTANCE_NAME.is_on(physical_state)


def test_app_one_iteration(test_app_one_app_state: AppState, physical_state:
    PhysicalState, internal_state: InternalState):
    if TEST_APP_ONE_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state
        ) or test_app_one_app_state.INT_0 == 42:
        test_app_one_unchecked_send_email('test@test.com', internal_state)
        TEST_APP_ONE_SWITCH_INSTANCE_NAME.on(physical_state)
    else:
        TEST_APP_ONE_SWITCH_INSTANCE_NAME.off(physical_state)


def test_app_one_unchecked_send_email(addr: str, internal_state: InternalState
    ) ->None:
    a = 1 + 1

def system_behaviour(test_app_one_app_state: AppState,
    test_app_two_app_state: AppState, physical_state: PhysicalState,
    internal_state: InternalState):
    if TEST_APP_ONE_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state
        ) or test_app_one_app_state.INT_0 == 42:
        test_app_one_unchecked_send_email('test@test.com', internal_state)
        TEST_APP_ONE_SWITCH_INSTANCE_NAME.on(physical_state)
    else:
        TEST_APP_ONE_SWITCH_INSTANCE_NAME.off(physical_state)
    if TEST_APP_TWO_TEMPERATURE_SENSOR.read(physical_state
        ) != None and TEST_APP_TWO_TEMPERATURE_SENSOR.read(physical_state
        ) > 22:
        test_app_two_unchecked_send_notif(internal_state)
