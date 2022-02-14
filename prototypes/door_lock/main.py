from instances import app_state, DOOR_LOCK_SENSOR, PRESENCE_DETECTOR
from slack_sdk.web.client import WebClient

def invariant() -> bool:
     return True


def iteration():
    if not PRESENCE_DETECTOR.is_on() and not DOOR_LOCK_SENSOR.is_on():
        if not app_state.BOOL_0:
            if app_state.INT_0 > 1:
                unchecked_send_message("The door at office INN319 is still opened but nobody is there!")
                app_state.BOOL_0 = True
            else:
                app_state.INT_0 += 1
    else:
        app_state.INT_0 = 0
        if app_state.BOOL_0:
            if PRESENCE_DETECTOR.is_on():
                unchecked_send_message("Someone entered the office INN319. All good!")
            elif DOOR_LOCK_SENSOR.is_on():
                unchecked_send_message("Someone locked the door of the office INN319. All good!")
                
            app_state.BOOL_0 = False


def unchecked_send_message(msg: str) -> None:
    token = "xoxb-2702504146389-2876497796775-3ccE1bS5XzrzfjpMLs7ysoUj"
    slack_client = WebClient(token=token)
    slack_client.chat_postMessage(channel="inn319", text=msg)