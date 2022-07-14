from instances import app_state, svshi_api, TEMPERATURE_SENSOR, SWITCH
import time

def invariant() -> bool:
    return True


def iteration():
    a = svshi_api.get_latest_value(periodic_get_time)


def periodic_get_time() -> float:
    """period: 0"""
    return time.time()
