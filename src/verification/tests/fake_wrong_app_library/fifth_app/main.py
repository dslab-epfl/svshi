import time
from instances import app_state, svshi_api, BINARY_SENSOR_INSTANCE_NAME, SWITCH_INSTANCE_NAME, TEMPERATURE_SENSOR_INSTANCE_NAME, HUMIDITY_SENSOR_INSTANCE_NAME

def invariant() -> bool:
    # Write the invariants of the app here
    # It can be any boolean expressions containing the read properties of the devices and constants
    return BINARY_SENSOR_INSTANCE_NAME.is_on() and SWITCH_INSTANCE_NAME.is_on() and svshi_api.get_latest_value(periodic_func) == 2


def iteration():
    # Write your app code here
    print(BINARY_SENSOR_INSTANCE_NAME.is_on())
    time.sleep(2)

def periodic_func() -> int:
    """period: 2"""
    return 2
