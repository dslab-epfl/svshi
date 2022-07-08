from slack_sdk.web.client import WebClient
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



@dataclasses.dataclass
class IsolatedFunctionsValues:
    door_lock_on_trigger_send_message: Optional[None] = None



@dataclasses.dataclass
class InternalState:
    date_time: time.struct_time # time object, at local machine time
    app_files_runtime_folder_path: str # path to the folder in which files used by apps are stored at runtime


class Binary_sensor_door_lock_door_lock_sensor():
    def is_on(self, physical_state: PhysicalState) -> bool:
        """
        pre:
        post: physical_state.GA_0_0_1 == __return__
        """
        return physical_state.GA_0_0_1


class Binary_sensor_door_lock_presence_detector():
    def is_on(self, physical_state: PhysicalState) -> bool:
        """
        pre:
        post: physical_state.GA_0_0_2 == __return__
        """
        return physical_state.GA_0_0_2


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
DOOR_LOCK_DOOR_LOCK_SENSOR = Binary_sensor_door_lock_door_lock_sensor()
DOOR_LOCK_PRESENCE_DETECTOR = Binary_sensor_door_lock_presence_detector()


def door_lock_invariant(door_lock_app_state: AppState, physical_state:
    PhysicalState, internal_state: InternalState) ->bool:
    return True


def door_lock_iteration(door_lock_app_state: AppState, physical_state:
    PhysicalState, internal_state: InternalState, isolated_fn_values:
    IsolatedFunctionsValues):
    if not DOOR_LOCK_PRESENCE_DETECTOR.is_on(physical_state
        ) and not DOOR_LOCK_DOOR_LOCK_SENSOR.is_on(physical_state):
        if not door_lock_app_state.BOOL_0:
            if door_lock_app_state.INT_0 > 1:
                svshi_api.trigger_if_not_running(
                    door_lock_on_trigger_send_message)(
                    'The door at office INN319 is still opened but nobody is there!'
                    )
                door_lock_app_state.BOOL_0 = True
            else:
                door_lock_app_state.INT_0 += 1
    else:
        door_lock_app_state.INT_0 = 0
        if door_lock_app_state.BOOL_0:
            if DOOR_LOCK_PRESENCE_DETECTOR.is_on(physical_state):
                svshi_api.trigger_if_not_running(
                    door_lock_on_trigger_send_message)(
                    'Someone entered the office INN319. All good!')
            elif DOOR_LOCK_DOOR_LOCK_SENSOR.is_on(physical_state):
                svshi_api.trigger_if_not_running(
                    door_lock_on_trigger_send_message)(
                    'Someone locked the door of the office INN319. All good!')
            door_lock_app_state.BOOL_0 = False


def door_lock_on_trigger_send_message(msg: str, internal_state: InternalState
    ) ->None:
    token = 'xoxb-2702504146389-2876497796775-r21j0QnaGcyfjwEVDFrYpkYO'
    slack_client = WebClient(token=token)
    slack_client.chat_postMessage(channel='inn319', text=msg)

def system_behaviour(door_lock_app_state: AppState, physical_state:
    PhysicalState, internal_state: InternalState, isolated_fn_values:
    IsolatedFunctionsValues):
    if not DOOR_LOCK_PRESENCE_DETECTOR.is_on(physical_state
        ) and not DOOR_LOCK_DOOR_LOCK_SENSOR.is_on(physical_state):
        if not door_lock_app_state.BOOL_0:
            if door_lock_app_state.INT_0 > 1:
                svshi_api.trigger_if_not_running(
                    door_lock_on_trigger_send_message)(
                    'The door at office INN319 is still opened but nobody is there!'
                    )
                door_lock_app_state.BOOL_0 = True
            else:
                door_lock_app_state.INT_0 += 1
    else:
        door_lock_app_state.INT_0 = 0
        if door_lock_app_state.BOOL_0:
            if DOOR_LOCK_PRESENCE_DETECTOR.is_on(physical_state):
                svshi_api.trigger_if_not_running(
                    door_lock_on_trigger_send_message)(
                    'Someone entered the office INN319. All good!')
            elif DOOR_LOCK_DOOR_LOCK_SENSOR.is_on(physical_state):
                svshi_api.trigger_if_not_running(
                    door_lock_on_trigger_send_message)(
                    'Someone locked the door of the office INN319. All good!')
            door_lock_app_state.BOOL_0 = False
