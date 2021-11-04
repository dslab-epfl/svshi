from generator.parsing.device import DeviceType, DeviceInstance
from generator.parsing.channel import ChannelType
from typing import List
import os


def generate_device_instances(device_instances: List[DeviceInstance], app_name: str):
    imports = ""
    devices = ""
    for device in device_instances:
        device_type_name = device.device_type.type
        device_type_class_name = device_type_name.capitalize()
        device_name = device.name

        import_statement = f"from {app_name}.models.{device_type_name} import {device_type_class_name}\n"
        if import_statement not in imports:
            imports += import_statement
        devices += (
            f"{device_name.upper()} = {device_type_class_name}('{device_name}')\n"
        )

    file = f"""
{imports}
{devices}
    """.strip()

    output_filename = f"{app_name}/devices.py"
    os.makedirs(os.path.dirname(output_filename), exist_ok=True)
    with open(output_filename, "w+") as output_file:
        output_file.write(file)


def generate_multiton_class(app_name: str):
    file = f"""
def multiton(cls):
    instances = {{}}

    def getinstance(name):
        if name not in instances:
            instances[name] = cls(name)
        return instances[name]  

    return getinstance    
    """.strip()

    output_filename = f"{app_name}/models/multiton.py"
    os.makedirs(os.path.dirname(output_filename), exist_ok=True)
    with open(output_filename, "w+") as output_file:
        output_file.write(file)


def generate_device_class(device_type: DeviceType, app_name: str):
    channels_init = ""
    channels_imports = set()
    for channel in device_type.channels:
        channel_name = channel.name
        channel_type = "WriteChannel"
        if channel.type == ChannelType.OUT:
            channel_type = "ReadChannel"
        else:
            channel_type = "ReadWriteChannel"
        channels_imports.add(channel_type)
        channels_init += f"self.{channel_name} = {channel_type}('{channel_name}', '{channel.datatype}')\n"

    device_type_name = device_type.type
    file = f"""
from {app_name}.communication.channel import {", ".join(channels_imports)}
from {app_name}.models.multiton import multiton

@multiton
class {device_type_name.capitalize()}:
    def __init__(self, name):
        self.name = name
        {channels_init}  
    """.strip()

    output_filename = f"{app_name}/models/{device_type_name}.py"
    os.makedirs(os.path.dirname(output_filename), exist_ok=True)
    with open(output_filename, "w+") as output_file:
        output_file.write(file)
