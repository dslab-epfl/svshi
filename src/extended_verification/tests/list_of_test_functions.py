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


@dataclasses.dataclass
class PhysicalState:
    GA_0_0_1: bool
    GA_0_0_2: bool
    GA_0_0_3: int
    GA_0_0_4: float



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
    time_min: int
    time_hour: int
    time_day: int
    time_weekday: int
    time_month: int
    time_year: int
    c0:int

class Binary_sensor_app_one_binary_sensor_instance_name():
    def is_on(self, physical_state: PhysicalState, internal_state: InternalState) -> bool:
        """
        pre:
        post: physical_state.GA_0_0_1 == __return__
        """
        return physical_state.GA_0_0_1


class an_int_device():
    def set(self, v:int,physical_state: PhysicalState, internal_state: InternalState):

        physical_state.GA_0_0_3 = v

    def read(self, physical_state: PhysicalState, internal_state: InternalState):

        return physical_state.GA_0_0_3

class a_float_device():
    def set(self, v:float,physical_state: PhysicalState, internal_state: InternalState):

        physical_state.GA_0_0_4 = v
    def read(self, physical_state: PhysicalState, internal_state: InternalState):

        return physical_state.GA_0_0_4




class Switch_app_one_switch_instance_name():
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
    def dummy_check(self,i: InternalState,v:int) -> bool:
        return i.c0 == v

    def check_time_property(self,a,b,c) -> bool:
        ...



svshi_api = SvshiApi()
APP_ONE_BINARY_SENSOR_INSTANCE_NAME = Binary_sensor_app_one_binary_sensor_instance_name()
APP_ONE_SWITCH_INSTANCE_NAME = Switch_app_one_switch_instance_name()
float_dev = a_float_device()
int_dev = an_int_device()
boiler = float_dev
solar_heater = int_dev
solar_heater_circulator = Switch_app_one_switch_instance_name()




def app_one_invariant(app_one_app_state: AppState, physical_state:
PhysicalState, internal_state: InternalState) ->bool:
    u=False
    if svshi_api.check_time_property(Day(1),Hour(10),APP_ONE_SWITCH_INSTANCE_NAME.is_on(physical_state, internal_state)):
        u = True
    if 10<= svshi_api.get_hour_of_the_day(internal_state):
        if APP_ONE_SWITCH_INSTANCE_NAME.is_on(physical_state, internal_state):
                return u
    else:
        return u





def app_one_iteration(app_one_app_state: AppState, physical_state:
PhysicalState, internal_state: InternalState):

    if 10<= svshi_api.get_hour_of_the_day(internal_state) :
        APP_ONE_SWITCH_INSTANCE_NAME.on(physical_state, internal_state)
    else:
        APP_ONE_SWITCH_INSTANCE_NAME.off(physical_state, internal_state)
    return {'app_one_app_state': app_one_app_state,
            'physical_state': physical_state, 'internal_state': internal_state}

def app_two_invariant(app_one_app_state: AppState, physical_state:
PhysicalState, internal_state: InternalState) ->bool:

    return svshi_api.check_time_property(Day(1),Hour(10),APP_ONE_SWITCH_INSTANCE_NAME.is_on(physical_state, internal_state))

def app_two_invariant_no_check(app_one_app_state: AppState, physical_state:
PhysicalState, internal_state: InternalState) ->bool:

    if 10<= svshi_api.get_hour_of_the_day(internal_state):
        return APP_ONE_SWITCH_INSTANCE_NAME.is_on(physical_state, internal_state)
    else:
        return True


def app_two_iteration(app_one_app_state: AppState, physical_state:
PhysicalState, internal_state: InternalState):

    if 10<= svshi_api.get_hour_of_the_day(internal_state) and APP_ONE_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state, internal_state) :
        APP_ONE_SWITCH_INSTANCE_NAME.on(physical_state, internal_state)
    else:
        APP_ONE_SWITCH_INSTANCE_NAME.off(physical_state, internal_state)
    return {'app_one_app_state': app_one_app_state,
            'physical_state': physical_state, 'internal_state': internal_state}


