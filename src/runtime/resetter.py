import textwrap
from typing import Final


class FileResetter:
    """
    File resetter.
    """

    __DEFAULT_RUNTIME_FILE: Final = textwrap.dedent(
        """\
        # Default file, will be overwritten while running
        from typing import Callable, IO, Optional, TypeVar
        from typing import Optional
        import dataclasses
        import sys
        if sys.version_info < (3, 10):
            from typing_extensions import ParamSpec
        else:
            from typing import ParamSpec
        import time


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


        @dataclasses.dataclass
        class PhysicalState:
            GA_0_0_1: bool
            GA_0_0_2: bool
            GA_0_0_3: float
            GA_0_0_4: float
            GA_0_0_5: int



        @dataclasses.dataclass
        class IsolatedFunctionsValues:
            pass



        @dataclasses.dataclass
        class InternalState:
            date_time: time.struct_time
            app_files_runtime_folder_path: str
        """
    )

    __DEFAULT_VERIFICATION_FILE: Final = textwrap.dedent(
        '''\
        # Default file, will be overwritten while running
        from typing import Callable, IO, Optional, TypeVar
        from typing import Optional
        import dataclasses
        import time


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


        @dataclasses.dataclass
        class PhysicalState:
            GA_0_0_1: bool
            GA_0_0_2: bool
            GA_0_0_3: float
            GA_0_0_4: float
            GA_0_0_5: int



        @dataclasses.dataclass
        class IsolatedFunctionsValues:
            pass



        @dataclasses.dataclass
        class InternalState:
            """
            inv: 0 <= self.time_hour <= 23
            inv: 0 <= self.time_min <= 59
            inv: 1 <= self.time_day <= 31
            inv: 1 <= self.time_weekday <= 7
            inv: 1 <= self.time_month <= 12
            inv: 0 <= self.time_year
            """
            time_hour: int
            time_min: int
            time_day: int
            time_weekday: int
            time_month: int
            time_year: int
        '''
    )

    def __init__(
        self,
        conditions_file_path: str,
        verification_file_path: str,
        runtime_file_path: str,
        isolated_fns_file_path: str,
    ):
        self.__conditions_file_path = conditions_file_path
        self.__verification_file_path = verification_file_path
        self.__runtime_file_path = runtime_file_path
        self.__isolated_fn_file_path = isolated_fns_file_path

    def reset_conditions_file(self):
        """
        Resets the conditions file.
        """
        file = textwrap.dedent(
            """\
            # Default file, will be overwritten while running

            from .runtime_file import AppState, PhysicalState, InternalState

            def check_conditions(physical_state: PhysicalState, internal_state: InternalState, **app_state: AppState) -> bool:
                return True
            """
        )

        with open(self.__conditions_file_path, "w+") as output_file:
            output_file.write(file)

    def reset_verification_file(self):
        """
        Resets the verification file.
        """
        with open(self.__verification_file_path, "w") as output_file:
            output_file.write(self.__DEFAULT_VERIFICATION_FILE)

    def reset_runtime_file(self):
        """
        Resets the runtime file.
        """
        with open(self.__runtime_file_path, "w") as output_file:
            output_file.write(self.__DEFAULT_RUNTIME_FILE)

    def reset_isolated_fns_file(self):
        """
        Resets the isolated_fns.json file.
        """
        with open(self.__isolated_fn_file_path, "w") as output_file:
            output_file.write("[]")
