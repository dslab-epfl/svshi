from instances import app_state, svshi_api, CO_TWO, TEMPERATURE
from slack_sdk.web.client import WebClient

def invariant() -> bool:
    return True


def iteration():
    co2 = CO_TWO.read()
    temp = TEMPERATURE.read()

    minutes = svshi_api.get_minute_in_hour()
    if minutes % 10 == 0:
        svshi_api.trigger_if_not_running(
            on_trigger_send_message,
            f"The current CO2 level is: {co2} ppm\nThe current room temperature is: {temp}°C"
        )


def on_trigger_send_message(msg: str) -> None:
    token = "xoxb-2702504146389-2876497796775-Da6QXSF6DIMDH4SrE9oLXEmi"
    slack_client = WebClient(token=token)
    slack_client.chat_postMessage(channel="info-inn319", text=msg)