def app_three_invariant(app_one_app_state: AppState, physical_state:
PhysicalState, internal_state: InternalState) ->bool:
    ab = APP_ONE_SWITCH_INSTANCE_NAME.is_on(physical_state, internal_state)

    if svshi_api.check_time_property(Day(1),Hour(10),ab):
        return True


def invariant_test_one(app_one_app_state: AppState, physical_state:
PhysicalState, internal_state: InternalState) ->bool:
    u= svshi_api.check_time_property(Day(1),Hour(10),APP_ONE_SWITCH_INSTANCE_NAME.is_on(physical_state, internal_state))
    if (10<= svshi_api.get_hour_of_the_day(internal_state) and APP_ONE_SWITCH_INSTANCE_NAME.is_on(physical_state, internal_state)):
        return u
    else:
        return True


def invariant_test_line_return(app_one_app_state: AppState, physical_state:
PhysicalState, internal_state: InternalState) ->bool:

    return svshi_api.check_time_property(Day(1),Hour(10),
                                 APP_ONE_SWITCH_INSTANCE_NAME.is_on(physical_state, internal_state)
                                 or
                                 APP_ONE_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state, internal_state))



def app_three_iteration(app_one_app_state: AppState, physical_state:
PhysicalState, internal_state: InternalState):

    if 10<= svshi_api.get_hour_of_the_day(internal_state) and APP_ONE_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state, internal_state) :
        APP_ONE_SWITCH_INSTANCE_NAME.on(physical_state, internal_state)
    else:
        APP_ONE_SWITCH_INSTANCE_NAME.off(physical_state, internal_state)
    return {'app_one_app_state': app_one_app_state,
            'physical_state': physical_state, 'internal_state': internal_state}


def switch_on(app_one_app_state: AppState, physical_state:
PhysicalState, internal_state: InternalState):

    APP_ONE_SWITCH_INSTANCE_NAME.on(physical_state, internal_state)

    return {'app_one_app_state': app_one_app_state,
            'physical_state': physical_state, 'internal_state': internal_state}


def switch_off(app_one_app_state: AppState, physical_state:
PhysicalState, internal_state: InternalState):

    APP_ONE_SWITCH_INSTANCE_NAME.off(physical_state, internal_state)

    return {'app_one_app_state': app_one_app_state,
            'physical_state': physical_state, 'internal_state': internal_state}


def check_empty_fun_on(app_one_app_state: AppState, physical_state:
PhysicalState, internal_state: InternalState) ->bool:

    if APP_ONE_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state, internal_state):
        return True

def check_empty_fun_off(app_one_app_state: AppState, physical_state:
PhysicalState, internal_state: InternalState) ->bool:

    return APP_ONE_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state, internal_state)

def empty_fun(app_one_app_state: AppState, physical_state:
PhysicalState, internal_state: InternalState):

    pass

    return {'app_one_app_state': app_one_app_state,
            'physical_state': physical_state, 'internal_state': internal_state}



def app_test_float(app_one_app_state: AppState, physical_state:
PhysicalState, internal_state: InternalState):
    float_dev.set(12.0,physical_state, internal_state)

    return {'app_one_app_state': app_one_app_state,
            'physical_state': physical_state, 'internal_state': internal_state}

def app_test_run_crosshair(app_one_app_state: AppState, physical_state:
PhysicalState, internal_state: InternalState):
    float_dev.set(12.0,physical_state, internal_state)
    int_dev.set(app_one_app_state.INT_0,physical_state, internal_state)

    return {'app_one_app_state': app_one_app_state,
            'physical_state': physical_state, 'internal_state': internal_state}

def app_test_float_depends_on_app_state_var(app_one_app_state: AppState, physical_state:
PhysicalState, internal_state: InternalState):
    float_dev.set(12.0*app_one_app_state.FLOAT_0,physical_state, internal_state)

    return {'app_one_app_state': app_one_app_state,
            'physical_state': physical_state, 'internal_state': internal_state}


def boiler_inv(app_one_app_state: AppState, physical_state:
PhysicalState, internal_state: InternalState) ->bool:

    return svshi_api.check_time_property(Day(1),Hour(1),float_dev.read(physical_state, internal_state)>60.0)

