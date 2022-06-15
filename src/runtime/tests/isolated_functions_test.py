import os

from ..isolated_functions import (
    RuntimeIsolatedFunction,
    get_isolated_functions,
    get_svshi_api_register_on_trigger_consumer,
)
from .expected.expected_runtime_file import (
    app_on_trigger_write,
    another_app_periodic_write,
    svshi_api,
)

ISOLATED_FNS_FILE_PATH = "tests/expected/expected_isolated_fns.json"
SVSHI_HOME = os.environ["SVSHI_HOME"].replace("\\", "/")
RUNTIME_FILE_MODULE = (
    f"{SVSHI_HOME.split('/')[-1]}.src.runtime.tests.expected.expected_runtime_file"
)


def test_get_isolated_functions():
    fns = get_isolated_functions(RUNTIME_FILE_MODULE, ISOLATED_FNS_FILE_PATH)
    assert fns == [
        RuntimeIsolatedFunction(
            "app_on_trigger_write", app_on_trigger_write, int, None
        ),
        RuntimeIsolatedFunction(
            "another_app_periodic_write", another_app_periodic_write, float, 0
        ),
    ]


def test_get_svshi_api_register_on_trigger_consumer():
    fn = get_svshi_api_register_on_trigger_consumer(RUNTIME_FILE_MODULE)
    assert fn == svshi_api.register_on_trigger_consumer
