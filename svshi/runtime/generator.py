import os
import subprocess


class ConditionsGenerator:
    def __init__(
        self,
        app_library_dir: str,
        conditions_file_path: str,
        verification_file_path: str,
        runtime_file_path: str,
    ):
        self.__app_library_dir = app_library_dir
        self.__conditions_file_path = conditions_file_path
        self.__verification_file_path = verification_file_path
        self.__runtime_file_path = runtime_file_path

    def __write_conditions_file(self, data: str):
        os.makedirs(os.path.dirname(self.__conditions_file_path), exist_ok=True)
        with open(self.__conditions_file_path, "w+") as output_file:
            output_file.write(data)

    def copy_verification_file_from_verification_module(
        self, verification_module_path: str
    ):
        """
        Copies the verification file from the verification module in the runtime module.
        """
        subprocess.run(
            f"cp {verification_module_path}/verification_file.py {self.__verification_file_path}",
            shell=True,
        )

    def copy_runtime_file_from_verification_module(self, verification_module_path: str):
        """
        Copies the runtime file from the verification module in the runtime module.
        """
        subprocess.run(
            f"cp {verification_module_path}/runtime_file.py {self.__verification_file_path}",
            shell=True,
        )

    def generate_conditions_file(self):
        """
        Generates the conditions file given the conditions of all the apps installed in the library.
        """
        apps_dirs = [
            f.name
            for f in os.scandir(self.__app_library_dir)
            if f.is_dir() and f.name != "__pycache__"
        ]
        imports = "from verification_file import "
        imports_code = []
        nb_apps = len(apps_dirs)
        for i, app in enumerate(sorted(apps_dirs)):
            # We also need to import the PhysicalState at the end
            suffix = ", " if i < nb_apps - 1 else ", PhysicalState\n"
            precond_function = f"{app}_precond"
            imports += f"{precond_function}{suffix}"
            imports_code.append(precond_function)

        check_conditions_body = ""
        nb_imports = len(imports_code)
        for i, import_code in enumerate(imports_code):
            suffix = " and " if i < nb_imports - 1 else ""
            check_conditions_body += f"{import_code}(state){suffix}"

        file = f"""
{imports}
def check_conditions(state: PhysicalState) -> bool:
    return {check_conditions_body}
""".strip()

        self.__write_conditions_file(file)

    def reset_conditions_file(self):
        """
        Resets the conditions file.
        """
        file = f"""
# Default file, will be overwritten while running
        
from .verification_file import PhysicalState

def check_conditions(state: PhysicalState) -> bool:
    return True    
""".strip()

        self.__write_conditions_file(file)

    def reset_verification_file(self):
        """
        Resets the verification file.
        """
        file = f"""
# Default file, will be overwritten while running
import dataclasses


@dataclasses.dataclass
class PhysicalState:
    GA_1_1_1: float
    GA_1_1_2: float
""".strip()

        with open(self.__verification_file_path, "w") as output_file:
            output_file.write(file)

    def reset_runtime_file(self):
        """
        Resets the runtime file.
        """
        file = f"""
# Default file, will be overwritten while running
import dataclasses


@dataclasses.dataclass
class PhysicalState:
    GA_1_1_1: float
    GA_1_1_2: float
""".strip()

        with open(self.__runtime_file_path, "w") as output_file:
            output_file.write(file)
