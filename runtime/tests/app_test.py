from ..app import App, get_addresses_listeners, get_apps
from unittest.mock import Mock


def test_app_notify():
    app_called = [False]

    def app_code(s):
        app_called[0] = True

    app = App("test", "tests", app_code)

    app.notify(Mock())

    assert app_called[0] == True


def test_app_stop():
    app = App("test", "tests", Mock())

    app.stop()

    assert app.should_run == False


def test_app_get_apps():
    apps = sorted(
        get_apps(
            "tests/fake_app_library",
            "smartinfra.runtime.tests.expected.expected_default_verification_file",
        ),
        key=lambda a: a.name,
    )

    first_app = apps[0]
    second_app = apps[1]

    assert first_app.name == "another_app"
    assert first_app.directory == "tests/fake_app_library"
    assert first_app.code is not None
    assert first_app.is_privileged == False
    assert second_app.name == "app"
    assert second_app.directory == "tests/fake_app_library"
    assert second_app.code is not None
    assert second_app.is_privileged == True


def test_app_get_addresses_listeners():
    app = App("app", "tests/fake_app_library", Mock(), is_privileged=True)
    another_app = App(
        "another_app", "tests/fake_app_library", Mock(), is_privileged=False
    )
    apps = [app, another_app]
    listeners = get_addresses_listeners(apps)

    assert listeners == {
        "0/0/1": [another_app, app],
        "0/0/2": [another_app, app],
        "0/0/3": [another_app, app],
        "0/0/4": [another_app, app],
    }
