# Formal Verification for Smart Infrastructure

![CI](https://github.com/dslab-epfl/smartinfra/actions/workflows/ci.yml/badge.svg)

- [Formal Verification for Smart Infrastructure](#formal-verification-for-smart-infrastructure)
  - [Requirements](#requirements)
  - [Supported devices](#supported-devices)
  - [Setup](#setup)
  - [Developing an application](#developing-an-application)
  - [Running the applications](#running-the-applications)
  - [Running the tests](#running-the-tests)
  - [App generator](#app-generator)
    - [Prototypical structure](#prototypical-structure)
    - [Usage](#usage)
    - [Tests](#tests)

This project is about developing a platform/runtime/toolchain for developing and running formally verified smart infrastructures, such as smart buildings, smart cities, etc.

## Requirements

- Unix-based OS (Linux or macOS).
- [Python](https://www.python.org) 3.8 or newer.
- [Scala](https://www.scala-lang.org) 2.13.x or newer. Scala 3 is not yet supported.
- [sbt](https://www.scala-sbt.org) 1.5.5 or newer.

## Supported devices

- **Binary sensors** (deviceType = "binary")
- **Temperature sensors** (deviceType = "temperature")
- **Humidity sensors** (deviceType = "humidity")
- **Switches** (deviceType = "switch")

## Setup

To install all the Python dependencies, run `pip install -r requirements.txt`.

## Developing an application

To develop an app for Pistis:

1. Create the `app_prototypical_structure.json` file containing the list of the devices the app should use, as explained in the [app prototypical structure](#prototypical-structure) section.
2. Run the app generator, as explained in the [app generator](#app-generator) section, to get the app skeleton. It will be created under the `generated/` folder.
3. Inside `core/`, run Pistis to generate the bindings with `sbt "run generateBindings ../ets.knxproj"`, where the argument is the path to the ETS project file.
4. Map the right physical ids given in `generated/physical_structure.json` to the right device in `generated/apps_bindings.json`. This is needed to provide the devices in the Python code with the group addresses to use. The first file represents the physical structure from the ETS project file, where each communication object has an id. The second one represents the apps structure with the devices and for each of them, the links they need.
5. Write your app.
6. Run Pistis to compile and verify the app with `sbt "run compile ../ets.knxproj"`.

## Running the applications

To run all the installed apps:

1. In [ETS](https://www.knx.org/knx-en/for-professionals/software/ets-professional/), assign to each communication object the right group address as presented in `assignments/assignment.txt`.
2. Execute `python -m runtime.main` at the root of the project.

## Running the tests

To run the tests of all the modules at once, execute `./run_tests.sh`.

## App generator

### Prototypical structure

This JSON file is given by the programmer/developer that wants to develop an application. It represents the prototypical devices that the app needs with their types.

This file has to be named `app_prototypical_structure.json` and needs to be saved at the root of the project, i.e. the same level as `core`, `app_library` etc. Once the app is generated, it is moved to the `generated` folder.

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
usage: main.py [-h] app_name

App generator.

positional arguments:
  app_name    the name of the app

optional arguments:
  -h, --help  show this help message and exit
```

The app generator is a small Python module that generates a Python app skeleton to be modified by the user. It takes as input the devices JSON file and the name of the app.

It requires Python >= 3.8 and a Unix-based OS (Linux or macOS).

Before executing it, you need to create the file `app_prototypical_structure.json` containing the list of the devices the app should use, as explained [above](#prototypical-structure).

To execute the generator, run `python -m generator.main app`, where the first argument (`app` in the example) is the name of the app to be used. The name has to follow the same rules as for Python modules: short, all-lowercase names. Underscores can be used if it improves readability.

### Tests

To run the tests, execute `pytest` in `generator/`.
