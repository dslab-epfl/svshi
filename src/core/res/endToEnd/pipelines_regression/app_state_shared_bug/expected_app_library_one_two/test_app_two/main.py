from instances import app_state, TEMPERATURE_SENSOR

def invariant() -> bool:
    return True

def iteration():
   if TEMPERATURE_SENSOR.read() != None and TEMPERATURE_SENSOR.read() > 42:
       app_state.INT_0 = 42
