from typing import List, Tuple
import os
import json


class Generator:
    """
    Code generator.
    """

    __BINARY_SENSOR_TEMPLATE = (
        lambda self, app_name, instance_name, group_address: f"""
class Binary_sensor_{app_name}_{instance_name}():
    def is_on(self, physical_state: PhysicalState) -> bool:
        '''
        pre:
        post: physical_state.{group_address} == __return__
        '''
        return physical_state.{group_address}
    """
    )

    __HUMIDITY_SENSOR_TEMPLATE = (
        lambda self, app_name, instance_name, group_address: f"""
class Humidity_sensor_{app_name}_{instance_name}():
    def read(self, physical_state: PhysicalState) -> float:
        '''
        pre:
        post: physical_state.{group_address} == __return__
        '''
        return physical_state.{group_address}
    """
    )

    __TEMPERATURE_SENSOR_TEMPLATE = (
        lambda self, app_name, instance_name, group_address: f"""
class Temperature_sensor_{app_name}_{instance_name}():
    def read(self, physical_state: PhysicalState) -> float:
        '''
        pre:
        post: physical_state.{group_address} == __return__
        '''
        return physical_state.{group_address}
    """
    )

    __SWITCH_TEMPLATE = (
        lambda self, app_name, instance_name, group_address: f"""
class Switch_{app_name}_{instance_name}():
    def on(self, physical_state: PhysicalState):
        '''
        pre: 
        post: physical_state.{group_address}  == True
        '''
        physical_state.{group_address} = True
        

    def off(self, physical_state: PhysicalState):
        '''
        pre: 
        post: physical_state.{group_address}  == False
        '''
        physical_state.{group_address} = False

    def is_on(self, physical_state: PhysicalState) -> bool:
        '''
        pre: 
        post: physical_state.{group_address}  == __return__
        '''
        return physical_state.{group_address}
    """
    )

    def __init__(
        self,
        filename: str,
        group_addresses: List[Tuple[str, str]],
        devices_instances: List[Tuple[str, str]],
    ):
        self.__filename: str = filename
        self.__group_addresses = group_addresses
        self.__devices_instances = devices_instances
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
        device_and_app_name_to_type_map = {}
        with open("generated/apps_bindings.json") as bindings_file:
            bindings_dict = json.load(bindings_file)
            for app in bindings_dict["appBindings"]:
                app_name = app["name"]
                for device in app["bindings"]:
                    device_name = device["name"]
                    type = device["binding"]["typeString"]
                    device_and_app_name_to_type_map[(app_name, device_name)] = type

        apps_dirs = [
            f.name
            for f in os.scandir("generated")
            if f.is_dir() and f.name != "__pycache__"
        ]
        code = []
        for app in apps_dirs:
            with open(f"generated/{app}/addresses.json") as file:
                devices_dict = json.load(file)
                for device in devices_dict["addresses"]:
                    name = device["name"]
                    type = device_and_app_name_to_type_map[(app, name)]
                    if type == "binary":
                        formatted_group_address = device["address"].replace("/", "_")
                        code.append(
                            self.__BINARY_SENSOR_TEMPLATE(
                                app, name, f"GA_{formatted_group_address}"
                            )
                        )
                    elif type == "temperature":
                        formatted_group_address = device["address"].replace("/", "_")
                        code.append(
                            self.__TEMPERATURE_SENSOR_TEMPLATE(
                                app, name, f"GA_{formatted_group_address}"
                            )
                        )
                    elif type == "humidity":
                        formatted_group_address = device["address"].replace("/", "_")
                        code.append(
                            self.__HUMIDITY_SENSOR_TEMPLATE(
                                app, name, f"GA_{formatted_group_address}"
                            )
                        )
                    elif type == "switch":
                        # Use one of the two addresses (writeAddress or readAddress), as they are actually the same
                        formatted_group_address = device["writeAddress"].replace(
                            "/", "_"
                        )
                        code.append(
                            self.__SWITCH_TEMPLATE(
                                app, name, f"GA_{formatted_group_address}"
                            )
                        )

        self.__code.extend(code)

    def __generate_devices_instances(self):
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

    def generate_verification_file(self):
        """
        Generates the whole verification file.
        """
        with open(self.__filename, "w") as file:
            self.__generate_physical_state_class()
            self.__generate_device_classes()
            self.__generate_devices_instances()
            file.write("\n".join(self.__imports))
            file.write("\n")
            file.write("\n".join(self.__code))
