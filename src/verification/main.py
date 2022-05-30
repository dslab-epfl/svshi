import os
from typing import Final

from .generator import Generator
from .parser import Parser

SVSHI_HOME: Final = os.environ["SVSHI_HOME"]
SVSHI_SRC_FOLDER: Final = f"{SVSHI_HOME}/src"
GENERATED_PATH = f"{SVSHI_HOME}/generated"
APP_LIBRARY: Final = f"{SVSHI_SRC_FOLDER}/app_library"
VERIFICATION_MODULE_PATH: Final = f"{SVSHI_SRC_FOLDER}/verification"
FILES_FOLDER_PATH: Final = f"{SVSHI_SRC_FOLDER}/runtime/files"


def main(
    generated_path: str,
    app_library_path: str,
    verification_module_path: str,
    files_folder_path: str,
):
    parser = Parser(generated_path, app_library_path)
    group_addresses_with_types = parser.parse_group_addresses()
    devices_instances = parser.parse_devices_instances()
    devices_classes = parser.parse_devices_classes()
    app_names = parser.get_app_names()
    filenames = parser.get_filenames()

    verification_filename = f"{verification_module_path}/verification_file.py"
    runtime_filename = f"{verification_module_path}/runtime_file.py"
    conditions_filename = f"{verification_module_path}/conditions.py"
    generator = Generator(
        verification_filename,
        runtime_filename,
        conditions_filename,
        files_folder_path,
        group_addresses_with_types,
        devices_instances,
        devices_classes,
        app_names,
        filenames,
    )
    app_priorities = parser.get_app_priorities()
    generator.generate_verification_file(app_priorities=app_priorities)
    generator.generate_runtime_file(app_priorities=app_priorities)
    generator.generate_conditions_file()
    print(verification_filename)


if __name__ == "__main__":
    main(GENERATED_PATH, APP_LIBRARY, VERIFICATION_MODULE_PATH, FILES_FOLDER_PATH)
