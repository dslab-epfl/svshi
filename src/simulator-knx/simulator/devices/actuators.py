"""
Class definitions for the simulated KNX actuators: 
LED, Heater, AC, Switch and IPInterface.
"""

import logging
import sys
from abc import ABC
from typing import Dict, Union, Tuple

from system.telegrams import Telegram, BinaryPayload, DimmerPayload, FloatPayload
from system.system_tools import IndividualAddress
from .device_abstractions import Actuator


class LightActuator(Actuator, ABC):
    """Abstract class to represent actuators acting on world's brightness"""

    def __init__(
        self,
        name: str,
        individual_addr: IndividualAddress,
        state: bool,
        lumen: float,
        beam_angle: float,
    ) -> None:
        """
        Initialization of the light actuator object

        lumen : Luminous flux of light source = quantity of visible light emitted from a source per unit of time
        beam_angle : Angle at which the light is emitted (e.g. 180° for a LED bulb)"""
        super().__init__(name, individual_addr, state)
        self.max_lumen = lumen
        self.beam_angle = beam_angle


class LED(LightActuator):
    """Concrete class to represent LED actuators"""

    def __init__(
        self, name: str, individual_addr: IndividualAddress, state: bool = False
    ) -> None:
        """Initialization of a LED device object"""
        super().__init__(name, individual_addr, state, lumen=800, beam_angle=180)
        self.state_ratio = 100  # in %
        self.max = 800.0  # for manual setting of actuator in gui
        self.min = 0.0
        self.state_value = (self.max - self.min) * self.state_ratio / 100 + self.min

    def send_state_in_telegram(self) -> None:
        dimmer_payload = DimmerPayload(
            binary_state=self.state, state_ratio=self.state_ratio
        )
        if hasattr(dimmer_payload, "content"):
            self.send_telegram(dimmer_payload)
        else:
            logging.error(
                f"The Dimmer payload was not correctly created, the telegram cannot be sent."
            )

    def update_state(self, telegram: Telegram) -> None:
        """
        Update the state and/or state_ratio of the LED actuator

        telegram : packet with new devices states
        """

        old_state = self.state
        old_state_value = self.state_value

        if isinstance(telegram.payload, BinaryPayload):
            self.state = telegram.payload.content
            if self.state:
                self.state_value = self.max
            else:
                self.state_value = self.min
        if isinstance(telegram.payload, DimmerPayload):
            self.state = telegram.payload.content
            if self.state:
                self.state_ratio = telegram.payload.state_ratio
                self.state_value = (
                    self.max - self.min
                ) * self.state_ratio / 100 + self.min
            else:
                self.state_ratio = 0.0
                self.state_value = self.min

        self.__str_state = "ON" if self.state else "OFF"
        logging.info(
            f"{self.name} has been turned {self.__str_state} by device '{telegram.source}'."
        )
        state_changed = old_state != self.state or old_state_value != self.state_value
        if hasattr(self, "svshi_mode"):
            if (
                self.svshi_mode and state_changed
            ):  # attribute svshi_mode set when actuator connected to the bus in room.add_device() method
                self.send_state_in_telegram()

    def user_input(self, state: bool = None, state_ratio: float = 100):
        """
        Update its state and send a telegram with FloatPayload on the bus.

        state_ratio : fraction of the dimmer state, percentage in (0-100)
        state_value : actual value of the actuator, in its units
        """
        old_state = self.state
        old_state_value = self.state_value

        self.state = not self.state
        if state is not None:
            self.state = state
        self.__str_state = "ON" if self.state else "OFF"

        if self.state:
            self.state_ratio = state_ratio
            self.state_value = (self.max - self.min) * self.state_ratio / 100 + self.min
            logging.info(
                f"The {self.name} has been turned {self.__str_state} at {self.state_ratio}% corresponding to {self.state_value} lumens for this LED."
            )
        else:
            self.state_ratio = 0.0
            self.state_value = self.min
            logging.info(f"The {self.name} has been turned {self.__str_state}.")

        state_changed = old_state != self.state or old_state_value != self.state_value
        if hasattr(self, "svshi_mode"):
            if (
                self.svshi_mode and state_changed
            ):  # attribute set when actuator connected to the bus in room.add_device() method
                self.send_state_in_telegram()

    def effective_lumen(self) -> float:
        """Lumen quantity adjusted with the state ratio (% of source's max lumens)"""
        return self.max_lumen * (self.state_ratio / 100)

    def get_dev_info(
        self,
    ) -> Dict[str, Union[str, bool, float, Tuple[float, float, float]]]:
        """Return information about the LED device's states and configuration, method called via CLI commmand 'getinfo'"""
        dev_specific_dict = {
            "state": self.state,
            "max_lumen": self.max_lumen,
            "effective_lumen": self.effective_lumen(),
            "beam_angle": self.beam_angle,
            "state_ratio": self.state_ratio,
        }
        dev_specific_dict.update(self._dev_basic_dict)
        return dev_specific_dict


