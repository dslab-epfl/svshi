from ..parsing.device import Device
from typing import List
import os
import shutil


class Generator:
    """
    A Python code generator.
    """

    def __init__(
        self, app_name: str, devices: List[Device], devices_json_filename: str
    ):
        self.__app_name = app_name
        self.__devices = devices
        self.__devices_json_filename = devices_json_filename

    def copy_skeleton_to_generated_app(self, skeleton_path: str):
        """
        Copies the skeleton to the newly generated app. The skeleton path should have the form "$SVSHI_HOME/generator/skeleton".
        """
        shutil.copytree(skeleton_path, self.__app_name, dirs_exist_ok=True)

    def move_devices_json_to_generated_app(self):
        """
        Moves the devices json to the newly generated app, renaming it `app_prototypical_structure.json`.
        """
        shutil.move(self.__devices_json_filename, f"{self.__app_name}/app_prototypical_structure.json")

    def generate_multiton_class(self):
        """
        Generates the source code file for the multiton class.
        """
        allowed_names = list(map(lambda d: d.name, self.__devices))
        file = f'''
###
### DO NOT TOUCH THIS FILE!!!
###

def multiton(cls):
    """
    Makes the given class a multiton: only a limited number of instances can exist. If an instance with
    a given name already exists, it is returned instead of creating a new object.
    """
    instances = {{}}
    allowed_names = {allowed_names}
    def getinstance(name):
        if name not in allowed_names:
            raise ValueError(f"Name '{{name}}' not allowed")
        if name not in instances:
            instances[name] = cls(name)
        return instances[name]  
    return getinstance    
        '''.strip()

        output_filename = f"{self.__app_name}/models/multiton.py"
        os.makedirs(os.path.dirname(output_filename), exist_ok=True)
        with open(output_filename, "w+") as output_file:
            output_file.write(file)

    def generate_instances(self):
        """
        Generates the source code file for the device instances and the app state instance.
        """
        imports = ""
        devices_code = ""
        for device in self.__devices:
            device_type_name = device.type
            device_type_import_name = device.import_module_name
            device_name = device.name

            import_statement = (
                f"from models.{device_type_import_name} import {device_type_name}\n"
            )
            if import_statement not in imports:
                imports += import_statement
            devices_code += (
                f"{device_name.upper()} = {device_type_name}('{device_name}')\n"
            )

        file = f"""
###
### DO NOT TOUCH THIS FILE!!!
###

{imports}
from models.state import AppState
from models.SvshiApi import SvshiApi

{devices_code}
svshi_api = SvshiApi()
app_state = AppState()
        """.strip()

        output_filename = f"{self.__app_name}/instances.py"
        os.makedirs(os.path.dirname(output_filename), exist_ok=True)
        with open(output_filename, "w+") as output_file:
            output_file.write(file)

    def add_instances_imports_to_main(self):
        """
        Writes import statements in `main.py` for the newly created instances.
        """
        with open(f"{self.__app_name}/main.py", "r+") as fp:
            lines = fp.readlines()
            lines.insert(0, "from instances import app_state, svshi_api")
            nb_devices = len(self.__devices)
            for i, device in enumerate(self.__devices):
                suffix = "\n\n" if i == nb_devices - 1 else ""
                lines.insert(i + 1, f", {device.name.upper()}{suffix}")

            fp.seek(0)
            fp.writelines(lines)

    def generate_init_files(self):
        """
        Generates the __init__.py file in each subdirectory of the generated app.
        """
        subdirectories = [x[0] for x in os.walk(self.__app_name)]
        for subdir in subdirectories:
            with open(f"{subdir}/__init__.py", "w+") as _:
                pass
