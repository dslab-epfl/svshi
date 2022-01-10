from instances import app_state, HUMIDITY_SENSOR
from slack_sdk.web.client import WebClient


def unchecked_send_message(channel: str, message: str) -> None:
    # token = str(config("SLACK_BOT_TOKEN", cast=str))
    token = "xoxb-2702504146389-2876497796775-r21j0QnaGcyfjwEVDFrYpkYO"
    slack_client = WebClient(token=token)
    slack_client.chat_postMessage(channel=channel, text=message)


def invariant() -> bool:
    return True


def iteration():
    humidity_level = HUMIDITY_SENSOR.read()
    if humidity_level != None and humidity_level < 20:
        if app_state.INT_0 > 20:
            unchecked_send_message("#plants", "The plant needs water!")
            app_state.INT_0 = 0
        else: 
            app_state.INT_0 += 1
    else:
        app_state.INT_0 = 0
