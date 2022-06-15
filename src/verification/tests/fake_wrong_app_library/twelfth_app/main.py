from instances import app_state, svshi_api, BINARY_SENSOR_INSTANCE_NAME, SWITCH_INSTANCE_NAME, TEMPERATURE_SENSOR_INSTANCE_NAME, HUMIDITY_SENSOR_INSTANCE_NAME

def invariant() -> bool:
    # Write the invariants of the app here
    # It can be any boolean expressions containing the read properties of the devices and constants
    return BINARY_SENSOR_INSTANCE_NAME.is_on() and SWITCH_INSTANCE_NAME.is_on()


def iteration():
    # Write your app code here
    f = svshi_api.get_file_text_mode("file1.txt", "wr")
    if BINARY_SENSOR_INSTANCE_NAME.is_on():
        svshi_api.get_latest_value(periodic_func)

def periodic_func() -> int:
    """period: 33"""
    return 2
