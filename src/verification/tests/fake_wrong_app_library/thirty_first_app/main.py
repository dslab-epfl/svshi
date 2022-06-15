from instances import app_state, svshi_api, BINARY_SENSOR_INSTANCE_NAME, SWITCH_INSTANCE_NAME, TEMPERATURE_SENSOR_INSTANCE_NAME, HUMIDITY_SENSOR_INSTANCE_NAME
import time

def invariant() -> bool:
    # Write the invariants of the app here
    # It can be any boolean expressions containing the read properties of the devices and constants
    return BINARY_SENSOR_INSTANCE_NAME.is_on() and SWITCH_INSTANCE_NAME.is_on()


def iteration():
    # Write your app code here
    if BINARY_SENSOR_INSTANCE_NAME.is_on():
        svshi_api.trigger_if_not_running(on_trigger_func)

def on_trigger_func() -> float:
    t = time.time()
    return t
