from instances import app_state, CO_TWO_DETECTOR, VENTILATION

def invariant() -> bool:
    if CO_TWO_DETECTOR.read() is not None and CO_TWO_DETECTOR.read() > 600.0:
        return VENTILATION.is_on()
    else:
        return True



def iteration():
    if CO_TWO_DETECTOR.read() is not None and CO_TWO_DETECTOR.read() > 600.0:
        VENTILATION.on()
    else:
        VENTILATION.off()

