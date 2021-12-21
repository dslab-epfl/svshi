import os


class ConditionsGenerator:

    __DEFAULT_RUNTIME_AND_VERIFICATION_FILE = f"""
# Default file, will be overwritten while running
import dataclasses


@dataclasses.dataclass
class PhysicalState:
    GA_1_1_1: float
    GA_1_1_2: float
    GA_1_1_3: bool
""".strip()

    def __init__(
        self,
        conditions_file_path: str,
        verification_file_path: str,
        runtime_file_path: str,
    ):
        self.__conditions_file_path = conditions_file_path
        self.__verification_file_path = verification_file_path
        self.__runtime_file_path = runtime_file_path

    def __write_conditions_file(self, data: str):
        os.makedirs(os.path.dirname(self.__conditions_file_path), exist_ok=True)
        with open(self.__conditions_file_path, "w+") as output_file:
            output_file.write(data)

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
        with open(self.__verification_file_path, "w") as output_file:
            output_file.write(self.__DEFAULT_RUNTIME_AND_VERIFICATION_FILE)

    def reset_runtime_file(self):
        """
        Resets the runtime file.
        """
        with open(self.__runtime_file_path, "w") as output_file:
            output_file.write(self.__DEFAULT_RUNTIME_AND_VERIFICATION_FILE)
