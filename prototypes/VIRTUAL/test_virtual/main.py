from instances import app_state, BINARY, SWITCH

def invariant() -> bool:
    return True


def iteration():
    if BINARY.is_on(): 
        SWITCH.on()
    else:
        SWITCH.off()
