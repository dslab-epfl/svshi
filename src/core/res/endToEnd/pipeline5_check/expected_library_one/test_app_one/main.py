from instances import app_state, svshi_api, BINARY_SENSOR_INSTANCE_NAME, SWITCH_INSTANCE_NAME

def invariant() -> bool:
    return (svshi_api.get_hour_of_the_day() >= 10 and SWITCH_INSTANCE_NAME.is_on()) or (not (svshi_api.get_hour_of_the_day() >= 10) and not SWITCH_INSTANCE_NAME.is_on())


def iteration():
    if svshi_api.get_hour_of_the_day() >= 10 :
        svshi_api.trigger_if_not_running(on_trigger_send_email)("test@test.com")
        SWITCH_INSTANCE_NAME.on()
    else:
        SWITCH_INSTANCE_NAME.off()
    

def on_trigger_send_email(addr: str) -> None:
    # do stuff
    a = 1+1
    
