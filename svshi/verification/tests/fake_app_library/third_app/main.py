from devices import BINARY_SENSOR_INSTANCE_NAME, SWITCH_INSTANCE_NAME, TEMPERATURE_SENSOR_INSTANCE_NAME, HUMIDITY_SENSOR_INSTANCE_NAME

def invariant() -> bool:
    # Write the invariants of the app here
    # It can be any boolean expressions containing the read properties of the devices and constants
    return HUMIDITY_SENSOR_INSTANCE_NAME.read() < 82


def iteration():
    # Write your app code here
    if HUMIDITY_SENSOR_INSTANCE_NAME.read() > 30:
        SWITCH_INSTANCE_NAME.on()
