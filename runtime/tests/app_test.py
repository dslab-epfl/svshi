from ..app import App, get_addresses_listeners, get_apps
from unittest.mock import Mock


def test_app_notify():
    app_called = [False]

    def app_code(s):
        app_called[0] = True

    app = App("test", app_code)

    app.notify(Mock())

    assert app_called[0] == True


def test_app_stop():
    app = App("test", Mock())

    app.stop()

    assert app.should_run == False


def test_app_get_apps():
    apps = get_apps(
        "tests/fake_app_library", "smartinfra.runtime.tests.verification_file"
    )

    first_app = apps[0]
    second_app = apps[1]

    assert first_app.name == "app"
    assert first_app.code is not None
    assert second_app.name == "another_app"
    assert second_app.code is not None


def test_app_get_addresses_listeners():
    listeners = get_addresses_listeners("tests/fake_app_library")

    assert listeners == {
        "0/0/1": ["app", "another_app"],
        "0/0/2": ["app", "another_app"],
        "0/0/3": ["app", "another_app"],
        "0/0/4": ["app", "another_app"],
    }
