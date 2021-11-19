from verifier.verifier import Verifier
from verifier.tracker import StompWritesTracker
import argparse
import time


def parse_args():
    """
    Parses the arguments, returning the list of apps process ids.
    """
    parser = argparse.ArgumentParser(description="Runtime verifier.")
    parser.add_argument(
        "-l", "--list", nargs="+", help="A list of '<app_name>/<pid>'", required=True
    )
    args = parser.parse_args()
    return args.list


if __name__ == "__main__":
    apps_pids = parse_args()
    print("Welcome to the Pistis runtime verifier!")

    try:
        apps = {s.split("/")[0]: s.split("/")[1] for s in apps_pids}
        print("Connecting to the tracker... ", end="")
        with StompWritesTracker() as tracker:
            print("done!")
            print("Initializing verifier... ", end="")
            verifier = Verifier(apps)
            print("done!")

            # Register on message callback to verify writes
            tracker.on_message(verifier.verify_write)

            # Infinite loop for listening to messages
            while True:
                time.sleep(1)
                pass

    except KeyboardInterrupt:
        print("Exiting...")
