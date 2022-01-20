import time
from instances import app_state, BINARY_SENSOR_INSTANCE_NAME, SWITCH_INSTANCE_NAME, TEMPERATURE_SENSOR_INSTANCE_NAME, HUMIDITY_SENSOR_INSTANCE_NAME

def invariant() -> bool:
    # Write the invariants of the app here
    # It can be any boolean expressions containing the read properties of the devices and constants
    return BINARY_SENSOR_INSTANCE_NAME.is_on() and SWITCH_INSTANCE_NAME.is_on()


def iteration():
    # Write your app code here
    if BINARY_SENSOR_INSTANCE_NAME.is_on() and unchecked_time() > 2.0:
        SWITCH_INSTANCE_NAME.on()

def unchecked_time() -> float:
    return time.time()
