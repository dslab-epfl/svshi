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
        lambda self, app_name, instance_name, group_address, verification: f'''
class Binary_sensor_{app_name}_{instance_name}():
    def is_on(self, physical_state: PhysicalState{", internal_state: InternalState" if verification else ""}) -> bool:
        """
        pre:
        post: physical_state.{group_address} == __return__
        """
        return physical_state.{group_address}
    '''
    )

    __HUMIDITY_SENSOR_TEMPLATE = (
        lambda self, app_name, instance_name, group_address, verification: f'''
class Humidity_sensor_{app_name}_{instance_name}():
    def read(self, physical_state: PhysicalState{", internal_state: InternalState" if verification else ""}) -> float:
        """
        pre:
        post: physical_state.{group_address} == __return__
        """
        return physical_state.{group_address}
    '''
    )

    __TEMPERATURE_SENSOR_TEMPLATE = (
        lambda self, app_name, instance_name, group_address, verification: f'''
class Temperature_sensor_{app_name}_{instance_name}():
    def read(self, physical_state: PhysicalState{", internal_state: InternalState" if verification else ""}) -> float:
        """
        pre:
        post: physical_state.{group_address} == __return__
        """
        return physical_state.{group_address}
    '''
    )

    __SWITCH_TEMPLATE = (
        lambda self, app_name, instance_name, group_address, verification: f'''
class Switch_{app_name}_{instance_name}():
    def on(self, physical_state: PhysicalState{", internal_state: InternalState" if verification else ""}):
        """
        pre: 
        post: physical_state.{group_address}  == True
        """
        physical_state.{group_address} = True
        

    def off(self, physical_state: PhysicalState{", internal_state: InternalState" if verification else ""}):
        """
        pre: 
        post: physical_state.{group_address}  == False
        """
        physical_state.{group_address} = False

    def is_on(self, physical_state: PhysicalState{", internal_state: InternalState" if verification else ""}) -> bool:
        """
        pre: 
        post: physical_state.{group_address}  == __return__
        """
        return physical_state.{group_address}
    '''
    )

    __CO2_SENSOR_TEMPLATE = (
        lambda self, app_name, instance_name, group_address, verification: f'''
class CO2_sensor_{app_name}_{instance_name}():
    def read(self, physical_state: PhysicalState{", internal_state: InternalState" if verification else ""}) -> float:
        """
        pre:
        post: physical_state.{group_address} == __return__
        """
        return physical_state.{group_address}
    '''
    )
    __SVSHI_API_IMPL_VRF = f'''
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
    '''
    __SVSHI_API_IMPL_RUN = f'''
class SvshiApi():

    def __init__(self):
        pass

    def get_hour_of_the_day(self, internal_state: InternalState) -> int:
        """
        post: 0 <= __return__ <= 23
        """
        return internal_state.date_time.tm_hour
        
    def get_minute_in_hour(self, internal_state: InternalState) -> int:
        """
        post: 0 <= __return__ <= 59
        """
        return internal_state.date_time.tm_min
    
    def get_day_of_week(self, internal_state: InternalState) -> int:
        """
        post: 1 <= __return__ <= 7
        """
        return internal_state.date_time.tm_wday
        
    def get_day_of_month(self, internal_state: InternalState) -> int:
        """
        post: 1 <= __return__ <= 31
        """
        return internal_state.date_time.tm_mday
        
    def get_month_in_year(self, internal_state: InternalState) -> int:
        """
        post: 1 <= __return__ <= 12
        """
        return internal_state.date_time.tm_mon
        
    def get_year(self, internal_state: InternalState) -> int:
        """
        post: 0 <= __return__
        """
        return internal_state.date_time.tm_year 
    '''

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

        self.__manipulator = Manipulator(
            instances_names_per_app, filenames_per_app, files_folder_path
        )

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
        self.__imports.append("import time")

    def __generate_internal_state_class(self, verification):
        if not verification:
            code = f"""
@dataclasses.dataclass
class InternalState:
    date_time: time.struct_time # time object, at local machine time
"""
        else:
            code = f"""
@dataclasses.dataclass
class InternalState:
    \"\"\"
    inv: 0 <= self.time_hour <= 23
    inv: 0 <= self.time_min <= 59
    inv: 1 <= self.time_day <= 31
    inv: 1 <= self.time_weekday <= 7
    inv: 1 <= self.time_month <= 12
    inv: 0 <= self.time_year
    \"\"\"
    time_hour: int
    time_min: int
    time_day: int
    time_weekday: int
    time_month: int
    time_year: int
"""
        self.__code.append(code)

    def __generate_app_state_class(self):
        code = f"""
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
"""
        self.__code.append(code)

    def __generate_device_classes(self, verification):
        code = []
        for device in sorted(self.__devices_classes, key=lambda c: c.app.name):
            app = device.app.name
            name = device.name
            type = device.type
            address = device.address
            formatted_group_address = self.__group_addr_to_field_name(address)
            if type == "binary":
                code.append(
                    self.__BINARY_SENSOR_TEMPLATE(
                        app, name, formatted_group_address, verification
                    )
                )
            elif type == "temperature":
                code.append(
                    self.__TEMPERATURE_SENSOR_TEMPLATE(
                        app, name, formatted_group_address, verification
                    )
                )
            elif type == "humidity":
                code.append(
                    self.__HUMIDITY_SENSOR_TEMPLATE(
                        app, name, formatted_group_address, verification
                    )
                )
            elif type == "switch":
                code.append(
                    self.__SWITCH_TEMPLATE(
                        app, name, formatted_group_address, verification
                    )
                )
            elif type == "co2":
                code.append(
                    self.__CO2_SENSOR_TEMPLATE(
                        app, name, formatted_group_address, verification
                    )
                )
        if verification:
            code.append(self.__SVSHI_API_IMPL_VRF)
        else:
            code.append(self.__SVSHI_API_IMPL_RUN)
        self.__code.extend(code)

    def __generate_devices_instances(self):
        self.__code.append("\n")
        devices_code = []
        devices_code.append("svshi_api = SvshiApi()")
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
            elif type == "co2":
                device_class = "CO2_sensor_"

            devices_code.append(f"{name.upper()} = {device_class}{name}()")

        self.__code.extend(devices_code)

    def __generate_invariant_and_iteration_functions(
        self, verification: bool, app_priorities: Dict[str, int]
    ):
        self.__code.append("\n")
        imports, functions = self.__manipulator.manipulate_mains(
            verification, app_priorities
        )
        self.__imports.extend(imports)
        self.__code.extend(functions)

    def __generate_file(
        self, filename: str, verification: bool, app_priorities: Dict[str, int]
    ):
        os.makedirs(os.path.dirname(filename), exist_ok=True)
        with open(filename, "w") as file:
            self.__generate_app_state_class()
            self.__generate_physical_state_class()
            self.__generate_internal_state_class(verification)
            self.__generate_device_classes(verification)
            self.__generate_devices_instances()
            self.__generate_invariant_and_iteration_functions(
                verification, app_priorities
            )
            file.write("\n".join((sorted(set(self.__imports)))))
            file.write("\n\n")
            file.write("\n".join(self.__code))

        # Clear everything to be used again
        self.__code.clear()
        self.__imports.clear()

    def generate_verification_file(self, app_priorities: Dict[str, int]):
        """
        Generates the whole verification file.
        """
        self.__generate_file(self.__verification_filename, True, app_priorities)

    def generate_runtime_file(self, app_priorities: Dict[str, int]):
        """
        Generates the whole runtime file.
        """
        self.__generate_file(self.__runtime_filename, False, app_priorities)

    def generate_conditions_file(self):
        """
        Generates the conditions file given the conditions of all the apps installed in the library.
        """
        imports = "from .runtime_file import "
        imports_code = []
        nb_apps = len(self.__app_names)
        app_state_arguments = []
        for i, app in enumerate(sorted(self.__app_names)):
            # We also need to import the states at the end
            suffix = ", " if i < nb_apps - 1 else ", AppState, PhysicalState, InternalState\n"
            invariant_function = f"{app}_invariant"
            imports += f"{invariant_function}{suffix}"
            imports_code.append((app, invariant_function))

            app_state_arguments.append(f"{app}_app_state: AppState")

        check_conditions_body = ""
        nb_imports = len(imports_code)
        for i, (app, import_code) in enumerate(imports_code):
            suffix = " and " if i < nb_imports - 1 else ""
            check_conditions_body += (
                f"{import_code}({app}_app_state, physical_state, internal_state){suffix}"
            )

        file = f"""
{imports}
def check_conditions({", ".join(app_state_arguments)}, physical_state: PhysicalState, internal_state: InternalState) -> bool:
    return {check_conditions_body}
""".strip()

        os.makedirs(os.path.dirname(self.__conditions_filename), exist_ok=True)
        with open(self.__conditions_filename, "w+") as output_file:
            output_file.write(file)