class TemperatureActuator(Actuator, ABC):
    """Abstract class to represent actuators acting on world's temperature"""

    def __init__(
        self,
        name: str,
        individual_addr: IndividualAddress,
        state: bool,
        update_rule: float,
        max_power: float = 0,
    ) -> None:
        """
        Initialization of the temperature actuator device object

        update_rule : +/- 1 for now, indicate whether the temperature actuator can heat up or cool down room's temperature
        max_power : power in Watts of the temperature actuator"""
        super().__init__(name, individual_addr, state)
        self.update_rule = update_rule
        self.max_power = max_power
        self.state_ratio = 100  # in %

    def effective_power(self) -> float:
        """Power value adjusted with the state ratio (% of source's max power)"""
        return self.max_power * self.state_ratio / 100

    def get_dev_info(
        self,
    ) -> Dict[str, Union[str, bool, float, Tuple[float, float, float]]]:
        """Return information about the Temperature actuator device's states and configuration, method called via CLI commmand 'getinfo'"""
        dev_specific_dict = {
            "state": self.state,
            "update_rule": self.update_rule,
            "max_power": self.max_power,
            "state_ratio": self.state_ratio,
            "effective_power": self.effective_power(),
        }
        dev_specific_dict.update(self._dev_basic_dict)
        return dev_specific_dict


class Heater(TemperatureActuator):
    """Concrete class to represent a heating device"""

    def __init__(
        self,
        name: str,
        individual_addr: IndividualAddress,
        max_power: float = 400,
        state: bool = False,
        update_rule: float = 1,
    ) -> None:
        """
        Initialization of a heater device object

        update_rule : should be > 0
        """
        try:
            assert update_rule > 0
        except AssertionError:
            logging.error(
                f"The Heater should have update_rule > 0, but {update_rule} was given."
            )
            sys.exit()
        super().__init__(name, individual_addr, state, update_rule, max_power)
        self.max = 400.0  # for manual setting of actuator in gui
        self.min = 0.0
        self.state_value = self.min

    def send_state_in_telegram(self):
        dimmer_payload = DimmerPayload(
            binary_state=self.state, state_ratio=self.state_ratio
        )
        if hasattr(dimmer_payload, "content"):
            self.send_telegram(dimmer_payload)
        else:
            logging.error(
                f"The Dimmer payload was not correctly created, the telegram cannot be sent."
            )

    def update_state(self, telegram: Telegram) -> None:
        """
        Update the state and/or state_ratio of the heater actuator

        telegram : packet with new devices states
        """
        old_state = self.state
        old_state_value = self.state_value
        if isinstance(telegram.payload, BinaryPayload):
            self.state = telegram.payload.content
            if self.state:
                self.state_value = self.max
            else:
                self.state_value = self.min
        if isinstance(telegram.payload, DimmerPayload):
            self.state = telegram.payload.content
            if self.state:
                self.state_ratio = telegram.payload.state_ratio
                self.state_value = (
                    self.max - self.min
                ) * self.state_ratio / 100 + self.min
            else:
                self.state_ratio = 0.0
                self.state_value = self.min

        self.__str_state = "ON" if self.state else "OFF"
        logging.info(
            f"{self.name} has been turned {self.__str_state} by device '{telegram.source}'."
        )
        state_changed = old_state != self.state or old_state_value != self.state_value
        if hasattr(self, "svshi_mode"):
            if (
                self.svshi_mode and state_changed
            ):  # attribute set when actuator connected to the bus in room.add_device() method
                self.send_state_in_telegram()

    def user_input(self, state: bool = None, state_ratio: float = 100):
        """
        Update its state and send a telegram with FloatPayload on the bus.

        state_ratio : fraction of the dimmer state, percentage in (0-100)
        state_value : actual value of the actuator, in its units
        """
        old_state = self.state
        old_state_value = self.state_value
        self.state = not self.state
        if state is not None:
            self.state = state
        self.__str_state = "ON" if self.state else "OFF"

        if self.state:
            self.state_ratio = state_ratio
            self.state_value = (self.max - self.min) * self.state_ratio / 100 + self.min
            logging.info(
                f"The {self.name} has been turned {self.__str_state} at {self.state_ratio}% corresponding to {self.state_value} Watts for this Heater."
            )
        else:
            self.state_ratio = 0.0
            self.state_value = self.min
            logging.info(f"The {self.name} has been turned {self.__str_state}.")

        state_changed = old_state != self.state or old_state_value != self.state_value
        if hasattr(self, "svshi_mode"):
            if (
                self.svshi_mode and state_changed
            ):  # attribute set when actuator connected to the bus in room.add_device() method
                self.send_state_in_telegram()


