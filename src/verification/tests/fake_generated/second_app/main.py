from instances import app_state, svshi_api, BINARY_SENSOR_INSTANCE_NAME, SWITCH_INSTANCE_NAME, TEMPERATURE_SENSOR_INSTANCE_NAME, HUMIDITY_SENSOR_INSTANCE_NAME

def invariant() -> bool:
    # Write the invariants of the app here
    # It can be any boolean expressions containing the read properties of the devices and constants
    return BINARY_SENSOR_INSTANCE_NAME.is_on() and SWITCH_INSTANCE_NAME.is_on() \
           and svshi_api.check_time_property(svshi_api.Day(1), svshi_api.Hour(1), SWITCH_INSTANCE_NAME.is_on())


def iteration():
    # Write your app code here
    latest_float = svshi_api.get_latest_value(periodic_float)
    if BINARY_SENSOR_INSTANCE_NAME.is_on() and latest_float and latest_float > 2.0:
        SWITCH_INSTANCE_NAME.on()
        svshi_api.trigger_if_not_running(on_trigger_do_nothing)()

def periodic_float() -> float:
    """period: 0"""
    return 42.0

def on_trigger_do_nothing() -> None:
    return None
