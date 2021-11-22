import asyncio
from communication.client import with_knx_client
import time

SLEEP_INTERVAL_SECONDS = 0.01 # Change that if needed
GATEWAY_ADDRESS = "15.15.0"  # Replace this with the right address


def iteration():
    # Write your app code here
    print("Hello, world!")


def main():
    '''
    DO NOT MODIFY THIS FUNCTION !!!!!!
    '''
    while True:
        iteration()
        time.sleep(SLEEP_INTERVAL_SECONDS)


if __name__ == "__main__":
    '''
    DO NOT MODIFY THIS FUNCTION !!!!!!
    '''
    loop = asyncio.get_event_loop()
    loop.run_until_complete(with_knx_client(GATEWAY_ADDRESS, main))
    loop.close()
