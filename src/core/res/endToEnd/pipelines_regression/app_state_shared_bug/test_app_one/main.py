from instances import app_state, BINARY_SENSOR_INSTANCE_NAME, SWITCH_INSTANCE_NAME

def invariant() -> bool:
    return app_state.INT_0 == 40

def iteration():
    app_state.INT_0 = 40
    if BINARY_SENSOR_INSTANCE_NAME.is_on():
        SWITCH_INSTANCE_NAME.on()
    else:
        SWITCH_INSTANCE_NAME.off()
    
