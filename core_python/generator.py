from itertools import groupby
from typing import List, Tuple

from core_python.manipulator import Manipulator


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
        filename: str,
        group_addresses: List[Tuple[str, str]],
        devices_instances: List[Tuple[str, str]],
        devices_classes: List[Tuple[str, str, str, str]],
    ):
        self.__filename: str = filename
        self.__group_addresses = group_addresses
        self.__devices_instances = devices_instances
        self.__devices_classes = devices_classes
        self.__instances_names_per_app = {}
        for key, group in groupby(self.__devices_classes, lambda x: x[0]):
            for device in group:
                self.__instances_names_per_app[key] = device[1]
        self.__manipulator = Manipulator(self.__instances_names_per_app)
        self.__code: List[str] = []
        self.__imports: List[str] = []

    def __generate_physical_state_class(self):
        fields = ""
        for (name, typee) in self.__group_addresses:
            fields += f" GA_{name.replace('/', '_')}: {typee}\n"

        code = f"""
@dataclasses.dataclass
class PhysicalState:
{fields}
"""
        self.__code.append(code)
        self.__imports.append("import dataclasses")

    def __generate_device_classes(self):
        code = []
        for (app, name, type, address) in self.__devices_classes:
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
        for (name, type) in self.__devices_instances:
            device_class = "Binary_sensor_"
            if type == "switch":
                device_class = "Switch_"
            elif type == "temperature":
                device_class = "Temperature_sensor_"
            elif type == "humidity":
                device_class = "Humidity_sensor_"

            devices_code.append(f"{name.upper()} = {device_class}{name}()")

        self.__code.extend(devices_code)

    def __generate_precond_iteration_functions(self):
        self.__code.append("\n")
        imports, functions = self.__manipulator.manipulate_mains()
        self.__imports.extend(imports)
        self.__code.extend(functions)

    def generate_verification_file(self):
        """
        Generates the whole verification file.
        """
        with open(self.__filename, "w") as file:
            self.__generate_physical_state_class()
            self.__generate_device_classes()
            self.__generate_devices_instances()
            self.__generate_precond_iteration_functions()
            file.write("\n".join(self.__imports))
            file.write("\n")
            file.write("\n".join(self.__code))
