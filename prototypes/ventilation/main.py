from instances import app_state, PRESENCE_DETECTOR, CO_TWO_SENSOR, VENTILATION

from gcsa.google_calendar import GoogleCalendar
from datetime import datetime

def invariant() -> bool:
    if (PRESENCE_DETECTOR.is_on() and VENTILATION.is_on() != None) or (CO_TWO_SENSOR.read() is not None and CO_TWO_SENSOR.read() > 600.0  and VENTILATION.is_on() != None):
        return VENTILATION.is_on()
    else:
        return True


def iteration():
    if PRESENCE_DETECTOR.is_on() or (CO_TWO_SENSOR.read() is not None and CO_TWO_SENSOR.read() > 600.0) or unchecked_in_meeting("sam.chassot@gmail.com"):
        VENTILATION.on()
    else:
        VENTILATION.off()

def unchecked_in_meeting(email: str) -> bool:
    gc = GoogleCalendar(email, credentials_path="credentials.json")

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
        diff_now_end_days = diff_end_now.days
        diff_now_end_minutes = diff_end_now.seconds // 60

        diff_now_end = now - e.end
        diff_now_end_days = diff_now_end.days
        diff_now_end_minutes = diff_now_end.seconds // 60

        in_meeting = diff_start_now_days < 0 and diff_now_end_days >= 0 and diff_now_end_minutes >= 0

        meeting_in_next_fifteen_min = diff_start_now_days == 0 and diff_start_now_minutes <= 15

        meeting_in_past_thirty_min = diff_now_end_days == 0 and diff_now_end_minutes <= 30

        acc = acc or in_meeting or meeting_in_next_fifteen_min or meeting_in_past_thirty_min

        if acc: 
            break
    return acc
