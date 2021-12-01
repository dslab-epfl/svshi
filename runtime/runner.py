import time
from typing import List

from runtime.app import App


class AppRunner:

    __SLEEP_INTERVAL_SECONDS = 0.01  # Change that if needed

    def __init__(self, apps: List[App]):
        self.__apps = sorted(apps, key=lambda app: app.priority)
        self.__should_run = False

    def run_all(self):
        self.__should_run = True
        while self.__should_run:
            [app.code() for app in self.__apps if app.should_run]
            time.sleep(self.__SLEEP_INTERVAL_SECONDS)

    def stop(self, app_name: str):
        for app in self.__apps:
            if app.name == app_name:
                app.should_run = False

    def stop_all(self):
        self.__should_run = False
