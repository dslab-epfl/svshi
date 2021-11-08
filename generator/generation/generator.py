from generator.parsing.device import DeviceType, DeviceInstance
from generator.parsing.channel import ChannelType
from typing import List
import os
import subprocess


class Generator:
    def __init__(self, app_name: str):
        self.__app_name = app_name

    def copy_skeleton_to_generated_app(self):
        """
        Copies the skeleton to the newly generated app.
        """
        subprocess.run(f"cp -r generator/skeleton/* {self.__app_name}", shell=True)

    def generate_device_instances(self, device_instances: List[DeviceInstance]):
        """
        Generates the source code files for the device instances.
        """
        imports = ""
        devices = ""
        for device in device_instances:
            device_type_name = device.device_type.type
            device_type_class_name = device_type_name.capitalize()
            device_name = device.name

            import_statement = f"from {self.__app_name}.models.{device_type_name} import {device_type_class_name}\n"
            if import_statement not in imports:
                imports += import_statement
            devices += (
                f"{device_name.upper()} = {device_type_class_name}('{device_name}')\n"
            )

        file = f"""
{imports}
{devices}
        """.strip()

        output_filename = f"{self.__app_name}/devices.py"
        os.makedirs(os.path.dirname(output_filename), exist_ok=True)
        with open(output_filename, "w+") as output_file:
            output_file.write(file)

    def generate_multiton_class(self):
        """
        Generates the source code files for the multiton class.
        """
        file = f"""
def multiton(cls):
    instances = {{}}

    def getinstance(name):
        if name not in instances:
            instances[name] = cls(name)
        return instances[name]  

    return getinstance    
        """.strip()

        output_filename = f"{self.__app_name}/models/multiton.py"
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

    def generate_device_classes(self, device_types: List[DeviceType]):
        """
        Generates the source code files for the device classes.
        """

        def generate_device_class(device_type: DeviceType):
            channels_init = ""
            channels_imports = set()
            for channel in device_type.channels:
                channel_name = channel.name
                channel_type = "WriteChannel"
                if channel.type == ChannelType.OUT:
                    channel_type = "ReadChannel"
                elif channel.type == ChannelType.IN_OUT:
                    channel_type = "ReadWriteChannel"
                channels_imports.add(channel_type)
                # The first channel initialization is already indented
                channels_init += f"{'        ' if channels_init else ''}self.{channel_name} = {channel_type}(f'{{self.name}}.{channel_name}', '{channel.datatype}')\n"

            device_type_name = device_type.type
            file = f"""
from {self.__app_name}.communication.channel import {", ".join(channels_imports)}
from {self.__app_name}.models.multiton import multiton

@multiton
class {device_type_name.capitalize()}:
    def __init__(self, name):
        self.name = name
        {channels_init}  
            """.strip()

            output_filename = f"{self.__app_name}/models/{device_type_name}.py"
            os.makedirs(os.path.dirname(output_filename), exist_ok=True)
            with open(output_filename, "w+") as output_file:
                output_file.write(file)

        [generate_device_class(dt) for dt in device_types]
