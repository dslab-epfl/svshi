"""
Some class definitions for the simulated KNX actuators.
"""

from system.telegrams import HeaterPayload, Payload, Telegram, TempControllerPayload, ButtonPayload, BinaryPayload, DimmerPayload#, SwitchPayload
from .device_abstractions import Actuator
from abc import ABC, abstractclassmethod, abstractmethod
import sys, logging
sys.path.append("core")


class LightActuator(Actuator, ABC):
    """Abstract class to represent light devices"""

    def __init__(self, class_name, name, refid, individual_addr, default_status, state, lumen):
        super().__init__(class_name, name, refid, individual_addr, default_status, "light", state)
        self.lumen = lumen

    def lumen_to_Lux(self, lumen, area):
        ''' The conversion from Lumens to Lux given the surface area in squared meters '''
        return lumen/area

    def lux_to_Lumen(self, lux, area):
        ''' The conversion from Lux to Lumen given the surface area in squared meters '''
        return area*lux


class LED(LightActuator):
    """Concrete class to represent LED lights"""

    # state is ON/OFF=True/False
    def __init__(self, name, refid, individual_addr, default_status, state=False):
        super().__init__('LED', name, refid, individual_addr, default_status, state, lumen=800)
        self.state = False
        self.state_ratio = 100 # Percentage of 'amplitude'

    def update_state(self, telegram):
        if telegram.control_field == True: # Control field bit

            if isinstance(telegram.payload, BinaryPayload):
                self.state = telegram.payload.binary_state
            # if isinstance(telegram.payload, SwitchPayload):
            #     # telegrams with Switch payload are telegrams from SVSHI, that are supposed to turn on a Switch
            #     self.state = telegram.payload.switch_state
            if isinstance(telegram.payload, DimmerPayload):
                self.state = telegram.payload.binary_state
                if self.state:
                    self.state_ratio = telegram.payload.state_ratio

            # if isinstance(telegram.payload, SwitchPayload):
            #     if telegram.payload.switched:
            #         self.state = not self.state
            self.str_state = 'ON' if self.state else 'OFF'
            logging.info(f"{self.name} has been turned {self.str_state} by device '{telegram.source}'.")
        # if the control field is not True, the telegram does nto concern the LED, except for a read state


# class Switch(Actuator):
#     """ Class to represent KNX Switch Actuators"""
#     def __init__(self, name, refid, individual_addr, default_status, switch_state=False):
#         super().__init__('Switch', name, refid, individual_addr, default_status, switch_state)
#         self.switch_state = switch_state



class TemperatureActuator(Actuator, ABC):
    """Abstract class to represent temperature devices"""

    def __init__(self, class_name, name, refid, individual_addr, default_status, actuator_type, state, update_rule, max_power=0):
        super().__init__(class_name, name, refid, individual_addr, default_status, actuator_type, state)
        self.update_rule = update_rule
        self.max_power = max_power
        """Power of the device in Watts"""
        self.power = max_power
        """Power really used, max by default"""


class Heater(TemperatureActuator):
    """Concrete class to represent a heating device"""

    def __init__(self, name, refid, individual_addr, default_status, max_power=400, state=False, update_rule=1):
        # Verification of update_rule sign
        try:
            assert update_rule >= 0
        except AssertionError:
            logging.error("The Heater should have update_rule>=0")
            sys.exit()
        super().__init__('Heater', name, refid, individual_addr, default_status, "heater", state, update_rule, max_power)


    def watts_to_temp(self, watts):
        return ((watts - 70)*2)/7 + 18

    def required_power(self, desired_temperature=20, volume=1, insulation_state="good"):
        from system import INSULATION_TO_CORRECTION_FACTOR
        assert desired_temperature >= 10 and desired_temperature <= 40
        desired_wattage = volume*self.temp_to_watts(desired_temperature)
        desired_wattage += desired_wattage*INSULATION_TO_CORRECTION_FACTOR[insulation_state]
        return desired_wattage

    def max_temperature_in_room(self, volume=1, insulation_state="good"):
        """Maximum reachable temperature for this heater in the specified room"""
        from system import INSULATION_TO_CORRECTION_FACTOR
        watts = self.power/((1+INSULATION_TO_CORRECTION_FACTOR[insulation_state])*volume)
        return self.watts_to_temp(watts)


    def update_state(self, telegram):
         if telegram.control_field == True:  # Control field bit
            # If simple binary telegram payload, we turn heater ON at max power
            if isinstance(telegram.payload, BinaryPayload):
                self.state = telegram.payload.binary_state
                self.power = self.max_power
            # if isinstance(telegram.payload, SwitchPayload):
            #     # telegrams with Switch payload are telegrams from SVSHI, that are supposed to turn on a Switch
            #     self.state = telegram.payload.switch_state
            #     self.power = self.max_power
            # If more complex telegram, we can adapt the power of the heater
            # if isinstance(telegram.payload, TempControllerPayload):
            #     if telegram.payload.set_heater_power is not Payload.EMPTY_FIELD:
            #         wished_power = telegram.payload.set_heater_power
            #         if wished_power < 0:
            #             wished_power = 0
            #         self.power = wished_power if wished_power <= self.max_power else self.max_power


class AC(TemperatureActuator):
    """Concrete class to represent a cooling device"""

    def __init__(self, name, refid, individual_addr, default_status, max_power=400, state=False, update_rule=-1):
        # Verification of update_rule sign
        try:
            assert update_rule <= 0
        except AssertionError:
            logging.error("The Cooler should have update_rule<=0")
            sys.exit()
        super().__init__('AC', name, refid, individual_addr, default_status, "ac", state, update_rule, max_power)


    def update_state(self, telegram):
        if telegram.control_field == True:  # Control field bit
            # If simple binary telegram payload, we turn heater ON at max power
            if isinstance(telegram.payload, BinaryPayload):
                self.state = telegram.payload.binary_state
                self.power = self.max_power
            if isinstance(telegram.payload, SwitchPayload):
                # telegrams with Switch payload are telegrams from SVSHI, that are supposed to turn on a Switch
                self.state = telegram.payload.switch_state
                self.power = self.max_power
            
            # if isinstance(telegram.payload, ACPayload):


            # TODO: Payload for AC
