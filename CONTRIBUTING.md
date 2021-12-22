# Contributing

- [Contributing](#contributing)
  - [Requirements](#requirements)
  - [Setup](#setup)
  - [Python](#python)
  - [Scala](#scala)
  - [Project structure](#project-structure)
  - [Tests](#tests)
  - [CI](#ci)
  - [Releases](#releases)

## Requirements

- Unix-based OS (Linux or macOS).
- [Python](https://www.python.org) 3.8 or newer.
- [Java](https://www.oracle.com/java/) 11 or newer.
- [Scala](https://www.scala-lang.org) 2.13.x or newer. Scala 3 is not yet supported.
- [sbt](https://www.scala-sbt.org) 1.5.5 or newer.

## Setup

Install SVSHI from sources as explained in the [README](/README.md), then install all the Python dependencies with `pip install -r requirements.txt` inside `svshi/`.

## Python

For formatting and code style, we use [black](https://github.com/psf/black). For tests, we use [pytest](https://github.com/pytest-dev/pytest/).

All code should be as typed as possible (the [typing](https://docs.python.org/3/library/typing.html) module is your friend).

## Scala

For formatting and code style, we use [scalafmt](https://github.com/scalameta/scalafmt). For tests, we use [scalatest](https://github.com/scalatest/scalatest).

## Project structure

```
┌── assignments - Contains group address assignments for KNX communication objects
├── generated - Contains generated but not yet installed apps, their bindings and the latest physical structure
├── svshi - Contains all Python and Scala source code
    ├── app_library - Contains installed apps, their bindings and the physical structure at the latest installation time
    ├── core - ETS project parser, bindings generator and apps compiler/verifier (Scala sbt project)
    ├── generator - Python app generator (Python module)
    ├── runtime - Runs the installed apps and keeps the system in sync with KNX (Python module)
    ├── verification - Manipulates the apps' source code to generate a file useful for the verification and for the runtime (Python module)
    └── ...
└── ...
```

## Core

This module contains mainly the `compiler`, the `verifier` and the KNX `programmer`. Along with these two major components live some parsers for `.json` and `.knxproj` (i.e., ETS project file) files and some utility functions that are used by them. This also includes everything related to the CLI.

### Parsers
The `compiler` and the `verifier` need parsers for some `JSON` files and ETS projects. These parsers are:
- ETS Project: this parser parses a file of type `.knxproj` which is the file used by ETS. It outputs an instance of `PhysicalStructure` that represents the physical KNX installation in which SVSHI is running.
- `JSON` files: these parsers read (and for some write) `JSON` files containing the following structures:
  - `Bindings`: this one is used to store and retrieve bindings between prototypical devices and physical communication objects
  - `PythonAddress`: this one produces the `address.json` file that is used by the runtime part to map prototypical devices to the corresponding physical address(es).
  - `PhysicalStructure`: this one is used to store and retrieve the physical structure output by the `ETS` parser as `JSON` to be kept along with the applications.
  - `PrototypicalStructure`: this one is used to read the prototypical structure that is also used by the application generator.

### Compiler

At a high level, the `compiler` takes the existing library of applications (i.e., applications that are already installed and run on the SVSHI installation) and new application(s) (i.e., application(s) that are currently being installed) and produces information for KNX programming (i.e., programming of the physical devices) as well as code for the runtime component and the `verifier`.

The `compiler` has two main tasks, that are called through the CLI.

#### GenerateBindings
During the execution of the `generateBindings` task, the `compiler` reads all applications structures and produces a `bindings` file (named `apps_bindings.json`) that the user has to fill. This `binding` file contains all applications and for each of them, the list of prototypical devices and their I/O channels. Each device type has its own sets of I/O channels. The user then binds each one of these channels to a physical communication object in the `PhysicalStructure` (named `physical_structure.json`) through `ids` in the `JSON` files.

If the `PhysicalStructure` did not change (i.e., no difference between the one produced when parsing the `.knxproj` file and the one stored in the library from the previous application(s) installation process), the `compiler` puts back the old bindings in the file, so that the user does not need to redo all the bindings but only the ones for the application(s) being installed.

#### Compile
During the execution of the `compile` task, the `compiler` produces a `GroupAddressAssignment`. The idea here is to take all the bindings between prototypical devices I/O channels and physical devices communications objects and assign them group addresses. A group address must be unique to one "connection" to ensure that no interference will occur on the bus at runtime (i.e., sending a command to a device will trigger another because they are listening to the same group address).

One new group address (assigned incrementally) is assigned to each physical id (i.e., one per physical device's communication object).

Once this assignment is constructed, the `compiler`:
- produces one `JSON` file (`PythonAddress`) for each application that will then be used by the runtime module and the `verifier` and stores it in the application's folder.
- passes this assignment to the KNX `programer` component.

### Verifier

The `verifier` does various kind of verification on what the `compiler` produced and on the applications themselves. Let us detail the different stages and what they verify.

Except when it is explicity said that the verifier outputs a warning, everytime something is invalid the `verifier` produces an error. More than 0 errors is considered as a fail of the verification and requires adjusting the applications and/or bindings to install the application.

#### bindings.Verifier

This part of the `verifier` verifies properties of the bindings between prototypical devices and physical ones.

##### IOTypes

The first property verified is that IO Types correctly matched. IO Type can be `in`, `out` or `in/out`. `in` means that the prototypical (physical repspectively) device channel (communication object repspectively) can receive value from the bus and react accordingly. `out` means the opposite, i.e., that the device channel (communication object respectively) can write value to the bus. Lastly `in/out` means that the channel (communication object respectively) can do both simulatenously.

Every prototypical device defined as a `SupportedDevice` provides the IO type for each of these channels (through the class `SupportedDeviceBinding`). The IO type of physical device's communication objects are read by the parser in the ETS project file.
As the IO type is not always provided for physical devices, it can be `Unknown`.

Compatibility is defined as follows:

<style type="text/css">
.tg  {border-collapse:collapse;border-spacing:0;}
.tg td{border-color:black;border-style:solid;border-width:1px;font-family:Arial, sans-serif;font-size:14px;
  overflow:hidden;padding:10px 5px;word-break:normal;}
.tg th{border-color:black;border-style:solid;border-width:1px;font-family:Arial, sans-serif;font-size:14px;
  font-weight:bold;overflow:hidden;padding:10px 5px;word-break:normal;}
.tg .tg-d52n{background-color:#32cb00;border-color:inherit;text-align:left;vertical-align:top}
.tg .tg-0pky{border-color:inherit;text-align:left;vertical-align:top}
.tg .tg-g191{background-color:#ff7474;border-color:inherit;text-align:left;vertical-align:top}
.tg .tg-lwlt{background-color:#ff7474;border-color:inherit;color:#333333;text-align:left;vertical-align:top}
.tg .tg-x6qq{background-color:#dae8fc;border-color:inherit;text-align:left;vertical-align:top}
</style>
<table class="tg">
<thead>
  <tr>
    <th class="tg-0pky">Physical\Prototypical</th>
    <th class="tg-0pky">In</th>
    <th class="tg-0pky">Out</th>
    <th class="tg-0pky">In/Out</th>
  </tr>
</thead>
<tbody>
  <tr>
    <th class="tg-0pky">In</th>
    <td class="tg-d52n">Yes</td>
    <td class="tg-g191">No</td>
    <td class="tg-lwlt">No</td>
  </tr>
  <tr>
    <th class="tg-0pky">Out</th>
    <td class="tg-d52n">Yes</td>
    <td class="tg-d52n">Yes</td>
    <td class="tg-d52n">Yes</td>
  </tr>
  <tr>
    <th class="tg-0pky">In/Out</th>
    <td class="tg-d52n">Yes</td>
    <td class="tg-d52n">Yes</td>
    <td class="tg-d52n">Yes</td>
  </tr>
  <tr>
    <th class="tg-0pky">Unknown</th>
    <td class="tg-x6qq">Yes but warning</td>
    <td class="tg-x6qq">Yes but warning</td>
    <td class="tg-x6qq">Yes but warning</td>
  </tr>
</tbody>
</table>


As we abstract the Physical state and run applications on it, it means that a prototypical device in an application can read a state that is only an `out` in the physical world (because its value is stored in the mirrored state kept by SVSHI). This is why `in` prototypical <-> `out` physical is permitted.

With `Unknown` type for physical devices, we cannot do more than give a warning to the developer to really make sure that the connection is valid.

##### Python types 

As we abstract the state of the physical installation in a mirrored state, we assign Python type (e.g., `int`, `float`, `bool`, ...) to each group address. For this to be valid, all prototypical device channels linked to that group address must use values of the same type.

Each device, just as for IO Types, has the Python type of the value it would read/write on the KNX bus encoded in the corresponding `SupportedDeviceBinding` class.

This stage then check that all channels connected to the same group address uses the same Python type for their values.

##### KNX datatypes
This stage does the same kind of verification as the IO Types one but checking that the KNX datatypes of the value that the device would read/write on the bus are compatible.

For each binding between a physical device's communication object and a prototypical device channel, we check that the KNX datatype is the same. The KNX datatype of the physical device's communication object is parsed from the ETS project and the one for the prototypical device channel is encoded in the corresponding `SupportedDeviceBinding`.

As for the IO Types, if the KNX Datatype is not known for a physical communication object, the `verifier` gives a warning to the developer. Otherwise, KNX Datatypes must be **equal**.

##### Mututal KNX datatypes

This stage performs the KNX Datatype check just as the previous ones but between prototypical device channels that are linked to the same physical device's communication object.

#### python.static.Verifier

This stage verifies that Python applications preserve the invariants.

In each Python application, the developer can fill 2 functions: `iteration()` and `invariant()`. Both of these functions can access the devices.
- `invariant` returns a `bool` and represents an invariant that the KNX system's state should always satisfy
- `iteration` is called everytime the state of one of the devices the applications uses changes. It represents the state modification function.

The static verification done at this stage verifies using [CrossHair](https://github.com/pschanely/CrossHair) that, given a valid state of the KNX installation (i.e., that satisfies the invariants of ALL applications installed or being installed), it cannot return a state that violates one of these invariants.

To do so, this stage calls the `verification` module written in Python that transforms the code and prepare two versions of it: one used for the verification and one used later by the runtime module.

The code modification consists, in a nutshell, to:
- add a `PhysicalState` instance as argument of `iteration` and `invariant` functions
- add an instance of the corresponding type as argument of `iteration` function for each `unchecked` function
- add a CrossHair contract to `iteration` containing:
  - one precondition for all `invariant` function of installed or being installed applications
  - one precondition for all postconditions of the `unchecked` functions
  - one postcondition for all `invariant` function of installed or being installed applications on `__return__` value
- move all used functions in one file
- add to that file all the generated code (`PhysicalState`, devices' classes, ...)

The `verifier` then calls CrossHair on that file, and retrieve `std.out`. Each counter example found by CrossHair is considered as an error and is returned by the `verifier` to be displayed.

#### What to change to add new prototypical devices?

The types of device that SVSHI supports require specific code to be handled properly. To add a new device type, you need to do the following:

- add a new `case object` that extends `ch.epfl.core.model.prototypical.SupportedDevice` (following comments and already present examples)
- add a new corresponding `case class` that extends `ch.epfl.core.model.prototypical.SupportedDeviceBinding` (following comments and already present examples)
> Once you added the new `SupportedDeviceBinding`, compiler warnings can guide you because the trait is sealed and thus match can be exhaustive.
- update the function `assignmentToPythonAddressJson` in `ch.epfl.core.parser.json.bindings.PythonAddressJsonParser` (specially the `match`)
- update the 3 following functions in `ch.epfl.core.verifier.bindings.Verifier`: `verifyBindingsMutualDPT`, `verifyBindingsIoTypes` and `verifyBindingsKNXDatatypes` (also the `match`es)

## Tests

To run the tests of all the modules at once, execute `./run_tests.sh` inside `svshi/`.

## CI

We use [GitHub Actions](https://github.com/dslab-epfl/smartinfra/actions) to run the whole test suite on every push and pull request. The workflows are defined in `.github/workflows/ci.yml`.

## Releases

To build a new release:

1. Run `./build_release.sh` inside `svshi/` to build the archive.
2. Create a new release on [GitHub](https://github.com/dslab-epfl/smartinfra/releases) and add the created `tar.gz` archive as an attachment. Make sure the release version and the svshi version are the same.
