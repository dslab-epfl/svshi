import dataclasses

@dataclasses.dataclass
class PhysicalState:
 GA_0_0_1: bool



class Binary_sensor_app_binary_sensor_instance_name():
    def is_on(self, physical_state: PhysicalState) -> bool:
        """
        pre:
        post: physical_state.GA_0_0_1 == __return__
        """
        return physical_state.GA_0_0_1
    

class Switch_app_switch_instance_name():
    def on(self, physical_state: PhysicalState):
        """
        pre: 
        post: physical_state.GA_0_0_1  == True
        """
        physical_state.GA_0_0_1 = True
        

    def off(self, physical_state: PhysicalState):
        """
        pre: 
        post: physical_state.GA_0_0_1  == False
        """
        physical_state.GA_0_0_1 = False

    def is_on(self, physical_state: PhysicalState) -> bool:
        """
        pre: 
        post: physical_state.GA_0_0_1  == __return__
        """
        return physical_state.GA_0_0_1
    

class Temperature_sensor_app_temperature_sensor_instance_name():
    def read(self, physical_state: PhysicalState) -> float:
        """
        pre:
        post: physical_state.GA_0_0_1 == __return__
        """
        return physical_state.GA_0_0_1
    

class Humidity_sensor_app_humidity_sensor_instance_name():
    def read(self, physical_state: PhysicalState) -> float:
        """
        pre:
        post: physical_state.GA_0_0_1 == __return__
        """
        return physical_state.GA_0_0_1
    


APP_BINARY_SENSOR_INSTANCE_NAME = Binary_sensor_app_binary_sensor_instance_name()
APP_SWITCH_INSTANCE_NAME = Switch_app_switch_instance_name()
APP_TEMPERATURE_SENSOR_INSTANCE_NAME = Temperature_sensor_app_temperature_sensor_instance_name()
APP_HUMIDITY_SENSOR_INSTANCE_NAME = Humidity_sensor_app_humidity_sensor_instance_name()


def app_precond(physical_state: PhysicalState) ->bool:
    return True


def app_iteration(physical_state: PhysicalState):
    """
pre: app_precond(physical_state)
pre: another_app_precond(physical_state)
post: app_precond(__return__)
post: another_app_precond(__return__)
"""
    print('Hello, world!')
    return physical_state

def another_app_precond(physical_state: PhysicalState) ->bool:
    return True


def another_app_iteration(physical_state: PhysicalState):
    """
pre: app_precond(physical_state)
pre: another_app_precond(physical_state)
post: app_precond(__return__)
post: another_app_precond(__return__)
"""
    print('Hello, world!')
    return physical_state
