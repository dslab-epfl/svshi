from instances import app_state, TEMPERATURE_SENSOR, SWITCH
import time

def invariant() -> bool:
    return True


def iteration():
    a = unchecked_get_time()

def unchecked_get_time() -> float:
    return time.time()