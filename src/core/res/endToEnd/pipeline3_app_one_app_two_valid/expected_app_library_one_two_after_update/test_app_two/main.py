from instances import app_state, svshi_api, TEMPERATURE_SENSOR

def invariant() -> bool:
    return True

def iteration():
   if TEMPERATURE_SENSOR.read() != None and TEMPERATURE_SENSOR.read() > 22:
       svshi_api.trigger_if_not_running(on_trigger_send_notif)
   if TEMPERATURE_SENSOR.read() != None and TEMPERATURE_SENSOR.read() > 42:
       app_state.INT_1 = 42
def on_trigger_send_notif() -> None:
    a = 1 + 1