from dataclasses import dataclass
from importlib import import_module
from inspect import signature
import json
from typing import Callable, Dict, List, Optional, Union


@dataclass(frozen=True)
class RuntimeIsolatedFunction:
    name: str
    code: Callable
    return_type: type
    period: Optional[int]


def get_isolated_functions(
    runtime_file_module: str, isolated_fns_file_path: str
) -> List[RuntimeIsolatedFunction]:
    """
    Get a list of the isolated functions of the `isolated_fns_file_path` with their code
    from the `runtime_file_module`.
    """
    with open(isolated_fns_file_path, "r") as f:
        fns: List[Dict[str, Union[str, int, None]]] = json.load(f)
    isolated_fns = []
    runtime_module = import_module(runtime_file_module)
    for fn in fns:
        fn_name = fn["name"]
        # Import the function code from the runtime file
        fn_code: Callable = getattr(runtime_module, fn_name)
        ret_type = signature(fn_code).return_annotation
        isolated_fns.append(
            RuntimeIsolatedFunction(
                fn_name, fn_code, ret_type or type(None), fn["period"]
            )
        )
    return isolated_fns


def get_svshi_api_register_on_trigger_consumer(
    runtime_file_module: str,
) -> Callable[[Callable], None]:
    """Get svshi_api.register_on_trigger_consumer from the runtime module."""
    return getattr(
        getattr(import_module(runtime_file_module), "svshi_api"),
        "register_on_trigger_consumer",
    )
