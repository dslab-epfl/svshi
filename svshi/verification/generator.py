import os
from itertools import groupby
from typing import Dict, Final, List, Set

from .manipulator import Manipulator
from .parser import DeviceClass, DeviceInstance, GroupAddress


class Generator:
    """
    Code generator.
    """

    __GROUP_ADDRESS_PREFIX: Final = "GA_"
    __SLASH: Final = "/"
    __UNDERSCORE: Final = "_"

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
        conditions_filename: str,
        files_folder_path: str,
        group_addresses: List[GroupAddress],
        devices_instances: List[DeviceInstance],
        devices_classes: List[DeviceClass],
        app_names: List[str],
        filenames_per_app: Dict[str, Set[str]],
    ):
        self.__verification_filename: str = verification_filename
        self.__runtime_filename: str = runtime_filename
        self.__conditions_filename: str = conditions_filename
        self.__group_addresses = group_addresses
        self.__devices_instances = devices_instances
        self.__devices_classes = devices_classes
        self.__app_names = app_names

        instances_names_per_app = {}
        for key, group in groupby(self.__devices_classes, lambda d: d.app):
            instances_names_per_app[(key.directory, key.name)] = {
                device.name.upper() for device in group
            }

        self.__manipulator = Manipulator(instances_names_per_app, filenames_per_app, files_folder_path)

        self.__code: List[str] = []
        self.__imports: List[str] = []

    def __group_addr_to_field_name(self, group_addr: str) -> str:
        """
        Converts a group address to its corresponding field name in PhysicalState.
        Example: 1/1/1 -> GA_1_1_1
        """
        return self.__GROUP_ADDRESS_PREFIX + group_addr.replace(
            self.__SLASH, self.__UNDERSCORE
        )

    def __generate_physical_state_class(self):
        fields = ""
        for group_address in self.__group_addresses:
            new_field = f" {self.__group_addr_to_field_name(group_address.address)}: {group_address.type}\n"
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
            formatted_group_address = self.__group_addr_to_field_name(address)
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

    def __generate_invariant_and_iteration_functions(self, verification: bool):
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
            self.__generate_invariant_and_iteration_functions(verification)
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

    def generate_conditions_file(self):
        """
        Generates the conditions file given the conditions of all the apps installed in the library.
        """
        imports = "from .verification_file import "
        imports_code = []
        nb_apps = len(self.__app_names)
        for i, app in enumerate(sorted(self.__app_names)):
            # We also need to import the PhysicalState at the end
            suffix = ", " if i < nb_apps - 1 else ", PhysicalState\n"
            invariant_function = f"{app}_invariant"
            imports += f"{invariant_function}{suffix}"
            imports_code.append(invariant_function)

        check_conditions_body = ""
        nb_imports = len(imports_code)
        for i, import_code in enumerate(imports_code):
            suffix = " and " if i < nb_imports - 1 else ""
            check_conditions_body += f"{import_code}(state){suffix}"

        file = f"""
{imports}
def check_conditions(state: PhysicalState) -> bool:
    return {check_conditions_body}
""".strip()

        os.makedirs(os.path.dirname(self.__conditions_filename), exist_ok=True)
        with open(self.__conditions_filename, "w+") as output_file:
            output_file.write(file)
