from instances import app_state, svshi_api, BINARY_SENSOR_INSTANCE_NAME, SWITCH_INSTANCE_NAME, TEMPERATURE_SENSOR_INSTANCE_NAME, HUMIDITY_SENSOR_INSTANCE_NAME
from slack_sdk.web.client import WebClient
from slack_sdk.web.slack_response import SlackResponse
from decouple import config

FILE = "file3.json"

def invariant() -> bool:
    # Write the invariants of the app here
    # It can be any boolean expressions containing the read properties of the devices and constants
    return BINARY_SENSOR_INSTANCE_NAME.is_on() and TEMPERATURE_SENSOR_INSTANCE_NAME.read() > 18 and not app_state.BOOL_1


def iteration():
    # Write your app code here
    if svshi_api.get_latest_value(periodic_compute_bool) and not app_state.BOOL_1:
        app_state.INT_1 = 42
        svshi_api.trigger_if_not_running(on_trigger_print)(BINARY_SENSOR_INSTANCE_NAME.is_on())
    else:
        v = svshi_api.get_latest_value(periodic_return_two)
        svshi_api.trigger_if_not_running(on_trigger_print)(v)
        svshi_api.trigger_if_not_running(on_trigger_print)("file4.csv")
        svshi_api.trigger_if_not_running(on_trigger_do_nothing)

def periodic_compute_bool() -> bool:
    """
    period: 5
    """
    return False

def periodic_return_two() -> int:
    """
    period: 100
    """
    p = svshi_api.get_file_path("file1.txt")
    f = svshi_api.get_file_text_mode("file2.txt", "w")
    f2 = svshi_api.get_file_binary_mode("file3.txt", "ar")
    return 2

def on_trigger_print(s) -> None:
    print(s)

def on_trigger_do_nothing() -> None:
    return None
