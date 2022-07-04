from instances import app_state, svshi_api, BINARY_SENSOR_INSTANCE_NAME, SWITCH_INSTANCE_NAME, TEMPERATURE_SENSOR_INSTANCE_NAME, HUMIDITY_SENSOR_INSTANCE_NAME, CO_TWO_SENSOR_INSTANCE_NAME, DIMMER_SENSOR_INSTANCE_NAME, DIMMER_ACTUATOR_INSTANCE_NAME

FILE = "file1.json"

def invariant() -> bool:
    # Write the invariants of the app here
    # It can be any boolean expressions containing the read properties of the devices and constants
    return HUMIDITY_SENSOR_INSTANCE_NAME.read() < 82 and ((2 <= svshi_api.get_hour_of_the_day() <=3 and not SWITCH_INSTANCE_NAME.is_on()) or not(2 <= svshi_api.get_hour_of_the_day() <=3))


def iteration():
    # Write your app code here
    if HUMIDITY_SENSOR_INSTANCE_NAME.read() > 30 and CO_TWO_SENSOR_INSTANCE_NAME.read() > 600.0:
        another_file = "file2.csv"
        SWITCH_INSTANCE_NAME.on()
        DIMMER_ACTUATOR_INSTANCE_NAME.set(34)
    elif 2 <= svshi_api.get_hour_of_the_day() <=3:
        t = svshi_api.get_minute_in_hour()
        SWITCH_INSTANCE_NAME.off()
