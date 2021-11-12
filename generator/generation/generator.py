from generator.parsing.device import Device
from typing import List
import os
import subprocess


class Generator:
    """
    A Python code generator.
    """

    def __init__(self, app_name: str):
        self.__app_name = app_name

    def copy_skeleton_to_generated_app(self):
        """
        Copies the skeleton to the newly generated app.
        """
        subprocess.run(f"cp -r generator/skeleton/* {self.__app_name}", shell=True)

    def generate_device_instances(self, devices: List[Device]):
        """
        Generates the source code files for the device instances.
        """
        imports = ""
        devices_code = ""
        for device in devices:
            device_type_name = device.type
            device_type_import_name = device.import_module_name
            device_name = device.name

            import_statement = f"from models.{device_type_import_name} import {device_type_name}\n"
            if import_statement not in imports:
                imports += import_statement
            devices_code += (
                f"{device_name.upper()} = {device_type_name}('{device_name}')\n"
            )

        file = f"""
{imports}
{devices_code}
        """.strip()

        output_filename = f"{self.__app_name}/devices.py"
        os.makedirs(os.path.dirname(output_filename), exist_ok=True)
        with open(output_filename, "w+") as output_file:
            output_file.write(file)

    def generate_init_files(self):
        """
        Generates the __init__.py file in each subdirectory of the generated app.
        """
        subdirectories = [x[0] for x in os.walk(self.__app_name)]
        for subdir in subdirectories:
            with open(f"{subdir}/__init__.py", "w+") as _:
                pass
