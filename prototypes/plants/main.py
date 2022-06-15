from instances import app_state, svshi_api, HUMIDITY_SENSOR
from slack_sdk.web.client import WebClient


def on_trigger_send_message(message: str) -> None:
    token = "xoxb-2702504146389-2876497796775-Da6QXSF6DIMDH4SrE9oLXEmi"
    slack_client = WebClient(token=token)
    slack_client.chat_postMessage(channel="#plants", text=message)


def invariant() -> bool:
    return True


def iteration():
    humidity_level = HUMIDITY_SENSOR.read()
    if humidity_level != None and humidity_level < 20:
        if app_state.INT_0 > 20:
            svshi_api.trigger_if_not_running(
                on_trigger_send_message,
                "The plant needs water!"
            )
            app_state.BOOL_0 = True
            app_state.INT_0 = 0
        else: 
            app_state.INT_0 += 1
    else:
        if app_state.BOOL_0:
            svshi_api.trigger_if_not_running(
                on_trigger_send_message,
                "Someone watered the plant. All good!"
            )
            app_state.BOOL_0 = False
            
        app_state.INT_0 = 0
