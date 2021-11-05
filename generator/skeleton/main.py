import asyncio
from communication.client import with_knx_client


def main():
    # Write your app code here
    print("Hello, world!")


if __name__ == "__main__":
    loop = asyncio.get_event_loop()
    loop.run_until_complete(with_knx_client(main))
    loop.close()
