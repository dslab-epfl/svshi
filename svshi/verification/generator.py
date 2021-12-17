import os
from itertools import groupby
from typing import List

from .manipulator import Manipulator
from .parser import DeviceClass, DeviceInstance, GroupAddress


class Generator:
    """
    Code generator.
    """

    __BINARY_SENSOR_TEMPLATE = (
        lambda self, app_name, instance_name, group_address: f'''
class Binary_sensor_{app_name}_{instance_name}():
    def is_on(self, physical_state: PhysicalState) -> bool:
        """
        pre:
        post: physical_state.{group_address} == __return__
        """
        return physical_state.{group_address}
    '''
    )

    __HUMIDITY_SENSOR_TEMPLATE = (
        lambda self, app_name, instance_name, group_address: f'''
class Humidity_sensor_{app_name}_{instance_name}():
    def read(self, physical_state: PhysicalState) -> float:
        """
        pre:
        post: physical_state.{group_address} == __return__
        """
        return physical_state.{group_address}
    '''
    )

    __TEMPERATURE_SENSOR_TEMPLATE = (
        lambda self, app_name, instance_name, group_address: f'''
class Temperature_sensor_{app_name}_{instance_name}():
    def read(self, physical_state: PhysicalState) -> float:
        """
        pre:
        post: physical_state.{group_address} == __return__
        """
        return physical_state.{group_address}
    '''
    )

    __SWITCH_TEMPLATE = (
        lambda self, app_name, instance_name, group_address: f'''
class Switch_{app_name}_{instance_name}():
    def on(self, physical_state: PhysicalState):
        """
        pre: 
        post: physical_state.{group_address}  == True
        """
        physical_state.{group_address} = True
        

    def off(self, physical_state: PhysicalState):
        """
        pre: 
        post: physical_state.{group_address}  == False
        """
        physical_state.{group_address} = False

    def is_on(self, physical_state: PhysicalState) -> bool:
        """
        pre: 
        post: physical_state.{group_address}  == __return__
        """
        return physical_state.{group_address}
    '''
    )

    def __init__(
        self,
        verification_filename: str,
        runtime_filename: str,
        group_addresses: List[GroupAddress],
        devices_instances: List[DeviceInstance],
        devices_classes: List[DeviceClass],
    ):
        self.__verification_filename: str = verification_filename
        self.__runtime_filename: str = runtime_filename
        self.__group_addresses = group_addresses
        self.__devices_instances = devices_instances
        self.__devices_classes = devices_classes
        self.__instances_names_per_app = {}
        for key, group in groupby(self.__devices_classes, lambda d: d.app):
            names = set()
            for device in group:
                names.add(device.name.upper())
            self.__instances_names_per_app[(key.directory, key.name)] = names
        self.__manipulator = Manipulator(self.__instances_names_per_app)
        self.__code: List[str] = []
        self.__imports: List[str] = []

    def __generate_physical_state_class(self):
        fields = ""
        for group_address in self.__group_addresses:
            new_field = (
                f" GA_{group_address.address.replace('/', '_')}: {group_address.type}\n"
            )
            if new_field not in fields:
                fields += new_field

        code = f"""
@dataclasses.dataclass
class PhysicalState:
{fields}
"""
        self.__code.append(code)
        self.__imports.append("import dataclasses")

    def __generate_device_classes(self):
        code = []
        for device in sorted(self.__devices_classes, key=lambda c: c.app.name):
            app = device.app.name
            name = device.name
            type = device.type
            address = device.address
            formatted_group_address = f"GA_{address.replace('/', '_')}"
            if type == "binary":
                code.append(
                    self.__BINARY_SENSOR_TEMPLATE(app, name, formatted_group_address)
                )
            elif type == "temperature":
                code.append(
                    self.__TEMPERATURE_SENSOR_TEMPLATE(
                        app, name, formatted_group_address
                    )
                )
            elif type == "humidity":
                code.append(
                    self.__HUMIDITY_SENSOR_TEMPLATE(app, name, formatted_group_address)
                )
            elif type == "switch":
                code.append(self.__SWITCH_TEMPLATE(app, name, formatted_group_address))

        self.__code.extend(code)

    def __generate_devices_instances(self):
        self.__code.append("\n")
        devices_code = []
        for instance in sorted(self.__devices_instances, key=lambda i: i.name):
            name = instance.name
            type = instance.type
            device_class = "Binary_sensor_"
            if type == "switch":
                device_class = "Switch_"
            elif type == "temperature":
                device_class = "Temperature_sensor_"
            elif type == "humidity":
                device_class = "Humidity_sensor_"

            devices_code.append(f"{name.upper()} = {device_class}{name}()")

        self.__code.extend(devices_code)

    def __generate_precond_iteration_functions(self, verification: bool):
        self.__code.append("\n")
        imports, functions = self.__manipulator.manipulate_mains(verification)
        self.__imports.extend(imports)
        self.__code.extend(functions)

    def __generate_file(self, filename: str, verification: bool):
        os.makedirs(os.path.dirname(filename), exist_ok=True)
        with open(filename, "w") as file:
            self.__generate_physical_state_class()
            self.__generate_device_classes()
            self.__generate_devices_instances()
            self.__generate_precond_iteration_functions(verification)
            file.write("\n".join((sorted(set(self.__imports)))))
            file.write("\n\n")
            file.write("\n".join(self.__code))

        # Clear everything to be used again
        self.__code.clear()
        self.__imports.clear()

    def generate_verification_file(self):
        """
        Generates the whole verification file.
        """
        self.__generate_file(self.__verification_filename, True)

    def generate_runtime_file(self):
        """
        Generates the whole runtime file.
        """
        self.__generate_file(self.__runtime_filename, False)
