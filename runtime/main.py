from verifier.verifier import Verifier
from verifier.tracker import StompWritesTracker
import signal

if __name__ == "__main__":
    with StompWritesTracker() as tracker:
        verifier = Verifier()
        tracker.on_message(verifier.verify_write)
        signal.pause()
