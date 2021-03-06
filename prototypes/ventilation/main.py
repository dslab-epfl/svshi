from instances import app_state, svshi_api, PRESENCE_DETECTOR, CO_TWO_SENSOR, VENTILATION

from gcsa.google_calendar import GoogleCalendar
from datetime import datetime

def invariant() -> bool:
    if (PRESENCE_DETECTOR.is_on() and VENTILATION.is_on() != None) or (CO_TWO_SENSOR.read() is not None and CO_TWO_SENSOR.read() > 900.0  and VENTILATION.is_on() != None):
        return VENTILATION.is_on()
    else:
        return True


def iteration():
    if PRESENCE_DETECTOR.is_on() or (CO_TWO_SENSOR.read() is not None and CO_TWO_SENSOR.read() > 900.0) or svshi_api.get_latest_value(periodic_in_meeting):
        VENTILATION.on()
    else:
        VENTILATION.off()

def periodic_in_meeting() -> bool:
    """
    period: 10
    """
    email = "sam.chassot@gmail.com"
    gc = GoogleCalendar(email, credentials_path=svshi_api.get_file_path("credentials.json"), token_path=svshi_api.get_file_path("token.pickle"))

    acc = False
    for e in gc:
        tzInfo = e.start.tzinfo
        now = datetime.now(tzInfo)
        diff_start_now = e.start - now
        diff_start_now_days = diff_start_now.days
        diff_start_now_minutes = diff_start_now.seconds // 60

        tzInfo = e.end.tzinfo
        now = datetime.now(tzInfo)
        diff_end_now = e.end - now
        diff_end_now_days = diff_end_now.days
        diff_end_now_minutes = diff_end_now.seconds // 60

        in_meeting = diff_start_now_days < 0 and diff_end_now_days >= 0 and diff_end_now_minutes >= 0

        meeting_in_next_fifteen_min = diff_start_now_days == 0 and diff_start_now_minutes <= 15

        acc = acc or in_meeting or meeting_in_next_fifteen_min

        if acc: 
            break
    return acc
