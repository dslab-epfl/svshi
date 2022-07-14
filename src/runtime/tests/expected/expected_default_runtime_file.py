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
