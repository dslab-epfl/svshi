from verifier.verifier import Verifier
from verifier.tracker import StompWritesTracker
import argparse


def parse_args():
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
        with StompWritesTracker() as tracker:
            verifier = Verifier(apps)
            tracker.on_message(verifier.verify_write)
            while True:
                pass
    except KeyboardInterrupt:
        print("Exiting...")