class AC(TemperatureActuator):
    """Concrete class to represent a cooling device"""

    def __init__(
        self,
        name: str,
        individual_addr: IndividualAddress,
        max_power: float = 400,
        state: bool = False,
        update_rule: float = -1,
    ) -> None:
        """
        Initialization of an AC device object

        update_rule : should be < 0
        """
        try:
            assert update_rule < 0
        except AssertionError:
            logging.error(
                f"The AC should have update_rule < 0, but {update_rule} was given."
            )
            sys.exit()
        super().__init__(name, individual_addr, state, update_rule, max_power)
        self.max = 400.0  # for manual setting of actuator in gui
        self.min = 0.0
        self.state_value = self.min

    def send_state_in_telegram(self):
        dimmer_payload = DimmerPayload(
            binary_state=self.state, state_ratio=self.state_ratio
        )
        if hasattr(dimmer_payload, "content"):
            self.send_telegram(dimmer_payload)
        else:
            logging.error(
                f"The Dimmer payload was not correctly created, the telegram cannot be sent."
            )

    def update_state(self, telegram: Telegram) -> None:
        """
        Update the state and/or state_ratio of the AC actuator

        telegram : packet with new devices states
        """
        old_state = self.state
        old_state_value = self.state_value

        if isinstance(telegram.payload, BinaryPayload):
            self.state = telegram.payload.content
            if self.state:
                self.state_value = self.max
            else:
                self.state_value = self.min
        if isinstance(telegram.payload, DimmerPayload):
            self.state = telegram.payload.content
            if self.state:
                self.state_ratio = telegram.payload.state_ratio
                self.state_value = (
                    self.max - self.min
                ) * self.state_ratio / 100 + self.min
            else:
                self.state_ratio = 0.0
                self.state_value = self.min

        self.__str_state = "ON" if self.state else "OFF"
        logging.info(
            f"{self.name} has been turned {self.__str_state} by device '{telegram.source}'."
        )

        state_changed = old_state != self.state or old_state_value != self.state_value
        if hasattr(self, "svshi_mode"):
            if (
                self.svshi_mode and state_changed
            ):  # attribute set when actuator connected to the bus in room.add_device() method
                self.send_state_in_telegram()

    def user_input(self, state: bool = None, state_ratio: float = 100):
        old_state = self.state
        old_state_value = self.state_value

        self.state = not self.state
        if state is not None:
            self.state = state
        self.__str_state = "ON" if self.state else "OFF"

        if self.state:
            self.state_ratio = state_ratio
            self.state_value = (self.max - self.min) * self.state_ratio / 100 + self.min
            logging.info(
                f"The {self.name} has been turned {self.__str_state} at {self.state_ratio}% corresponding to {self.state_value} Watts for this AC."
            )
        else:
            self.state_ratio = 0.0
            self.state_value = self.min
            logging.info(f"The {self.name} has been turned {self.__str_state}.")

        state_changed = old_state != self.state or old_state_value != self.state_value

        if hasattr(self, "svshi_mode"):
            if (
                self.svshi_mode and state_changed
            ):  # attribute set when actuator connected to the bus in room.add_device() method
                self.send_state_in_telegram()


