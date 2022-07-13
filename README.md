# SVSHI - Secure and Verified Smart Home Infrastructure

<p align="center">
  <img src="src/res/logo.png" alt="logo" width="50%"/>
</p>

![CI](https://github.com/dslab-epfl/svshi_private/actions/workflows/ci.yml/badge.svg)
[![codecov](https://codecov.io/gh/dslab-epfl/svshi_private/branch/main/graph/badge.svg?token=NYRGL343U2)](https://codecov.io/gh/dslab-epfl/svshi)
![Latest Stable Version](https://img.shields.io/github/v/release/dslab/svshi?label=version)
![License](https://img.shields.io/github/license/dslab/svshi)

- [SVSHI - Secure and Verified Smart Home Infrastructure](#svshi---secure-and-verified-smart-home-infrastructure)
  - [Installation](#installation)
  - [GUI](#gui)
    - [Docker (can be used on Windows or Unix machine) <-- RECOMMENDED](#docker-can-be-used-on-windows-or-unix-machine----recommended)
    - [Unix (Linux / macOS)](#unix-linux--macos)
    - [From archive](#from-archive)
      - [Unix (Linux / macOS)](#unix-linux--macos-1)
      - [Windows](#windows)
    - [From sources](#from-sources)
      - [Unix (Linux / macOS)](#unix-linux--macos-2)
      - [Windows](#windows-1)
    - [Docker](#docker)
    - [Unix (Linux / macOS)](#unix-linux--macos-3)
  - [Supported devices](#supported-devices)
  - [Developing an application](#developing-an-application)
    - [Writing apps](#writing-apps)
      - [SVSHI built-in functions](#svshi-built-in-functions)
    - [App example](#app-example)
  - [Running the applications](#running-the-applications)
  - [Updating an application](#updating-an-application)
  - [App generator](#app-generator)
    - [Prototypical structure](#prototypical-structure)
    - [Usage](#usage)
  - [CLI](#cli)
  - [Simulator](#simulator)
  - [How SVSHI works](#how-svshi-works)
    - [Compilation](#compilation)
      - [Bindings](#bindings)
    - [Verification](#verification)
      - [Static](#static)
      - [Runtime](#runtime)
    - [Execution](#execution)
  - [GUI](#gui-1)
    - [JSON API](#json-api)
  - [KNX Virtual](#knx-virtual)
  - [Contributing](#contributing)
  - [White paper](#white-paper)
  - [License](#license)

The **SVSHI** (**S**ecure and **V**erified **S**mart **H**ome **I**nfrastructure) (pronounced like "sushi") project is a platform/runtime/toolchain for developing and running formally verified smart infrastructures, such as smart buildings, smart cities, etc.

It provides a [CLI](#cli), `svshi`, to interact easily with the platform.

With SVSHI, a user can develop and run Python applications interacting with [KNX](https://www.knx.org/knx-en/for-professionals/index.php) systems that are [formally verified](#verification) at both compile and run-time against a set of provided invariants.

## Installation

We strongly recommend to use SVSHI on Docker with the GUI, see [Docker (can be used on Windows or Unix machine)](#docker-can-be-used-on-windows-or-unix-machine).

To work, SVSHI needs Python 3.9 (or newer)([download here](https://www.python.org/downloads/)), `pip` (`pip` will be automatically installed by scripts on Windows, on Linux simply use `sudo apt install python3-pip` and on MacOS use `python3 -m ensurepip`) and Java 11 (or newer)([download here](https://www.oracle.com/java/technologies/downloads/)). Optionally, [sbt](https://www.scala-sbt.org) 1.5.5 (or newer) is needed to build from sources.

To check if the installation was successful, run `svshi version` in your terminal.

## GUI

SVSHI comes with a GUI in the form of a web application. For this usage, we strongly recommend to use the provided Docker image.

### Docker (can be used on Windows or Unix machine) <-- RECOMMENDED

Here the SVSHI system and the frontend server run in the same Docker image. To use it:

- Run `cd ./scripts && ./build_docker.sh` (Windows: `.ps1`) to build the image
- Run `cd ./scripts && ./run_docker.sh` to run the docker container
- Open a browser and navigate to `http://localhost:3000` (or replace `localhost` by the IP of the machine running the Docker)

Do not forget to use volumes (see https://docs.docker.com/storage/volumes/) if you want your container files to be non-volatile. The scripts that we provide that run the container already create a volume and use it for the container. Feel free to modify those scripts if you are an advanced user of Docker.

### Unix (Linux / macOS)

You need to have `npm` installed on your system!

To use the GUI:

- run `svshi gui` in a terminal to start SVSHI
- run the script `start_ui.sh` in another terminal
- connect to [http://localhost:3000](http://localhost:3000) in your favorite browser

When you want to stop, kill both processes running in terminals.

> If you are running SVSHI on a smart building infrastructure, DO NOT kill `svshi gui`, it would stop running on your building installation! You can however stop the `start-ui.sh` process.

### From archive

#### Unix (Linux / macOS)

To install SVSHI on Linux or macOS:

1. Download the latest version `.zip` file from the [Releases](https://github.com/dslab-epfl/svshi/releases) page
2. Unzip it, move the folder to the desired installation site, then run `cd ./scripts && ./install.sh` inside it.
3. Add `$HOME/local/bin` (where the CLI executable is stored) to the path by adding `export PATH=$HOME/local/bin:$PATH` to your `.bash_profile`, `.zsh_profile`, etc.
4. Add the variable `SVSHI_HOME` to your environment by adding `export SVSHI_HOME=path/to/the/svshi/folder` (a path example is `~/svshi-v1.1.0`) to your `.bash_profile`, `.zsh_profile`, etc.

To update SVSHI, you just need to do the first two steps. However, do not forget to reinstall the applications (stored in `installedApps`) by moving all the `installedApps` content in the new version's `generated` folder and compiling.

#### Windows

To install SVSHI on Windows:

1. Download the latest version `.zip` file from the [Releases](https://github.com/dslab-epfl/svshi/releases) page
2. Unzip the archive, move the unzipped folder to the desired installation site. The moved folder must contain (among other things) a `src` folder and some scripts.
3. Open a Powershell instance a `CD` into the unzipped directory.
4. Setup `python3` by executing the script `.\setup-python3.ps1`. This sets up a `python3` alias if it does not exist.
   > The command `py`, `python` or `python3` must be available in your Powershell environment for this script to work!
5. Reboot your computer.
6. Execute `.\install-pip.ps1` to install pip on your computer
7. Run the `.\install.ps1` script inside the unzipped folder to install the program.

> IMPORTANT! use Powershell to execute the scripts!

To update SVSHI, you just need to do steps 1, 2, 3 and 7. However, do not forget to reinstall the applications (stored in `installedApps`) by moving all the `installedApps` content in the new version's `generated` folder and compiling.

### From sources

#### Unix (Linux / macOS)

To build from sources on Linux or macOS:

1. Clone the repository
2. Run `cd ./scripts && ./build.sh`
3. Add `$HOME/local/bin` (where the CLI executable is stored) to the path by adding `export PATH=$HOME/local/bin:$PATH` to your `.bash_profile`, `.zsh_profile`, etc.
4. Add the variable `SVSHI_HOME` to your environment by adding `export SVSHI_HOME=path/to/your/cloned/repo` to your `.bash_profile`, `.zsh_profile`, etc.

To update SVSHI, you just need to pull the latest changes and perform step 2. However, do not forget to reinstall the applications (stored in `installedApps`) by moving all the `installedApps` content in the new version's `generated` folder and compiling.

#### Windows

To build from sources on Windows:

1. Clone the repository
2. Open a Powershell instance and `CD` into the unzipped directory.
3. Setup `python3` by executing the script `.\setup-python3.ps1`. This sets up a `python3` alias if it does not exist.
   > The command `py`, `python` or `python3` must be available in your Powershell environment for this script to work!
4. Reboot your computer.
5. Execute `.\install-pip.ps1` to install pip on your computer
6. Run the `.\install.ps1 -build $true` script inside the cloned folder to install the program.

> IMPORTANT! use Powershell to execute the scripts!

To update SVSHI, you just need to pull the latest changes and perform steps 2 and 6. However, do not forget to reinstall the applications (stored in `installedApps`) by moving all the `installedApps` content in the new version's `generated` folder and compiling.

### Docker

We also provide a Docker image with all requirements and SVSHI installed. To use it:

1. Run `cd ./scripts && ./build_docker.sh` to build the image
2. Run `cd ./scripts && ./run_docker.sh` to run the docker container. It opens a `sh` instance in the container with the current directory mapped to `/pwd` in the container
3. You can find the repo copied in `/home/maki/svshi`
4. `svshi` command is accessible

### Unix (Linux / macOS)

You need to have `npm` installed on your system!

To use the GUI:

- run `svshi gui` in a terminal to start SVSHI
- run the script `start_ui.sh` in another terminal
- connect to [http://localhost:3000](http://localhost:3000) in your favorite browser

When you want to stop, kill both processes running in terminals.

> If you are running SVSHI on a smart building infrastructure, DO NOT kill `svshi gui`, it would stop running on your building installation! You can however stop the `start-ui.sh` process.

## Supported devices

- **Binary sensors** (deviceType = "binary")
- **Temperature sensors** (deviceType = "temperature")
- **Humidity sensors** (deviceType = "humidity")
- **CO2 sensors** (deviceType = "co2")
- **Switches** (deviceType = "switch")
- **Dimmer Actuator** (deviceType = "dimmerActuator")
- **Dimmer Sensor** (deviceType = "dimmerSensor")

## Developing an application

To develop an app for SVSHI:

1. Create the devices prototypical structure file containing the list of the devices the app should use, as explained in the [app prototypical structure](#prototypical-structure) section.
2. Run the app generator, as explained in the [app generator](#app-generator) section, to get the app skeleton. It will be created under the `generated/` folder.
3. [Write your app](#writing-apps).
4. Run `svshi` to generate the bindings with `svshi generateBindings -f ets.knxproj`, where the argument is the _absolute_ path to the ETS project file.
    > Note that SVSHI supports a .json file as input instead of the ets.knxproj file but this should be use only when using the simulator!
5. Map the right physical ids given in `generated/physical_structure.json` to the right device in `generated/apps_bindings.json`. This is needed to provide the devices in the Python code with the group addresses to use. The first file represents the physical structure from the ETS project file, where each communication object has an id. The second one represents the apps structure with the devices and for each of them, the links they need.
6. Run `svshi` again to [compile](#compilation) and [verify](#verification) the app with `svshi compile -f ets.knxproj`.
    > Note that SVSHI supports a .json file as input instead of the ets.knxproj file but this should be use only when using the simulator!

### Writing apps

To write an app, you mainly have to modify the `main.py` file, optionally adding dependencies into the `requirements.txt` file provided in the generated project. To understand how the project is generated, please refer to the [app generation section](#app-generator).

All the available device instances are already imported in `main.py`. They mirror what has been defined in the device prototypical structure file.

The application can use external files. They must live in the `files` folder at the root of the application folder. Calling `open` directly is forbidden. One must use the functions provided by `svshi_api` to access files. Files are managed by the SVSHI runtime and thus all interactions must go through `svshi_api` functions. Please refer to the [SVSHI built-in functions](#SVSHI-built-in-functions) section for details about these functions.

There are two important functions in `main.py`, `invariant()` and `iteration()`. In the first one the user should define all the conditions (or _invariants_) that the entire KNX system must satisfy throughout execution of **all** applications, while in the second she should write the app code.

An important thing to be aware of is that `iteration()` cannot use external libraries directly. Instead, these calls have to be defined first inside _periodic_ or _on\_trigger_ functions, which are functions whose name starts with `periodic`, respectively `on_trigger` and whose return type is explicitly stated. Then, these functions can be used in `iteration()`.

In addition, note that `invariant()` must return a boolean value, so any kind of boolean expression containing the _read_ properties of the devices and constants is fine. However, here operations with side effects, external libraries calls, `periodic` and `on_trigger` functions calls are **not** allowed.

**Periodic and on_trigger functions** are used to empower performance and formal verification, while slightly reducing utility. They are meant to encapsulate calls to external libraries, as such calls might be slow and have higher chances to crash. They are run asynchronously, so that even if they need time to execute, they will not slow down the apps. Their content is not verified by SVSHI and they are allowed to crash, which will not impact the running apps. This is why, when you retrieve the result you should treat it as unsafe input and expect **any** value of the correct type, or `None` (if the function has not been executed, yet).

Note that some modules are forbidden to use even in periodic and on_trigger functions. For now, the `time` module is forbidden, please use the time provided by the SVSHI_API, see [SVSHI built-in functions](#SVSHI-built-in-functions).

To execute and retrieve values of such functions, use the provided api available through the `svshi_api` object: `svshi_api.trigger_if_not_running` and `svshi_api.get_latest_value` (more details below).

**Periodic functions** should have a name starting with `periodic` and a period in seconds defined in the docstring like this: `period: 3` (meaning a period of 3 seconds). They are not allowed to have any argument as input. Such functions are automatically executed by svshi periodically, according to the given period. A period of X seconds means that there will be X seconds between each executions' **start**, unless the function takes more than X seconds to execute (in that case, next execution starts immediately after the function terminates). If the provided period is `0`, the function is executed as often as possible, but not more often than every 0.5 second. Here is an example:

```python
def periodic_function() -> int:
  """
  period: 10
  """
  return external_library_get_int()
```

**On_trigger functions** have a name starting with `on_trigger`. They may take some arguments as input, but are not allowed to have default arguments, *args or **kwargs.

**svshi_api.trigger_if_not_running(fn)(args)** is used to trigger the execution of an `on_trigger` function. See the example below on how it should be used.

```python
def on_trigger_function(x: int, y: bool) -> int:
  return external_library_get_int(x, y)

# Somewhere in the iteration function:
svshi_api.trigger_if_not_running(on_trigger_function)(3, True)
```

**svshi_api.get_latest_value(fn)** is used to retrieve the latest value returned by a periodic of on_trigger function. Remember that you should assume it to be **any** value of the correct type, or `None` if the function was never executed yet. The verification will fail if any of the returned values leads to an invalid state, which is why you should sanitize the received value.
Here is an example of retrieving a value:

```python
def on_trigger_function(x: int, y: bool) -> int:
  return external_library_get_int(x, y)

# Somewhere in the iteration function:
x = svshi_api.get_latest_value(on_trigger_function)
# Since on_trigger_functions returns an int, x might be any integer value, or None

# Suppose you expect a value between -5 and 30.
if x is None or x < -5 or x > 30:
  # Some default and safe behaviour.
else:
  # Regular behaviour, using x.
```

Furthermore, applications have access to a set of variables (the _app state_) they can use to keep track of state between calls. Indeed, the `iteration()` function is called in an [event-based manner](#execution) (either the KNX devices' state changes or a periodic app's timer is finished). All calls to `iteration()` are independent and thus SVSHI offers a way to store some state that will live in between calls. There is a local state instance _per app_.

To do so, in `main.py` the `app_state` instance is imported along with the devices. This is a [dataclass](https://docs.python.org/3/library/dataclasses.html) and it contains 4 fields of each of the following types: `int`, `float`, `bool`. The fields are called respectively `INT_X`, `FLOAT_X`, `BOOL_X` where X equals 0, 1, 2 or 3.

These values can be used in `iteration()` and `invariant()`. One should be careful while using it in `invariant()` or in a condition that will affect the KNX installation's state (the _physical_ state): the formal verification would fail if ANY possible value of the `app_state` leads to an invalid state after running `iteration()` even if this case should not occur because of previous calls to `iteration()` that would have set the values.

#### SVSHI built-in functions

A `svshi_api` instance is imported in every app, like the `app_state`.
This instance offers several functions used to track the system's time in applications:

- `get_hour_of_the_day` returns an integer between 0 and 23.
- `get_minute_in_hour` returns an integer between 0 and 59.
- `get_day_of_week` returns an integer between 1 and 7, where 1 corresponds to Monday.
- `get_day_of_month` returns an integer between 1 and 31, corresponding to the day of the month.
- `get_month_in_year` returns an integer between 1 and 12, where 1 corresponds to January.
- `get_year`return the current year.
Example: For Wed Apr 13 15:57:17 2022 UTC `get_hour_of_the_day` returns `15`, `get_minute_in_hour` returns `57`, `get_day_of_week` returns `3`, `get_day_of_month` returns `13`, `get_month_in_year` returns `4` and `get_year` returns `2022`.
This allows to put time constraints into the invariant and to write and verify time sensitive applications . Therefore, it can be used for the code of `iteration`, in `invariant` and in pre- and post- conditions.

As seen before, the `svshi_api` instance also offers a way to trigger and retrieve values of periodic and on_trigger functions (via `trigger_if_not_running` and `get_latest_value`).

The `svshi_api` instance additionally offers functions to interact with external files:

- `get_file_text_mode(file_name, mode)`: open the file with the given name in the corresponding mode as a text file and return the `IO[str]` instance (same as returned by `open`) or `None` if an error occured. The mode can be `a`, `w`, `ar` or `wr`
- `get_file_binary_mode(file_name, mode)`: open the file with the given name in the corresponding mode as a binary file and return the `IO[bytes]` instance (same as returned by `open`) or `None` if an error occured. The mode can be `a`, `w`, `ar` or `wr`
- `get_file_path(file_name)`: returns the path to the file with the given filename as managed by SVSHI. This can be used to pass as arguments to some libraries. However, you must use use the `get_file_...` functions to open the file directly!

To ease the verification process of time sensitive functions, a `check_time_property` function is offered:

`check_time_property` has three arguments : `frequency`,`duration` and finally the condition.
- `frequency` and `duration` time are given in `svshi_api`, already imported on the `main.py`.
- `frequency` indicates how often the property must be valid. Having `svshi_api.Day(2)` for frequency means that the property must be true every two days.
- `duration` states for how long the property holds. For example, if `duration` is `svshi_api.Minute(10)`, the property must be always valid for 10 minutes, continuously.

Classes in `svshi_api` are used to represent various time durations; Possible representations are `Year`,`Month`,`Week`,`Day`,`Hour` and `Minute`.
Each of these instances take one value at construction, the amount of time. Example: `svshi_api.Day(5)` represent 5 days.
- `condition` is the proposition that needs to be valid for some time at the given frequency. WARNING: Do not use any previously declared variables or the verification will fail.
For example, if you want that the `switch_one` is on every hour for ten minutes, you can write:
`svshi_api.check_time_property(frequency=Hour(1),duration=Minute(10),condition=switch_one.is_on()`
NB: This function can only be used in the `invariant` and cannot be used in `iteration`.

### App example

In `main.py`:

```python
from instances import app_state, BINARY_SENSOR, SWITCH


def invariant() -> bool:
    # The switch should be on when the binary sensor is on or when INT_0 == 42, off otherwise
    return ((BINARY_SENSOR.is_on() or app_state.INT_0 == 42) and SWITCH.is_on()) or (not BINARY_SENSOR.is_on() and not SWITCH.is_on())


def iteration():
    if BINARY_SENSOR.is_on() or app_state.INT_0 == 42:
        SWITCH.on()
    else:
        SWITCH.off()
```

## Running the applications

To run all the installed apps (with [runtime verification](#runtime) enabled):

1. In [ETS](https://www.knx.org/knx-en/for-professionals/software/ets-professional/), import the file `assignments/assignment.csv` to create the group addresses, then assign to each communication object the right group address as presented in `assignments/assignment.txt`. The name of the group address should help understanding which device it is meant to.
2. In ETS, do a basic configuration of the devices to make them have the correct basic behaviour (the amount of configuration depends on the particular device)
3. Execute `svshi run -a address:port`, where address is the KNX IP gateway address and port is the KNX IP gateway port.

SVSHI logs which apps have been called during execution and which telegrams have been received. You can find the logs in `logs/`.

## Updating an application

If you want to update the code of an app that is already installed, SVSHI offers a simplified process. To be updated, an app must have the **same** prototypical structure as its previous version and be installed.
  
To update the app, you then:

1. Put the new version of the app in the `generated` folder
   > There must be NO other app in the folder
2. Run `svshi updateApp -n app_name` where `app_name` is the app you want to update
  
Using this process, you do not need to generate or modify the bindings and you do not need to pass an ETS project.

If the new version does not pass the verification stage, the update is aborted and the old set of apps is restored as before the operation.

## App generator

### Prototypical structure

This JSON file is given by the programmer/developer that wants to develop an application. It represents the prototypical devices that the app needs with their types. It also specifies whether the app is _privileged_ or not (`"permissionLevel": "privileged" | "notPrivileged"`). A privileged app overrides the behavior of the non-privileged ones at runtime.

Moreover, the `timer` attribute can be used to run the application even though the physical state has not changed. The app thus becomes a **periodic** app.

- If `timer == 0` the application runs only when the physical state of the devices it uses changes
- If `timer > 0` the application runs when the physical state changes AND every `timer` seconds.

Once the app is generated, it is moved in the generated apps' folder.

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
  task <command>           The task to run. Can be passed as is. Possible options are 'run', 'compile', 'generateBindings', 'generateApp', 'removeApp', 'listApps' and 'version'. This argument is not case sensitive.
  -f --ets-file <str>      The ETS project file to use for the tasks 'compile' and 'generateBindings'
  -d --devices-json <str>  The devices prototypical structure JSON file to use for the task 'generateApp'
  -n --app-name <str>      The app name to use for the tasks 'generateApp' and 'removeApp'
  -a --address <str>       The KNX address to use for the task 'run'. The correct format is 'address:port' (ex: 192.168.1.1:5555)
  --no-colors              The flag to disable output coloring
  --all                    The flag to remove all apps for the task 'removeApp'
```

Available commands are:

- `svshi run -a 1.1.1.1:5555` to run all the apps with [runtime verification](#runtime)
- `svshi compile -f ets.knxproj` to compile all the apps
- `svshi generateBindings -f ets.knxproj` to generate the bindings for all the apps
- `svshi generateApp -d devices.json -n app_name` to generate a new Python app
- `svshi removeApp -n app_name` to remove an installed app
- `svshi updateApp -n app_name` to update the code of an app that is already installed
- `svshi listApps` to list all the installed apps
- `svshi version` to display the CLI version

Shorter aliases are also available:

- `svshi r` for `svshi run`
- `svshi c` for `svshi compile`
- `svshi gb` for `svshi generateBindings`
- `svshi ga` for `svshi generateApp`
- `svshi ra` for `svshi removeApp`
- `svshi ua` for `svshi updateApp`
- `svshi la` for `svshi listApps`
- `svshi v` for `svshi version`

## Simulator

To facilitate development and demo, we developed a KNX simulator that can be used with or without SVSHI.

To use it with SVSHI, to avoid having to create an ETS project, it is possible to input a `.json` file when running `compile` and `generatedBindings`. This JSON file that represents the physical system can be easily generated using our [GUI](#gui).

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

Whenever an app wants to update the KNX system, SVSHI verifies whether the update could break the apps' invariants. If it is the case, the apps are prevented from running, otherwise the updates are propagated to KNX.

### Execution

At compile time, all applications are combined to create a function representing the system's behaviour. This function combines all applications' `iteration()` functions and the order is thus set at compile time. Applications that are `privileged` overrides behaviour of `nonPrivileged` ones if they are conflicting.

SVSHI's runtime is **reactive** and **event-based**. The system _listen_ for changes to the group addresses of the devices it uses, and is run on a state change (an _event_). The state transition can be triggered externally by the KNX system or by its own behaviour, in which case the system proceeds to notify the listener again. Notable exception are apps that run every X seconds based on a timer, which not only react to state changes but are also executed periodically.

For a given set of applications, the system behaviour function is run every time the state of the devices changes and every Y seconds where Y is the **minimum of all timers of all applications which are greater than 0**.

_Running applications_ concretely means that the system behaviour function is executed on the current physical state of the system and with the current app states.

This execution model has been chosen for its ease of use: users do not need to write `while` loops or deal with synchronization explicitly.

## GUI

To provide a GUI in the form of a web application, SVSHI offers a JSON API through HTTP. To start the server, run `svshi gui`. It starts a server that serves requests at `http://localhost:4242`.

### JSON API

Except otherwise specified, requests reply with a JSON body with the following structure:

```json
{
  "status": true,
  "output": 
    [
      "first line of the output text",
      "second line of the output text"
    ]
}
```

where the `status` represents whether the request succeeded and `output` contains the output specific to each request.

Here are the available endpoints:

- GET `/version`

  Corresponds to `svshi version` of the CLI.

  Returns the current version of `svshi` in the first line of `output`.

- GET `/listApps`

  Corresponds to `svshi listApps` of the CLI.

  Replies with one app name per element in `output`

- GET `/availableProtoDevices`

  Does not correspond to any CLI function.

  Replies with one device type per element in `output`. These are the [Supported devices](#supported-devices).

- GET `/availableDpts`

  Does not correspond to any CLI function.

  Replies with one dpt per element in `output`. The format is "DPT-X" where X is the int of an available dpt.

- POST `/generateApp/:appName`

  Corresponds to the `svshi generateApp` of the CLI.

  The name of the new app is passed in the endpoint path. The body of the request must contain the prototypical structure as a JSON (same structure as created manually for the CLI).

- POST `/compile`

  Corresponds to the `svshi compile` of the CLI.

  Runs the `compile` operation of `svshi`, replying with the `status` representating whether the compilation was successful or not and the `output` contains the messages returned by `svshi`.

  The body must be a `.zip` archive containing the `.knxproj` file used to compile.

- POST `/updateApp/:appName`

  Corresponds to the `svshi updateApp -n appName` of the CLI.

  Runs the `updateApp -n appName` operation of `svshi`, replying with the `status` representating whether the update was successful or not and the `output` contains the messages returned by `svshi`.

  The body must be a `.zip` archive containing the `.knxproj` file or the `.json` file used to compile.

- POST `/generateBindings`

  Corresponds to the `svshi generateBindings` of the CLI.

  Runs the `generateBindings` operation of `svshi`, replying with the `status` representating whether the generation was successful or not and the `output` contains the messages returned by `svshi`.

  The body must be a `.zip` archive containing the `.knxproj` file or the `.json` file used to generate the bindings.

- POST `/removeApp/:appName`

  Corresponds to the `svshi removeApp -n appName` of the CLI.

  Runs the `removeApp -n appName` operation of `svshi`, replying with the `status` representating whether the removal was successful or not and the `output` contains the messages returned by `svshi`.

- POST `/removeAllApps`

  Corresponds to the `svshi removeApp --all` of the CLI.

  Runs the `removeApp --all` operation of `svshi`, replying with the `status` representating whether the removal was successful or not and the `output` contains the messages returned by `svshi`.

- POST `/run/:ipPort`

  Corresponds to the `svshi run -a ip:port` of the CLI.

  Launches `svshi` run replying with the `status = true` and the `output` containing a fixed message that indicates that the server started. 

  It stores all the outputs of `svshi` running in a file, to be retrieved by `/runStatus`.

- GET `/runStatus`

  Replies with the standard body JSON with `status` representing whether `svshi` is running or not, and empty `output`.

- GET `/logs/run`

 If the log file exists (i.e., SVSHI was run or is running), it replies with the standard body JSON with `status=true` and `output` containing the output lines of `svshi` running, truncated if the log file goes beyond a given size (e.g., 20MB).

- GET `/logs/receivedTelegrams`

 If the log file exists (i.e., SVSHI was run or is running), it replies with the standard body JSON with `status=true` and `output` containing the received telegrams log file of the latest execution, truncated if the log file goes beyond a given size (e.g., 20MB).

- GET `/logs/execution`

 If the log file exists (i.e., SVSHI was run or is running), it replies with the standard body JSON with `status=true` and `output` containing the complete execution log file of the latest execution, truncated if the log file goes beyond a given size (e.g., 20MB).

- GET `/logs/physicalState`

 If the physical state log file exists (i.e., SVSHI runtime part wrote it), it replies with a JSON Body containing the physical state (i.e., mapping from Group Addresses to their current value in the system). Reply with a 404 if the file does not exist.

- POST `/stopRun`

  Stops the instance of `svshi` running if it was indeed running, does nothing otherwise. Replies with the standard body JSON with `status=true` and `output` containing a standard message.

- GET `/newApps`
  
  Replies with the standard body JSON with `status=true` and `output` containing the names of the apps currently in the `generated` folder (i.e., to be installed if calling `compile`).

- GET `/bindings`

  Replies with the bindings and physical structure currently in the `generated` folder.

  This endpoint replies with a different body. It is a JSON body containing the physical structure and the generated bindings in the following form:

  ```json
    {
      "physicalStructure": {...},
      "bindings": {...}
    }
  ```

  If no bindings or physical structure json are available in generated folder, error message is sent with a 404 error code.

- POST `/generated`

  Allows to add/overwrite files in the `generated` folder. The body must be a `.zip` archive containing files and folders that must go into `generated`. If a file already exists in `generated`, it is overwritten. If a folder already exists in `generated` it is merged (i.e., files with same names are overwritten, files that exist only in one of the two are kept).

  Replies with the standard body JSON with `status` representing whether the copy worked and `output` containing errors or the names of folders and files that were added in case of success.

- GET `/generated`

  Replies with the `generated` folder in a `.zip` archive as body.

- GET `/generated/:filename`

  Replies with the requested `filename` folder or file in a `.zip` archive as body.

  If the file or directory does not exist, it replies with an empty `.zip`.

- POST `/deleteAllGenerated`

  Deletes the content of the `generated` folder.

  Replies with the standard body JSON with `status` representing whether the removal worked and `output` containing errors or a standard message in case of success.

- POST `/deleteGenerated/:filename`

  Deletes the file or directory with the name `filename` in the `generated` folder.

  Replies with the standard body JSON with `status` representing whether the removal worked and `output` containing errors or a standard message in case of success.

  If the file or directory does not exist, it replies with a success message.

- GET `/installedApp/:appName`

  Replies with the requested app folder in a `.zip` archive as body if the requested app name corresponds to an installed app, 404 error otherwise.

- GET `/allInstalledApps`

  Replies with the requested installedApps folder in a `.zip` archive as body if at least one app is installed, 404 error otherwise.
  The folder contains all installed apps and the bindings and physical structure. It can be uploaded to generated and be compiled as is.

- GET `/assignments`

  Replies with a `.zip` archive as body containing the `assignments` folder if assignments are created, 404 error if no assignments are created.

The POST endpoints functions cannot be ran in parallel. These endpoints then acquire a "lock" and reply with a 423 error code if the lock is already aquired by another endpoint currently running.

## KNX Virtual

You do not necessarily need to build a KNX infrastructure to test your app. You can use _KNX Virtual_ to run SVSHI. This a **Windows**-only program that is available on the KNX Association's website[^1]. Once you downloaded and installed it, you can use it exactly as a physical installation with SVSHI. Use the startup settings of KNX Virtual (address and port) when executing `svshi run`. Default values are `127.0.0.1:3671`.

Please be careful and use the right devices inside ETS when configuring the project and refer to the KNX Help for KNX Virtual related issues[^2].

[^1]: https://www.knx.org/knx-en/for-professionals/get-started/knx-virtual/
[^2]: https://support.knx.org/hc/en-us/sections/360000827779-KNX-Virtual

## Contributing

See [the contributing guide](/CONTRIBUTING.md) for detailed instructions on how to get started with the project.

## White paper

See [the white paper](/src/documentation/documentation.md) for an in-depth understanding about why SVSHI exists, what it does and how it does it.

## License

SVSHI is licensed under the [MIT license](/LICENSE).

Logo: Copyright @norafisler
