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

## Tests

To run the tests of all the modules at once, execute `./run_tests.sh` inside `svshi/`.

## CI

We use [GitHub Actions](https://github.com/dslab-epfl/smartinfra/actions) to run the whole test suite on every push and pull request. The workflows are defined in `.github/workflows/ci.yml`.

## Releases

To build a new release:

1. Run `./build_release.sh` inside `svshi/` to build the archive.
2. Create a new release on [GitHub](https://github.com/dslab-epfl/smartinfra/releases) and add the created `tar.gz` archive as an attachment. Make sure the release version and the svshi version are the same.
