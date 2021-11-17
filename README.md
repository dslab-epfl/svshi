# Formal Verification for Smart Infrastructure

![CI](https://github.com/dslab-epfl/smartinfra/actions/workflows/ci.yml/badge.svg)

This project is about developing a platform/runtime/toolchain for developing and running formally verified smart infrastructures, such as smart buildings, smart cities, etc.

## Supported devices

- **Binary sensors** (deviceType = "binary")
- **Temperature sensors** (deviceType = "temperature")
- **Humidity sensors** (deviceType = "humidity")
- **Switches** (deviceType = "switch")

## App generator

### Prototypical structure

This JSON file is given by the programmer/developer that wants to develop an application. It represents the prototypical devices that the app needs with their types.

This file has to be named `app_prototypical_structure.json` and needs to be saved at the root of the project, i.e. the same level as `core`, `app-library` etc. Once the app is generated, it is moved to the app folder.

Here is an example:

```json
{
  "devices": [
    {
      "name": "name_of_the_instances",
      "deviceType": "type_of_the_devices"
    }
  ]
}
```

The `name` is used as the instance name in the Python app that is generated. It should then be unique in a given app, and should follow the Python variables naming conventions: no whitespaces nor numbers.
The `deviceType` should be [supported by Pistis](#supported-devices).

### Usage

```text
usage: main.py [-h] devices_json app_name

App generator.

positional arguments:
  devices_json  the name of the devices JSON file
  app_name      the name of the app

optional arguments:
  -h, --help    show this help message and exit
```

The app generator is a small Python module that generates a Python app skeleton to be modified by the user. It takes as input the devices JSON file and the name of the app.

It requires Python >= 3.8 and a Unix-based OS (Linux or macOS).

Before executing it, you need to create the file `app_prototypical_structure.json` containing the list of the devices the app should use, as explained [above](#prototypical-structure).

To execute the generator, run `python -m generator.main app`, where the first argument (`app` in the example) is the name of the app to be used. The name has to follow the same rules as for Python modules: short, all-lowercase names. Underscores can be used if it improves readability.
