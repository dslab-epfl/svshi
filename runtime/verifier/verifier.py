from verifier.tracker import Message
import subprocess


class Verifier:
    """
    App verifier.
    """
    def __init__(self, apps_pids: dict):
        self.__apps_pids = apps_pids

    def __kill_app(self, app_name: str):
        app_pid = self.__apps_pids[app_name]
        subprocess.run(f"kill {app_pid}", shell=True)

    def verify_write(self, message: Message):
        """
        Verifies that the write contained in the message does not violate the safety conditions.
        """
        app = message.app_name
        device = message.device
        data = message.data
        print(app, device, data)
