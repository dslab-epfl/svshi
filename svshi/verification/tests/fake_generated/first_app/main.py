from devices import BINARY_SENSOR_INSTANCE_NAME, SWITCH_INSTANCE_NAME, TEMPERATURE_SENSOR_INSTANCE_NAME, HUMIDITY_SENSOR_INSTANCE_NAME

def precond() -> bool:
    # Write the preconditions of the app here
    # It can be any boolean expressions containing the read properties of the devices and constants
    return BINARY_SENSOR_INSTANCE_NAME.is_on() and TEMPERATURE_SENSOR_INSTANCE_NAME.read() > 18


def iteration():
    # Write your app code here
    if uncheckedcompute_bool():
        print(BINARY_SENSOR_INSTANCE_NAME.is_on())
    else:
        v = unchecked_return_two()
        print(v)

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
