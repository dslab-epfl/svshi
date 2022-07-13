from instances import app_state, TEMPERATURE_SENSOR, SWITCH, svshi_api

def invariant() -> bool:
    # Write the invariants of the app here
    # It can be any boolean expressions containing the read properties of the devices and constants
    # You CANNOT use external libraries here, nor `periodic` or `on_trigger` functions
    return svshi_api.check_time_property(svshi_api.Day(1),svshi_api.Hour(20),condition=SWITCH.is_on())


def iteration():
    # Write your app code here
    # You CANNOT use external libraries here, encapsulate calls to those in functions whose names start
    # with "periodic" or "on_trigger" (see documentation) and use these functions instead
    pass
