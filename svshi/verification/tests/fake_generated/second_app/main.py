import time
from devices import BINARY_SENSOR_INSTANCE_NAME, SWITCH_INSTANCE_NAME, TEMPERATURE_SENSOR_INSTANCE_NAME, HUMIDITY_SENSOR_INSTANCE_NAME

def precond() -> bool:
    # Write the preconditions of the app here
    # It can be any boolean expressions containing the read properties of the devices and constants
    return BINARY_SENSOR_INSTANCE_NAME.is_on() and SWITCH_INSTANCE_NAME.is_on()


def iteration():
    # Write your app code here
    print(BINARY_SENSOR_INSTANCE_NAME.is_on())
    print(unchecked_time())

def unchecked_time() -> float:
    return time.time()
