# SVSHI - Secure and Verified Smart Home Infrastructure

<p align="center">
  <img src="src/res/logo.png" alt="logo" width="50%"/>
</p>

![CI](https://github.com/dslab-epfl/svshi/actions/workflows/ci.yml/badge.svg)
[![codecov](https://codecov.io/gh/dslab-epfl/svshi/branch/main/graph/badge.svg?token=NYRGL343U2)](https://codecov.io/gh/dslab-epfl/svshi)
![Latest Stable Version](https://img.shields.io/github/v/release/dslab/svshi?label=version)
![License](https://img.shields.io/github/license/dslab/svshi)

- [SVSHI - Secure and Verified Smart Home Infrastructure](#svshi---secure-and-verified-smart-home-infrastructure)
  - [Installation](#installation)
    - [From archive](#from-archive)
      - [Unix (Linux / macOS)](#unix-linux--macos)
      - [Windows](#windows)
    - [From sources](#from-sources)
      - [Unix (Linux / macOS)](#unix-linux--macos-1)
      - [Windows](#windows-1)
    - [Docker](#docker)
  - [Supported devices](#supported-devices)
  - [Developing an application](#developing-an-application)
    - [Writing apps](#writing-apps)
    - [App example](#app-example)
  - [Running the applications](#running-the-applications)
  - [Updating an application](#updating-an-application)
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
  - [KNX Virtual](#knx-virtual)
  - [Contributing](#contributing)
  - [White paper](#white-paper)
  - [License](#license)

The **SVSHI** (**S**ecure and **V**erified **S**mart **H**ome **I**nfrastructure) (pronounced like "sushi") project is a platform/runtime/toolchain for developing and running formally verified smart infrastructures, such as smart buildings, smart cities, etc.

It provides a [CLI](#cli), `svshi`, to interact easily with the platform.

With SVSHI, a user can develop and run Python applications interacting with [KNX](https://www.knx.org/knx-en/for-professionals/index.php) systems that are [formally verified](#verification) at both compile and run-time against a set of provided invariants.

## Installation

To work, SVSHI needs Python 3.9 (or newer)([download here](https://www.python.org/downloads/)) and Java 11 (or newer)([download here](https://www.oracle.com/java/technologies/downloads/)). Optionally, [sbt](https://www.scala-sbt.org) 1.5.5 (or newer) is needed to build from sources.

To check if the installation was successful, run `svshi version` in your terminal.

### From archive

#### Unix (Linux / macOS)

To install SVSHI on Linux or macOS:

1. Download the latest version `.zip` file from the [Releases](https://github.com/dslab-epfl/svshi/releases) page
2. Unzip it, move the folder to the desired installation site, then run `./install.sh` inside it.
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
2. Run `./build.sh`
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

1. Run `./build_docker.sh` to build the image
2. Run `./run_docker.sh` to run the docker container. It opens a `sh` instance in the container with the current directory mapped to `/pwd` in the container
3. You can find the repo copied in `/home/maki/svshi`
4. `svshi` command is accessible

## Supported devices

- **Binary sensors** (deviceType = "binary")
- **Temperature sensors** (deviceType = "temperature")
- **Humidity sensors** (deviceType = "humidity")
- **CO2 sensors** (deviceType = "co2")
- **Switches** (deviceType = "switch")

## Developing an application

To develop an app for SVSHI:

1. Create the devices prototypical structure file containing the list of the devices the app should use, as explained in the [app prototypical structure](#prototypical-structure) section.
2. Run the app generator, as explained in the [app generator](#app-generator) section, to get the app skeleton. It will be created under the `generated/` folder.
3. [Write your app](#writing-apps).
4. Run `svshi` to generate the bindings with `svshi generateBindings -f ets.knxproj`, where the argument is the _absolute_ path to the ETS project file.
5. Map the right physical ids given in `generated/physical_structure.json` to the right device in `generated/apps_bindings.json`. This is needed to provide the devices in the Python code with the group addresses to use. The first file represents the physical structure from the ETS project file, where each communication object has an id. The second one represents the apps structure with the devices and for each of them, the links they need.
6. Run `svshi` again to [compile](#compilation) and [verify](#verification) the app with `svshi compile -f ets.knxproj`.

### Writing apps

To write an app, you mainly have to modify the `main.py` file, optionally adding dependencies into the `requirements.txt` file provided in the generated project. To understand how the project is generated, please refer to the [app generation section](#app-generator).

All the available device instances are already imported in `main.py`. They mirror what has been defined in the device prototypical structure file.

The application can use external files. They however need to have been declared in the prototypical structure `json` file and they have to be located at the root of the project, next to `main.py`.

There are two important functions in `main.py`, `invariant()` and `iteration()`. In the first one the user should define all the conditions (or _invariants_) that the entire KNX system must satisfy throughout execution of **all** applications, while in the second she should write the app code.

An important thing to be aware of is that `iteration()` cannot use external libraries directly. Instead, these calls have to be defined first inside _unchecked functions_, which are functions whose name starts with `unchecked` and whose return type is explicitly stated. Then, these functions can be used in `iteration()`.

In addition, note that `invariant()` must return a boolean value, so any kind of boolean expression containing the _read_ properties of the devices and constants is fine. However, here operations with side effects, external libraries calls and unchecked functions calls are **not** allowed.

**Unchecked functions** are used as a compromise between usability and formal verification, and as such must be used as little as possible: their content is not verified by SVSHI. Furthermore, they should be short and simple: we encourage developers to add one different unchecked function for each call to an external library. All logic that does not involve calls to the library should be done in `iteration()` to maximize code that is indeed formally verified.
Nonetheless, the user can help the verification deal with their presence by annotating their docstring with _post-conditions_.

Functions' **post-conditions** define a set of _axioms_ on the return value of the function: these conditions are assumed to be always true by SVSHI during verification. They are defined like this: `post: __return__ > 0`. You can use constants and other operations. You can add as much post-conditions as you like and need. Therefore, we encourage developers to avoid having conjunctions in post-conditions but rather to have multiple post-conditions. This does not make difference for the verification but helps the readability.  
However, keep in mind that these conditions are **assumed to be true** during formal verification! If these do not necessarily hold with respect to the external call, bad results can occur at runtime even though the code verification was successful!

An example with multiple post-conditions could be:

```python
def unchecked_function() -> int:
  """
  post: __return__ > 0
  post: __return__ != 3
  """
  return external_library_get_int()
```

Furthermore, applications have access to a set of variables (the _app state_) they can use to keep track of state between calls. Indeed, the `iteration()` function is called in an [event-based manner](#execution) (either the KNX devices' state changes or a periodic app's timer is finished). All calls to `iteration()` are independent and thus SVSHI offers a way to store some state that will live in between calls. There is a local state instance _per app_.

To do so, in `main.py` the `app_state` instance is imported along with the devices. This is a [dataclass](https://docs.python.org/3/library/dataclasses.html) and it contains 4 fields of each of the following types: `int`, `float`, `bool` and `str`. The fields are called respectively `INT_X`, `FLOAT_X`, `BOOL_X` and `STR_X` where X equals 0, 1, 2 or 3.

These values can be used in `iteration()` and `invariant()`. One should be careful while using it in `invariant()` or in a condition that will affect the KNX installation's state (the _physical_ state): the formal verification would fail if ANY possible value of the `app_state` leads to an invalid state after running `iteration()` even if this case should not occur because of previous calls to `iteration()` that would have set the values.

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

The `files` attributes is used to indicate files that the app needs to work properly. These files must be at the root of the application project (next to `main.py`).

Once the app is generated, it is moved in the generated apps' folder.

Here is an example:

```json
{
  "permissionLevel": "notPrivileged",
  "timer": 60,
  "files": ["file1.txt", "file2.png"],
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

SVSHI's runtime is **reactive** and **event-based**. Applications _listen_ for changes to the group addresses of the devices they use, and are run on a state change (an _event_). The state transition can be triggered externally by the KNX system or by another app, which then proceeds to notify all the other listeners. Notable exception are apps that run every X seconds based on a timer, which not only react to state changes but are also executed periodically.

_Running an application_ concretely means that its `iteration()` function is executed on the current physical state of the system and on the current app state.

Apps are always run in alphabetical order in their group (`privileged` or `notPrivileged`). The non-privileged apps run first, then the privileged ones: in such a way privileged applications can override the behavior of non-privileged ones.

This execution model has been chosen for its ease of use: users do not need to write `while` loops or deal with synchronization explicitly.

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
