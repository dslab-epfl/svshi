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
 GA_0_0_2: bool
 GA_0_0_1: bool



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


class Binary_sensor_door_lock_door_lock_sensor():
    def is_on(self, physical_state: PhysicalState, internal_state: InternalState) -> bool:
        """
        pre:
        post: physical_state.GA_0_0_1 == __return__
        """
        return physical_state.GA_0_0_1
    

class Binary_sensor_door_lock_presence_detector():
    def is_on(self, physical_state: PhysicalState, internal_state: InternalState) -> bool:
        """
        pre:
        post: physical_state.GA_0_0_2 == __return__
        """
        return physical_state.GA_0_0_2
    

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
    


svshi_api = SvshiApi()
DOOR_LOCK_DOOR_LOCK_SENSOR = Binary_sensor_door_lock_door_lock_sensor()
DOOR_LOCK_PRESENCE_DETECTOR = Binary_sensor_door_lock_presence_detector()


def door_lock_invariant(door_lock_app_state: AppState, physical_state:
    PhysicalState, internal_state: InternalState) ->bool:
    return True


def door_lock_iteration(door_lock_app_state: AppState, physical_state:
    PhysicalState, internal_state: InternalState):
    """
pre: door_lock_invariant(door_lock_app_state, physical_state, internal_state)
post: door_lock_invariant(**__return__)
"""
    if not DOOR_LOCK_PRESENCE_DETECTOR.is_on(physical_state, internal_state
        ) and not DOOR_LOCK_DOOR_LOCK_SENSOR.is_on(physical_state,
        internal_state):
        if not door_lock_app_state.BOOL_0:
            if door_lock_app_state.INT_0 > 1:
                None
                door_lock_app_state.BOOL_0 = True
            else:
                door_lock_app_state.INT_0 += 1
    else:
        door_lock_app_state.INT_0 = 0
        if door_lock_app_state.BOOL_0:
            if DOOR_LOCK_PRESENCE_DETECTOR.is_on(physical_state, internal_state
                ):
                None
            elif DOOR_LOCK_DOOR_LOCK_SENSOR.is_on(physical_state,
                internal_state):
                None
            door_lock_app_state.BOOL_0 = False
    return {'door_lock_app_state': door_lock_app_state, 'physical_state':
        physical_state, 'internal_state': internal_state}

def system_behaviour(door_lock_app_state: AppState, physical_state:
    PhysicalState, internal_state: InternalState):
    if not DOOR_LOCK_PRESENCE_DETECTOR.is_on(physical_state, internal_state
        ) and not DOOR_LOCK_DOOR_LOCK_SENSOR.is_on(physical_state,
        internal_state):
        if not door_lock_app_state.BOOL_0:
            if door_lock_app_state.INT_0 > 1:
                None
                door_lock_app_state.BOOL_0 = True
            else:
                door_lock_app_state.INT_0 += 1
    else:
        door_lock_app_state.INT_0 = 0
        if door_lock_app_state.BOOL_0:
            if DOOR_LOCK_PRESENCE_DETECTOR.is_on(physical_state, internal_state
                ):
                None
            elif DOOR_LOCK_DOOR_LOCK_SENSOR.is_on(physical_state,
                internal_state):
                None
            door_lock_app_state.BOOL_0 = False
    return {'door_lock_app_state': door_lock_app_state, 'physical_state':
        physical_state, 'internal_state': internal_state}
