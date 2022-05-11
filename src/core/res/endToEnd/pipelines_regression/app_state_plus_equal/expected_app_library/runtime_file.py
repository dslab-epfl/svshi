from slack_sdk.web.client import WebClient
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
 GA_0_0_2: bool
 GA_0_0_1: bool



@dataclasses.dataclass
class InternalState:
 """
 inv: self.time>=0
 """
 time: int #time in seconds


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
DOOR_LOCK_DOOR_LOCK_SENSOR = Binary_sensor_door_lock_door_lock_sensor()
DOOR_LOCK_PRESENCE_DETECTOR = Binary_sensor_door_lock_presence_detector()


def door_lock_invariant(door_lock_app_state: AppState, physical_state:
    PhysicalState, internal_state: InternalState) ->bool:
    return True


def door_lock_iteration(door_lock_app_state: AppState, physical_state:
    PhysicalState, internal_state: InternalState):
    if not DOOR_LOCK_PRESENCE_DETECTOR.is_on(physical_state
        ) and not DOOR_LOCK_DOOR_LOCK_SENSOR.is_on(physical_state):
        if not door_lock_app_state.BOOL_0:
            if door_lock_app_state.INT_0 > 1:
                door_lock_unchecked_send_message(
                    'The door at office INN319 is still opened but nobody is there!'
                    )
                door_lock_app_state.BOOL_0 = True
            else:
                door_lock_app_state.INT_0 += 1
    else:
        door_lock_app_state.INT_0 = 0
        if door_lock_app_state.BOOL_0:
            if DOOR_LOCK_PRESENCE_DETECTOR.is_on(physical_state):
                door_lock_unchecked_send_message(
                    'Someone entered the office INN319. All good!')
            elif DOOR_LOCK_DOOR_LOCK_SENSOR.is_on(physical_state):
                door_lock_unchecked_send_message(
                    'Someone locked the door of the office INN319. All good!')
            door_lock_app_state.BOOL_0 = False


def door_lock_unchecked_send_message(msg: str) ->None:
    token = 'xoxb-2702504146389-2876497796775-r21j0QnaGcyfjwEVDFrYpkYO'
    slack_client = WebClient(token=token)
    slack_client.chat_postMessage(channel='inn319', text=msg)

def system_behaviour(door_lock_app_state: AppState, physical_state:
    PhysicalState, internal_state: InternalState):
    if not DOOR_LOCK_PRESENCE_DETECTOR.is_on(physical_state
        ) and not DOOR_LOCK_DOOR_LOCK_SENSOR.is_on(physical_state):
        if not door_lock_app_state.BOOL_0:
            if door_lock_app_state.INT_0 > 1:
                door_lock_unchecked_send_message(
                    'The door at office INN319 is still opened but nobody is there!'
                    )
                door_lock_app_state.BOOL_0 = True
            else:
                door_lock_app_state.INT_0 += 1
    else:
        door_lock_app_state.INT_0 = 0
        if door_lock_app_state.BOOL_0:
            if DOOR_LOCK_PRESENCE_DETECTOR.is_on(physical_state):
                door_lock_unchecked_send_message(
                    'Someone entered the office INN319. All good!')
            elif DOOR_LOCK_DOOR_LOCK_SENSOR.is_on(physical_state):
                door_lock_unchecked_send_message(
                    'Someone locked the door of the office INN319. All good!')
            door_lock_app_state.BOOL_0 = False
