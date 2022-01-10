from instances import app_state, BINARY_SENSOR_INSTANCE_NAME, SWITCH_INSTANCE_NAME

def invariant() -> bool:
    return ((BINARY_SENSOR_INSTANCE_NAME.is_on() or app_state.INT_0 == 42) and SWITCH_INSTANCE_NAME.is_on()) or (not (BINARY_SENSOR_INSTANCE_NAME.is_on() or app_state.INT_0 == 42) and not SWITCH_INSTANCE_NAME.is_on())


def iteration():
    if BINARY_SENSOR_INSTANCE_NAME.is_on() or app_state.INT_0 == 42:
        unchecked_send_email("test@test.com")
        SWITCH_INSTANCE_NAME.on()
    else:
        SWITCH_INSTANCE_NAME.on()
    

def unchecked_send_email(addr: str) -> None:
    # do stuff
    a = 1+1
    
