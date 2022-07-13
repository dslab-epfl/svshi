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
    GA_1_1_1: float
    GA_1_1_2: float
    GA_1_1_3: bool
    GA_1_1_4: bool



@dataclasses.dataclass
class IsolatedFunctionsValues:
    pass

@dataclasses.dataclass
class CheckState:
    start_frequency: int = 0
    start_condition_true: int = 0
    condition_was_true: bool = False


@dataclasses.dataclass
class InternalState:
    date_time: time.struct_time
    check_condition = {0: CheckState(), 1: CheckState()}
    app_files_runtime_folder_path: str