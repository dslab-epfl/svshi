import dataclasses
import time


@dataclasses.dataclass
class PhysicalState:
 GA_0_0_1: bool
 GA_0_0_2: bool
 GA_0_0_3: float
 GA_0_0_4: float
 GA_0_0_5: int



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
    


SECOND_APP_BINARY_SENSOR_INSTANCE_NAME = Binary_sensor_second_app_binary_sensor_instance_name()
SECOND_APP_SWITCH_INSTANCE_NAME = Switch_second_app_switch_instance_name()
SECOND_APP_TEMPERATURE_SENSOR_INSTANCE_NAME = Temperature_sensor_second_app_temperature_sensor_instance_name()
SECOND_APP_HUMIDITY_SENSOR_INSTANCE_NAME = Humidity_sensor_second_app_humidity_sensor_instance_name()
FIRST_APP_BINARY_SENSOR_INSTANCE_NAME = Binary_sensor_first_app_binary_sensor_instance_name()
FIRST_APP_SWITCH_INSTANCE_NAME = Switch_first_app_switch_instance_name()
FIRST_APP_TEMPERATURE_SENSOR_INSTANCE_NAME = Temperature_sensor_first_app_temperature_sensor_instance_name()
FIRST_APP_HUMIDITY_SENSOR_INSTANCE_NAME = Humidity_sensor_first_app_humidity_sensor_instance_name()
THIRD_APP_BINARY_SENSOR_INSTANCE_NAME = Binary_sensor_third_app_binary_sensor_instance_name()
THIRD_APP_SWITCH_INSTANCE_NAME = Switch_third_app_switch_instance_name()
THIRD_APP_TEMPERATURE_SENSOR_INSTANCE_NAME = Temperature_sensor_third_app_temperature_sensor_instance_name()
THIRD_APP_HUMIDITY_SENSOR_INSTANCE_NAME = Humidity_sensor_third_app_humidity_sensor_instance_name()


def second_app_precond(physical_state: PhysicalState) ->bool:
    return SECOND_APP_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state
        ) and SECOND_APP_SWITCH_INSTANCE_NAME.is_on(physical_state)


def second_app_iteration(physical_state: PhysicalState):
    """
pre: second_app_precond(physical_state)
pre: first_app_precond(physical_state)
pre: third_app_precond(physical_state)
post: second_app_precond(__return__)
post: first_app_precond(__return__)
post: third_app_precond(__return__)
"""
    print(SECOND_APP_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state))
    time.sleep(2)
    return physical_state

def first_app_precond(physical_state: PhysicalState) ->bool:
    return FIRST_APP_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state
        ) and FIRST_APP_TEMPERATURE_SENSOR_INSTANCE_NAME.read(physical_state
        ) > 18


def first_app_iteration(physical_state: PhysicalState):
    """
pre: second_app_precond(physical_state)
pre: first_app_precond(physical_state)
pre: third_app_precond(physical_state)
post: second_app_precond(__return__)
post: first_app_precond(__return__)
post: third_app_precond(__return__)
"""
    print(FIRST_APP_BINARY_SENSOR_INSTANCE_NAME.is_on(physical_state))
    return physical_state

def third_app_precond(physical_state: PhysicalState) ->bool:
    return THIRD_APP_HUMIDITY_SENSOR_INSTANCE_NAME.read(physical_state) < 82


def third_app_iteration(physical_state: PhysicalState):
    """
pre: second_app_precond(physical_state)
pre: first_app_precond(physical_state)
pre: third_app_precond(physical_state)
post: second_app_precond(__return__)
post: first_app_precond(__return__)
post: third_app_precond(__return__)
"""
    if THIRD_APP_HUMIDITY_SENSOR_INSTANCE_NAME.read(physical_state) > 30:
        THIRD_APP_SWITCH_INSTANCE_NAME.on(physical_state)
    return physical_state
