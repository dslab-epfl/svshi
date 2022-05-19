from instances import app_state, svshi_api, LIGHT, SENSOR

def invariant() -> bool:
    # Write the invariants of the app here
    # It can be any boolean expressions containing the read properties of the devices and constants
    # You CANNOT use external libraries here, nor unchecked functions
    return True


def iteration():
    if SENSOR.is_on():
        LIGHT.on()
    else:
        LIGHT.off()