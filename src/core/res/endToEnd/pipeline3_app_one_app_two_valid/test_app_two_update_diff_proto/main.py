from instances import app_state, TEMPERATURE_SENSOR

def invariant() -> bool:
    return True

def iteration():
   if TEMPERATURE_SENSOR.read() != None and TEMPERATURE_SENSOR.read() > 22:
       unchecked_send_notif()
def unchecked_send_notif() -> None:
    a = 1 + 1