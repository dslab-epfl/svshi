from instances import app_state, BINARY_SENSOR_INSTANCE_NAME, SWITCH_INSTANCE_NAME, TEMPERATURE_SENSOR_INSTANCE_NAME, HUMIDITY_SENSOR_INSTANCE_NAME
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
    if uncheckedcompute_bool() and not app_state.BOOL_1:
        app_state.INT_1 = 42
        unchecked_print(BINARY_SENSOR_INSTANCE_NAME.is_on())
    else:
        v = unchecked_return_two()
        unchecked_print(v)
        unchecked_print("file4.csv")

def uncheckedcompute_bool() -> bool:
    """
    post: __return__ == False
    """
    return False

def unchecked_return_two() -> int:
    """
    pre: True
    post: __return__ > 0
    post: __return__ != 3
    """
    return 2

def unchecked_print(s) -> None:
    print(s)
