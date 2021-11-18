import asyncio
from communication.client import with_knx_client

GATEWAY_ADDRESS = "15.15.0"  # Replace this with the right address


def main():
    # Write your app code here
    print("Hello, world!")


if __name__ == "__main__":
    asyncio.run(with_knx_client(GATEWAY_ADDRESS, main))
