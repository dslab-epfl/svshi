import dataclasses


@dataclasses.dataclass
class PhysicalState:
 GA_0_0_3: float
 GA_0_0_4: float
 GA_0_0_2: bool
 GA_0_0_1: bool



class Binary_sensor_app_sam_three_binary_sensor_instance_name():
    def is_on(self, physical_state: PhysicalState) -> bool:
        """
        pre:
        post: physical_state.GA_0_0_1 == __return__
        """
        return physical_state.GA_0_0_1
    

class Switch_app_sam_three_switch_instance_name():
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
    

class Temperature_sensor_app_sam_three_temperature_sensor_instance_name():
    def read(self, physical_state: PhysicalState) -> float:
        """
        pre:
        post: physical_state.GA_0_0_3 == __return__
        """
        return physical_state.GA_0_0_3
    

class Humidity_sensor_app_sam_three_humidity_sensor_instance_name():
    def read(self, physical_state: PhysicalState) -> float:
        """
        pre:
        post: physical_state.GA_0_0_4 == __return__
        """
        return physical_state.GA_0_0_4
    


APP_SAM_THREE_BINARY_SENSOR_INSTANCE_NAME = Binary_sensor_app_sam_three_binary_sensor_instance_name()
APP_SAM_THREE_HUMIDITY_SENSOR_INSTANCE_NAME = Humidity_sensor_app_sam_three_humidity_sensor_instance_name()
APP_SAM_THREE_SWITCH_INSTANCE_NAME = Switch_app_sam_three_switch_instance_name()
APP_SAM_THREE_TEMPERATURE_SENSOR_INSTANCE_NAME = Temperature_sensor_app_sam_three_temperature_sensor_instance_name()


def app_sam_three_precond(physical_state: PhysicalState) ->bool:
    return True


def app_sam_three_iteration(physical_state: PhysicalState):
    """
pre: app_sam_three_precond(physical_state)
post: app_sam_three_precond(__return__)
"""
    print('Hello, world!')
    return physical_state
