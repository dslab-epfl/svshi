from instances import app_state, CO_TWO, TEMPERATURE
import time
from slack_sdk.web.client import WebClient

def invariant() -> bool:
    return True


def iteration():
    co2 = CO_TWO.read()
    temp = TEMPERATURE.read()

    tm = unchecked_get_time()
    diff = tm - app_state.FLOAT_0
    if diff > 3600:
        unchecked_send_message(f"The current CO2 level is: {co2} ppm\nThe current room temperature is: {temp}°C")
        app_state.FLOAT_0 = tm


def unchecked_get_time() -> float:
    return time.time()

def unchecked_send_message(msg: str) -> None:
    token = "xoxb-2702504146389-2876497796775-Da6QXSF6DIMDH4SrE9oLXEmi"
    slack_client = WebClient(token=token)
    slack_client.chat_postMessage(channel="info-inn319", text=msg)