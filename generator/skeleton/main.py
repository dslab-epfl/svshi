import asyncio
from communication.client import with_knx_client
import time

SLEEP_INTERVAL_SECONDS = 0.01  # Change that if needed
GATEWAY_ADDRESS = "15.15.0"  # Replace this with the right address


def precond() -> bool:
    # Write the preconditions of the app here
    # It can be any boolean expressions containing the read properties of the devices and constants
    return True


def iteration():
    # Write your app code here
    print("Hello, world!")


####################################################################################################
# DO NOT MODIFY THE CODE BELOW


def main():
    """
    DO NOT MODIFY THIS FUNCTION !!!!!!
    """
    while True:
        iteration()
        time.sleep(SLEEP_INTERVAL_SECONDS)


if __name__ == "__main__":
    """
    DO NOT MODIFY THIS FUNCTION !!!!!!
    """
    asyncio.run(with_knx_client(GATEWAY_ADDRESS, main))