def solar_boiler_app_compliant(app_one_app_state: AppState, physical_state:
PhysicalState, internal_state: InternalState):
    if 21<=svshi_api.get_hour_of_the_day(internal_state) <=23:
        solar_heater_circulator.off(physical_state, internal_state)
        boiler.set(65.0,physical_state, internal_state) #GA_0_0_4
    elif solar_heater.read(physical_state,internal_state) > 45:
        solar_heater_circulator.on(physical_state, internal_state)
        boiler.set(0.0,physical_state, internal_state)
    else:
        solar_heater_circulator.off(physical_state, internal_state)
        boiler.set(45.0,physical_state, internal_state) #GA_0_0_4

    return {'app_one_app_state': app_one_app_state,
            'physical_state': physical_state, 'internal_state': internal_state}

def solar_boiler_app(app_one_app_state: AppState, physical_state:
PhysicalState, internal_state: InternalState):
    if solar_heater.read(physical_state,internal_state) > 45:
        solar_heater_circulator.on(physical_state, internal_state)
        boiler.set(float(solar_heater.read(physical_state, internal_state)),physical_state, internal_state)  #GA_0_0_4
    else:
        solar_heater_circulator.off(physical_state, internal_state)
        boiler.set(45.0,physical_state, internal_state)

    return {'app_one_app_state': app_one_app_state,
            'physical_state': physical_state, 'internal_state': internal_state}


def solar_boiler_app_evil(app_one_app_state: AppState, physical_state:
PhysicalState, internal_state: InternalState):
    if 21<=svshi_api.get_hour_of_the_day(internal_state) :
        float_dev.set(60.0,physical_state, internal_state) #GA_0_0_4
    else:
        float_dev.set(float(int_dev.read(physical_state, internal_state))-5.0,physical_state, internal_state)

    return {'physical_state': physical_state}

def solar_boiler_app_evile(app_one_app_state: AppState, physical_state:
PhysicalState, internal_state: InternalState):
    if 21<=svshi_api.get_hour_of_the_day(internal_state) <=23:
        if svshi_api.get_minute_in_hour(internal_state) ==1:
            float_dev.set(0.0,physical_state, internal_state) #GA_0_0_4
        else:
            float_dev.set(65.0,physical_state, internal_state) #GA_0_0_4
    else:
        float_dev.set(float(int_dev.read(physical_state, internal_state))-5.0,physical_state, internal_state)

    return {'app_one_app_state': app_one_app_state,
            'physical_state': physical_state, 'internal_state': internal_state}


def check_on_float_sat(app_one_app_state: AppState, physical_state:
PhysicalState, internal_state: InternalState) ->bool:

    if svshi_api.check_time_property(Day(10),Hour(2),float_dev.read(physical_state, internal_state)>=10):
        return True
def inv_on_float_sat(app_one_app_state: AppState, physical_state:
PhysicalState, internal_state: InternalState) ->bool:

    if float_dev.read(physical_state, internal_state)>=11 :
        return True

def check_on_float_unsat(app_one_app_state: AppState, physical_state:
PhysicalState, internal_state: InternalState) ->bool:

    if svshi_api.check_time_property(Day(10),Hour(2),float_dev.read(physical_state, internal_state)>=14):
        return True
def inv_on_float_unsat(app_one_app_state: AppState, physical_state:
PhysicalState, internal_state: InternalState) ->bool:

    if float_dev.read(physical_state, internal_state)>=14 :
        return True

def inv_switch_off(app_one_app_state: AppState, physical_state:
PhysicalState, internal_state: InternalState) ->bool:
    if not APP_ONE_SWITCH_INSTANCE_NAME.is_on(physical_state, internal_state):
        return True


def multiple_check_sat(app_one_app_state: AppState, physical_state:
PhysicalState, internal_state: InternalState) ->bool:

    return svshi_api.check_time_property(Day(10),Hour(2),float_dev.read(physical_state, internal_state)>=41) or svshi_api.check_time_property(Day(2),Hour(2),float_dev.read(physical_state, internal_state)>=11)

def multiple_check_unsat(app_one_app_state: AppState, physical_state:
PhysicalState, internal_state: InternalState) ->bool:

    return svshi_api.check_time_property(Day(10),Hour(2),float_dev.read(physical_state, internal_state)>=14) and svshi_api.check_time_property(Day(10),Hour(2),float_dev.read(physical_state, internal_state)>=11)