from decouple import config
from slack_sdk.web.client import WebClient
from slack_sdk.web.slack_response import SlackResponse
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
 GA_0_0_1: bool
 GA_0_0_2: bool
 GA_0_0_3: float
 GA_0_0_4: float
 GA_0_0_5: int



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


def third_app_invariant(third_app_app_state: AppState, physical_state:
    PhysicalState, internal_state: InternalState) ->bool:
    return THIRD_APP_HUMIDITY_SENSOR_INSTANCE_NAME.read(physical_state
        ) < 82 and (2 <= svshi_api.get_hour_of_the_day(internal_state) <= 3 and
        not THIRD_APP_SWITCH_INSTANCE_NAME.is_on(physical_state) or not 2 <=
        svshi_api.get_hour_of_the_day(internal_state) <= 3)


def third_app_iteration(third_app_app_state: AppState, physical_state:
    PhysicalState, internal_state: InternalState):
    if THIRD_APP_HUMIDITY_SENSOR_INSTANCE_NAME.read(physical_state
        ) > 30 and THIRD_APP_CO_TWO_SENSOR_INSTANCE_NAME.read(physical_state
        ) > 600.0:
        another_file = 'file2.csv'
        THIRD_APP_SWITCH_INSTANCE_NAME.on(physical_state)
    elif 2 <= svshi_api.get_hour_of_the_day(internal_state) <= 3:
        t = svshi_api.get_minute_in_hour(internal_state)
        THIRD_APP_SWITCH_INSTANCE_NAME.off(physical_state)

def first_app_invariant(first_app_app_state: AppState, physical_state:
    PhysicalState, internal_state: InternalState) ->bool:
    return FIRST_APP_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state
        ) and FIRST_APP_TEMPERATURE_SENSOR_INSTANCE_NAME.read(physical_state
        ) > 18 and not first_app_app_state.BOOL_1


def first_app_iteration(first_app_app_state: AppState, physical_state:
    PhysicalState, internal_state: InternalState):
    if first_app_uncheckedcompute_bool(internal_state
        ) and not first_app_app_state.BOOL_1:
        first_app_app_state.INT_1 = 42
        first_app_unchecked_print(FIRST_APP_BINARY_SENSOR_INSTANCE_NAME.
            is_on(physical_state), internal_state)
    else:
        v = first_app_unchecked_return_two(internal_state)
        first_app_unchecked_print(v, internal_state)
        first_app_unchecked_print('file4.csv', internal_state)


def first_app_uncheckedcompute_bool(internal_state: InternalState) ->bool:
    """
    post: __return__ == False
    """
    return False


def first_app_unchecked_return_two(internal_state: InternalState) ->int:
    """
    pre: True
    post: __return__ > 0
    post: __return__ != 3
    """
    p = svshi_api.get_file_path('first_app', 'file1.txt', internal_state)
    f = svshi_api.get_file_text_mode('first_app', 'file2.txt', 'w',
        internal_state)
    f2 = svshi_api.get_file_binary_mode('first_app', 'file3.txt', 'ar',
        internal_state)
    return 2


def first_app_unchecked_print(s, internal_state: InternalState) ->None:
    print(s)

def second_app_invariant(second_app_app_state: AppState, physical_state:
    PhysicalState, internal_state: InternalState) ->bool:
    return SECOND_APP_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state
        ) and SECOND_APP_SWITCH_INSTANCE_NAME.is_on(physical_state)


def second_app_iteration(second_app_app_state: AppState, physical_state:
    PhysicalState, internal_state: InternalState):
    if SECOND_APP_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state
        ) and second_app_unchecked_time(internal_state) > 2.0:
        SECOND_APP_SWITCH_INSTANCE_NAME.on(physical_state)


def second_app_unchecked_time(internal_state: InternalState) ->float:
    return time.time()

def system_behaviour(first_app_app_state: AppState, second_app_app_state:
    AppState, third_app_app_state: AppState, physical_state: PhysicalState,
    internal_state: InternalState):
    if first_app_uncheckedcompute_bool(internal_state
        ) and not first_app_app_state.BOOL_1:
        first_app_app_state.INT_1 = 42
        first_app_unchecked_print(FIRST_APP_BINARY_SENSOR_INSTANCE_NAME.
            is_on(physical_state), internal_state)
    else:
        v = first_app_unchecked_return_two(internal_state)
        first_app_unchecked_print(v, internal_state)
        first_app_unchecked_print('file4.csv', internal_state)
    if THIRD_APP_HUMIDITY_SENSOR_INSTANCE_NAME.read(physical_state
        ) > 30 and THIRD_APP_CO_TWO_SENSOR_INSTANCE_NAME.read(physical_state
        ) > 600.0:
        another_file = 'file2.csv'
        THIRD_APP_SWITCH_INSTANCE_NAME.on(physical_state)
    elif 2 <= svshi_api.get_hour_of_the_day(internal_state) <= 3:
        t = svshi_api.get_minute_in_hour(internal_state)
        THIRD_APP_SWITCH_INSTANCE_NAME.off(physical_state)
    if SECOND_APP_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state
        ) and second_app_unchecked_time(internal_state) > 2.0:
        SECOND_APP_SWITCH_INSTANCE_NAME.on(physical_state)
