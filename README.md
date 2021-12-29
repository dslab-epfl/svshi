# SVSHI - Secure and Verified Smart Home Infrastructure

<img src="logo/logo.png" alt="logo" width="300"/>

![CI](https://github.com/dslab-epfl/smartinfra/actions/workflows/ci.yml/badge.svg)
![Latest Stable Version](https://img.shields.io/github/v/release/dslab/smartinfra?label=version)
![License](https://img.shields.io/github/license/dslab/smartinfra)

- [SVSHI - Secure and Verified Smart Home Infrastructure](#svshi---secure-and-verified-smart-home-infrastructure)
  - [Installation](#installation)
    - [From archive](#from-archive)
    - [From sources](#from-sources)
    - [Docker](#docker)
  - [Supported devices](#supported-devices)
  - [Developing an application](#developing-an-application)
    - [Writing apps](#writing-apps)
    - [App example](#app-example)
  - [Running the applications](#running-the-applications)
  - [App generator](#app-generator)
    - [Prototypical structure](#prototypical-structure)
    - [Usage](#usage)
  - [CLI](#cli)
  - [How SVSHI works](#how-svshi-works)
    - [Compilation](#compilation)
      - [Bindings](#bindings)
    - [Verification](#verification)
      - [Static](#static)
      - [Runtime](#runtime)
    - [Execution](#execution)
  - [Contributing](#contributing)
  - [License](#license)

The **SVSHI** (**S**ecure and **V**erified **S**mart **H**ome **I**nfrastructure) project is a platform/runtime/toolchain for developing and running formally verified smart infrastructures, such as smart buildings, smart cities, etc.

It provides a [CLI](#cli), `svshi`, to interact easily with the platform.

With SVSHI, a user can develop and run Python applications interacting with [KNX](https://www.knx.org/knx-en/for-professionals/index.php) systems that are [formally verified](#verification) at both compile and run-time against a set of provided invariants.

## Installation

To work, SVSHI needs Python 3.8 (or newer) and Java 11 (or newer). Optionally, [sbt](https://www.scala-sbt.org) 1.5.5 (or newer) is needed to build from sources.

To check if the installation was successful, run `svshi version` in your terminal.

### From archive

To install SVSHI:

1. Download the latest version `tar.gz` archive from the [Releases](https://github.com/dslab-epfl/smartinfra/releases) page
2. Unzip it, then run `make install`
3. Add `$HOME/local/bin` (where the CLI executable is stored) to the path by adding `export PATH=$HOME/local/bin:$PATH` to your `.bash_profile`, `.zsh_profile`, etc.
4. Add the variable `SVSHI_HOME` to your environment by adding `export SVSHI_HOME=path/to/your/cloned/repo` to your `.bash_profile`, `.zsh_profile`, etc.

To update SVSHI, you just need to do the first two steps.

### From sources

To build from sources:

1. Clone the repo
2. Run `./build.sh`
3. Add `$HOME/local/bin` (where the CLI executable is stored) to the path by adding `export PATH=$HOME/local/bin:$PATH` to your `.bash_profile`, `.zsh_profile`, etc.
4. Add the variable `SVSHI_HOME` to your environment by adding `export SVSHI_HOME=path/to/your/cloned/repo` to your `.bash_profile`, `.zsh_profile`, etc.

### Docker

We also provide a Docker image with all requirements and SVSHI installed. To use it:

1. Run `./build_docker.sh` to build the image
2. Run `./run_docker.sh` to run the docker container. It opens a `sh` instance in the container with the current directory mapped to `/pwd` in the container

## Supported devices

- **Binary sensors** (deviceType = "binary")
- **Temperature sensors** (deviceType = "temperature")
- **Humidity sensors** (deviceType = "humidity")
- **Switches** (deviceType = "switch")

## Developing an application

To develop an app for SVSHI:

1. Create the devices prototypical structure file containing the list of the devices the app should use, as explained in the [app prototypical structure](#prototypical-structure) section.
2. Run the app generator, as explained in the [app generator](#app-generator) section, to get the app skeleton. It will be created under the `generated/` folder.
3. Run `svshi` to generate the bindings with `svshi generateBindings -f ets.knxproj`, where the argument is the _absolute_ path to the ETS project file.
4. Map the right physical ids given in `generated/physical_structure.json` to the right device in `generated/apps_bindings.json`. This is needed to provide the devices in the Python code with the group addresses to use. The first file represents the physical structure from the ETS project file, where each communication object has an id. The second one represents the apps structure with the devices and for each of them, the links they need.
5. [Write your app](#writing-apps).
6. Run `svshi` again to compile and [verify](#verification) the app with `svshi compile -f ets.knxproj`.

### Writing apps

To write an app, you mainly have to modify the `main.py` file, optionally adding dependencies to the provided `requirements.txt` in the project you generated following the [app generator](#app-generator) section.

All the device instances you can use are already imported in `main.py`. They mirror what has been defined in the device prototypical structure file.

There are two important functions in `main.py`, `invariant()` and `iteration()`. In the first one you should define all the conditions (or _invariants_) that the entire KNX system must satisfy throughout execution of **all** applications, while in the second you should write the app code.

An important thing to be aware of is that `iteration()` cannot use external libraries directly. Instead, these calls have to be defined first inside _unchecked functions_, which are functions whose name starts with `unchecked` and whose return type is explicitly stated, and only then they can be used in `iteration()`.

In addition, note that `invariant()` must return a boolean value, so any kind of boolean expression containing the _read_ properties of the devices and constants is fine. However, here you **cannot** perform operations with side effects, call external libraries nor unchecked functions.

**Unchecked functions** are used as a compromise between usability and formal verification, and as such must be used as little as possible: their content is not verified by SVSHI. Furthermore, they should be short and simple: we encourage developers to add one different unchecked function for each call to an external library. All logic that does not involve calls to the library should be done in `iteration()` to maximise code that is indeed formally verified.
Nonetheless, you can help the verification deal with their presence by annotating their docstring with _post-conditions_.

Functions' **post-conditions** define a set of _axioms_ on the return value of the function: these conditions are assumed to be always true by SVSHI during verification. They are defined like this: `post: __return__ > 0`. You can add as much post-conditions as you like and need. However, keep in mind that these conditions are **assumed to be true** during formal verification! If these do not necessarily hold with respect to the external call, bad results can occur at runtime even though the code verification was successful!

An example with multiple post-conditions could be:

```python
def unchecked_function() -> int:
  """
  post: __return__ > 0
  post: __return__ != 3
  """
  return external_library_get_int()
```

### App example

In `main.py`:

```python
from devices import BINARY_SENSOR, SWITCH


def invariant() -> bool:
    # The switch should be in the same state as the binary sensor
    return (BINARY_SENSOR.is_on() and SWITCH.is_on()) or (not BINARY_SENSOR.is_on() and not SWITCH.is_on())


def iteration():
    if BINARY_SENSOR.is_on():
        SWITCH.on()
    else:
        SWITCH.off()
```

## Running the applications

To run all the installed apps (with [runtime verification](#runtime) enabled):

1. In [ETS](https://www.knx.org/knx-en/for-professionals/software/ets-professional/), assign to each communication object the right group address as presented in `assignments/assignment.txt` and program the devices.
2. Execute `svshi run -a address:port`, where address is the KNX IP gateway address and port is the KNX IP gateway port.

## App generator

### Prototypical structure

This JSON file is given by the programmer/developer that wants to develop an application. It represents the prototypical devices that the app needs with their types. It also specifies whether the app is _privileged_ or not (`"permissionLevel": "privileged" | "notPrivileged"`). A privileged app can override the behavior of the non-privileged ones.

The `timer` attribute can be used to run the application even though the physical state has not changed. 
- if `timer == 0` the application runs when the physical state changes
- if `timer > 0` the application runs when the physical state changes AND every `timer` seconds.

Once the app is generated, it is moved to the `generated` folder.

Here is an example:

```json
{
  "permissionLevel": "notPrivileged",
  "timer": 60,
  "devices": [
    {
      "name": "name_of_the_instances",
      "deviceType": "type_of_the_devices"
    }
  ]
}
```

The `name` is used as the instance name in the Python app that is generated. It should then be unique in a given app, and should follow the Python variables naming conventions: no whitespaces nor numbers.
The `deviceType` should be [supported by SVSHI](#supported-devices).

### Usage

The app generator takes as input the devices JSON file and the name of the app.

Before executing it, you need to create the file containing the list of the devices the app should use, as explained [above](#prototypical-structure).

To execute the generator, run `svshi generateApp -d devices.json -n app_name`, where the first argument is the _absolute_ path to the prototypical structure file and the second one (`app_name` in the example) is the name of the app to be used. The name has to follow the same rules as for Python modules: short, all-lowercase names. Underscores can be used if it improves readability.

## CLI

You can run `svshi --help` to display the following:

```
svshi
Secure and Verified Smart Home Infrastructure
  task <command>           The task to run. Can be passed as is. Possible options are 'run', 'compile', 'generateBindings', 'generateApp', 'listApps' and 'version'. The argument is not case sensitive.
  -f --ets-file <str>      The ETS project file to use for the tasks 'compile' and 'generateBindings'
  -d --devices-json <str>  The devices prototypical structure JSON file to use for the task 'generateApp'
  -n --app-name <str>      The app name to use for the task 'generateApp'
  -a --address <str>       The KNX address to use for the task 'run'. The correct format is 'address:port' (ex: 192.168.1.1:5555)
  --no-colors              The flag to disable output coloring
```

Available commands are:

- `svshi run -a 1.1.1.1:5555` to run all the apps with [runtime verification](#runtime)
- `svshi compile -f ets.knxproj` to compile all the apps
- `svshi generateBindings -f ets.knxproj` to generate the bindings for all the apps
- `svshi generateApp -d devices.json -n app_name` to generate a new Python app
- `svshi listApps` to list all the installed apps
- `svshi version` to display the CLI version

Shorter aliases are also available:

- `svshi r` for `svshi run`
- `svshi c` for `svshi compile`
- `svshi gb` for `svshi generateBindings`
- `svshi ga` for `svshi generateApp`
- `svshi la` for `svshi listApps`
- `svshi v` for `svshi version`

## How SVSHI works

### Compilation

The compiler combines all applications already installed (in `app_library`) with new applications (in `generated`). It generates the bindings between physical and prototypical devices communication objects, assigns group addresses to used physical communication objects and produces useful files for the runtime.

When compiling applications, if the verification passed all checks, applications from `generated` are moved into `app_library` to become _installed_ applications.

#### Bindings

The compiler generates the bindings file to let the developer map physical device communication objects (from the ETS project) to prototypical devices from applications.

Bindings for the installed applications are stored and when `svshi generateBindings -f ets.knxproj` is called, the new bindings reuse current bindings **if the physical structure did not change** since last application installation. Therefore, only bindings for new applications are empty and must be filled. If the physical structure changed, the bindings file is a fresh one and all bindings must be entered again.

### Verification

#### Static

When compiling, the apps are also verified to make sure each one of them satisfies the invariants of each app. If not, the procedure fails with helpful error messages. Therefore, the static verification not only catches apps violating the invariants, but it also ensures compatibility between installed apps.

#### Runtime

Whenever an app wants to update the KNX system, SVSHI verifies whether the update could break the apps' invariants. If it is the case, the app is prevented from running, otherwise the updates are propagated to KNX.

### Execution

SVSHI's runtime is **reactive** and **event-based**. Applications _listen_ for changes to the group addresses of the devices they use, and are run on a state change (an _event_). The state transition can be triggered externally by the KNX system or by another app, which then proceeds to notify all the other listeners.

_Running an application_ concretely means that its `iteration()` function is executed on the current state of the system.

Apps are always run in alphabetical order in their group (`privileged` or `notPrivileged`). The non-privileged apps run first, then the privileged ones: in such a way privileged applications can override the behavior of non-privileged ones.

This execution model has been chosen for its ease of use: users do not need to write `while` loops or deal with synchronization explicitly.

## Contributing

See [the contributing guide](/CONTRIBUTING.md) for detailed instructions on how to get started with the project.

## License

SVSHI is licensed under the [MIT license](/LICENSE).
