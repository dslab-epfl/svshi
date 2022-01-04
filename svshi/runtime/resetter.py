from typing import Final


class FileResetter:
    """
    File resetter.
    """

    __DEFAULT_RUNTIME_AND_VERIFICATION_FILE: Final = f"""
# Default file, will be overwritten while running
import dataclasses


@dataclasses.dataclass
class AppState:
    INT_0: int = 0
    INT_1: int = 0
    INT_2: int = 0
    INT_3: int = 0
    FLOAT_0: float = 0.0
    FLOAT_1: float = 0.0
    FLOAT_2: float = 0.0
    FLOAT_3: float = 0.0
    BOOL_0: bool = False
    BOOL_1: bool = False
    BOOL_2: bool = False
    BOOL_3: bool = False
    STR_0: str = ""
    STR_1: str = ""
    STR_2: str = ""
    STR_3: str = ""


@dataclasses.dataclass
class PhysicalState:
    GA_1_1_1: float
    GA_1_1_2: float
    GA_1_1_3: bool
    GA_1_1_4: bool

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

        with open(self.__conditions_file_path, "w+") as output_file:
            output_file.write(file)

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
