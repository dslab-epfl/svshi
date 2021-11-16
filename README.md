# Formal Verification for Smart Infrastructure

This project is about developing a platform/runtime/toolchain for developing and running formally verified smart infrastructures, such as smart buildings, smart cities, etc.

## App generator

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

To execute it, run `python -m generator.main devices.json app`, where:

- The first argument (`devices.json` in the example) is the name of the JSON file containing the devices description.
- The second argument (`app` in the example) is the name of the app to be used. The name has to follow the same rules as for Python modules: short, all-lowercase names. Underscores can be used if it improves readability.
