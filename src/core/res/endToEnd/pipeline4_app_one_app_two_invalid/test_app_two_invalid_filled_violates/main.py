from instances import app_state, TEMPERATURE_SENSOR, SWITCH

def invariant() -> bool:
    return True


def iteration():
    if TEMPERATURE_SENSOR.read() != None and TEMPERATURE_SENSOR.read() > 22:
        SWITCH.on()
