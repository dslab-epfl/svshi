from instances import app_state, DOOR_LOCK_SENSOR, PRESENCE_DETECTOR
from slack_sdk.web.client import WebClient

def invariant() -> bool:
     return True


def iteration():
    if not PRESENCE_DETECTOR.is_on() and not DOOR_LOCK_SENSOR.is_on():
        if app_state.INT_0 > 5:
            unchecked_send_message("The door at office INN319 is still opened but nobody is there!")
        else:
            app_state.INT_0 += 1
    else:
        app_state.INT_0 = 0


def unchecked_send_message(msg: str) -> None:
    token = "xoxb-2702504146389-2876497796775-r21j0QnaGcyfjwEVDFrYpkYO"
    slack_client = WebClient(token=token)
    slack_client.chat_postMessage(channel="test_python", text=msg)