from typing import Callable, IO, Optional, Protocol
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
    GA_1_1_1: bool
    GA_1_1_2: bool
    GA_1_1_3: int
    GA_1_1_4: int



@dataclasses.dataclass
class IsolatedFunctionsValues:
    app_on_trigger_write: Optional[int] = None
    another_app_periodic_write: Optional[float] = None



@dataclasses.dataclass
class InternalState:
    date_time: time.struct_time # time object, at local machine time
    app_files_runtime_folder_path: str # path to the folder in which files used by apps are stored at runtime


class Binary_sensor_app_binary_sensor_instance_name():
    def is_on(self, physical_state: PhysicalState) -> bool:
        """
        pre:
        post: physical_state.GA_1_1_1 == __return__
        """
        return physical_state.GA_1_1_1


class Switch_app_switch_instance_name():
    def on(self, physical_state: PhysicalState):
        """
        pre: 
        post: physical_state.GA_1_1_1  == True
        """
        physical_state.GA_1_1_1 = True

    def off(self, physical_state: PhysicalState):
        """
        pre: 
        post: physical_state.GA_1_1_1  == False
        """
        physical_state.GA_1_1_1 = False

    def is_on(self, physical_state: PhysicalState) -> bool:
        """
        pre: 
        post: physical_state.GA_1_1_1  == __return__
        """
        return physical_state.GA_1_1_1


class Temperature_sensor_app_temperature_sensor_instance_name():
    def read(self, physical_state: PhysicalState) -> float:
        """
        pre:
        post: physical_state.GA_1_1_1 == __return__
        """
        return physical_state.GA_1_1_1


class Humidity_sensor_app_humidity_sensor_instance_name():
    def read(self, physical_state: PhysicalState) -> float:
        """
        pre:
        post: physical_state.GA_1_1_1 == __return__
        """
        return physical_state.GA_1_1_1


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

    class OnTriggerConsumer(Protocol):
        """Type alias for the on_trigger consumer."""
        def __call__(self, on_trigger_fn: Callable, *args, **kwargs) -> None: ...

    def __not_implemented_consumer(self, fn: Callable, *args, **kwargs):
        raise NotImplementedError(
            "on_trigger_consumer was called before being initialized."
        )

    __on_trigger_consumer: OnTriggerConsumer = __not_implemented_consumer

    def register_on_trigger_consumer(self, on_trigger_consumer: OnTriggerConsumer):
        self.__on_trigger_consumer = on_trigger_consumer

    def trigger_if_not_running(
        self, on_trigger_function: Callable, *args, **kwargs
    ) -> None:
        self.__on_trigger_consumer(on_trigger_function, *args, **kwargs)

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
APP_BINARY_SENSOR_INSTANCE_NAME = Binary_sensor_app_binary_sensor_instance_name()
APP_SWITCH_INSTANCE_NAME = Switch_app_switch_instance_name()
APP_TEMPERATURE_SENSOR_INSTANCE_NAME = Temperature_sensor_app_temperature_sensor_instance_name()
APP_HUMIDITY_SENSOR_INSTANCE_NAME = Humidity_sensor_app_humidity_sensor_instance_name()


def app_invariant(physical_state: PhysicalState) ->bool:
    return True


def app_iteration(app_app_state: AppState, physical_state: PhysicalState,
    internal_state: InternalState, isolated_fn_values: IsolatedFunctionsValues):
    """
pre: app_invariant(physical_state)
pre: another_app_invariant(physical_state)
post: app_invariant(__return__)
post: another_app_invariant(__return__)
"""
    svshi_api.trigger_if_not_running(app_on_trigger_write, "123\n")
    return physical_state

def app_on_trigger_write(txt: str, internal_state: InternalState) ->int:
    with open("app_tmp.txt", "a") as f:
        f.write(txt)
    try:
        return int(txt)
    except Exception:
        return 2

def another_app_invariant(physical_state: PhysicalState) ->bool:
    return True


def another_app_iteration(another_app_app_state: AppState, physical_state:
    PhysicalState, internal_state: InternalState, isolated_fn_values:
    IsolatedFunctionsValues):
    """
pre: app_invariant(physical_state)
pre: another_app_invariant(physical_state)
post: app_invariant(__return__)
post: another_app_invariant(__return__)
"""
    print('Hello, world!')
    return physical_state

def another_app_periodic_write(internal_state: InternalState) ->float:
    """period: 0"""
    with open("another_app_tmp.txt", "a") as f:
        f.write("Hello, world!\n")
    return 3.2

def system_behaviour(app_app_state: AppState, another_app_app_state: AppState,
    physical_state: PhysicalState, internal_state: InternalState,
    isolated_fn_values: IsolatedFunctionsValues):
    print('Hello, world!')
    svshi_api.trigger_if_not_running(app_on_trigger_write, "123\n")
    return physical_state