class Switch(Actuator):
    """Concrete class to represent a swicth indicator, can be linked to any real actuator device to indicate its state in GUI"""

    def __init__(
        self, name: str, individual_addr: IndividualAddress, state: bool = False
    ) -> None:
        """Initialization of an Switch device object"""
        super().__init__(name, individual_addr, state)
        self.state_ratio = 100  # in %
        self.max = 100.0  # for manual setting of actuator in gui
        self.min = 0.0
        self.state_value = (self.max - self.min) * self.state_ratio / 100 + self.min

    def send_state_in_telegram(self):
        binary_payload = BinaryPayload(binary_state=self.state)
        if hasattr(binary_payload, "content"):
            self.send_telegram(binary_payload)
        else:
            logging.error(
                f"The Binary payload was not correctly created, the telegram cannot be sent."
            )

    def update_state(self, telegram: Telegram) -> None:
        """
        Update the state and/or state_ratio of the Switch actuator

        telegram : packet with new devices states
        """
        old_state = self.state
        old_state_value = self.state_value

        if isinstance(telegram.payload, BinaryPayload):
            self.state = telegram.payload.content
            if self.state:
                self.state_value = self.max
            else:
                self.state_value = self.min
        if isinstance(telegram.payload, DimmerPayload):
            self.state = telegram.payload.content
            if self.state:
                self.state_ratio = telegram.payload.state_ratio
                self.state_value = (
                    self.max - self.min
                ) * self.state_ratio / 100 + self.min
            else:
                self.state_ratio = 0.0
                self.state_value = self.min

        self.__str_state = "ON" if self.state else "OFF"
        logging.info(
            f"{self.name} has been turned {self.__str_state} by device '{telegram.source}'."
        )

        state_changed = old_state != self.state or old_state_value != self.state_value

        if hasattr(self, "svshi_mode"):
            if (
                self.svshi_mode and state_changed
            ):  # attribute set when actuator connected to the bus in room.add_device() method
                self.send_state_in_telegram()

    # def user_input(self):
    #     print(f"actuator {self.name} pressed")
    #     self.state = not self.state

    def user_input(self, state: bool = None, state_ratio: float = 100):
        """
        Update its state and send a telegram with FloatPayload on the bus.

        state_ratio : fraction of the dimmer state, percentage in (0-100)
        state_value : actual value of the actuator, in its units
        """
        old_state = self.state
        old_state_value = self.state_value

        self.state = not self.state
        if state is not None:
            self.state = state
        self.__str_state = "ON" if self.state else "OFF"

        if self.state:
            self.state_ratio = state_ratio
            self.state_value = (self.max - self.min) * self.state_ratio + self.min
            logging.info(
                f"The {self.name} has been turned {self.__str_state} at {self.state_ratio}% corresponding to {self.state_value} % for this Switch."
            )
        else:
            self.state_ratio = 0.0
            self.state_value = self.min
            logging.info(f"The {self.name} has been turned {self.__str_state}.")

        state_changed = old_state != self.state or old_state_value != self.state_value

        if hasattr(self, "svshi_mode"):
            if (
                self.svshi_mode and state_changed
            ):  # attribute set when actuator connected to the bus in room.add_device() method
                self.send_state_in_telegram()

    def get_dev_info(
        self,
    ) -> Dict[str, Union[str, bool, float, Tuple[float, float, float]]]:
        """Return information about the Switch actuator device's states and configuration, method called via CLI commmand 'getinfo'"""
        dev_specific_dict = {"state": self.state}
        dev_specific_dict.update(self._dev_basic_dict)
        return dev_specific_dict


class IPInterface(Actuator):
    """
    Concrete class to represent an IP interface to communicate with external interfaces
    Considered as an actuator as it subscribes to the bus, receives telegrams and 'act' on the physical world (send network packets to external SVSHI program
    """

    from svshi_interface.main import Interface

    def __init__(
        self,
        name: str,
        individual_addr: IndividualAddress,
        interface: Interface,
        state: bool = False,
    ) -> None:
        """Initialization of an Switch device object"""
        super().__init__(name, individual_addr, state)
        self.interface = interface

    def update_state(self, telegram: Telegram) -> None:
        """
        Update the state and/or state_ratio of the Switch actuator

        telegram : packet with new devices states
        """

        if isinstance(telegram.payload, BinaryPayload) and not isinstance(
            telegram.payload, DimmerPayload
        ):
            self.interface.add_to_sending_queue([telegram])
        elif isinstance(telegram.payload, DimmerPayload):
            print("sending dimmer payload telegram")
            self.interface.add_to_sending_queue([telegram])
        elif isinstance(
            telegram.payload, FloatPayload
        ):  # Sensors values sent regularly on bus
            self.interface.add_to_sending_queue([telegram])

    def user_input(self):
        print(f"actuator {self.name} pressed")

    def get_dev_info(self) -> None:
        """IP Interface is not considered as a real device and has no specific attributes/characteristics, method implemented to respect the definition of abstract class Actuators"""
        return None
