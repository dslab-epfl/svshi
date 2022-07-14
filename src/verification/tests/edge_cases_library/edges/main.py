from typing import Generator, Iterable
from instances import app_state, svshi_api, BINARY_SENSOR_INSTANCE_NAME, SWITCH_INSTANCE_NAME


def invariant() -> bool:
    return (
        (BINARY_SENSOR_INSTANCE_NAME.is_on() or app_state.INT_0 == 42)
        and SWITCH_INSTANCE_NAME.is_on()
    ) or (
        not (BINARY_SENSOR_INSTANCE_NAME.is_on() or app_state.INT_0 == 42)
        and not SWITCH_INSTANCE_NAME.is_on()
    )


def iteration():
    def yield_fun() -> Iterable[bool]:
        yield not SWITCH_INSTANCE_NAME.is_on()

    def return_fun() -> bool:
        return not SWITCH_INSTANCE_NAME.is_on()

    if BINARY_SENSOR_INSTANCE_NAME.is_on() or app_state.INT_0 == 42:
        svshi_api.trigger_if_not_running(on_trigger_send_email)("test@test.com")
        SWITCH_INSTANCE_NAME.on()
    else:
        SWITCH_INSTANCE_NAME.off()

    a = app_state.INT_0 + 1
    latest_int = svshi_api.get_latest_value(periodic_return_int)
    if latest_int == 42:
        b = [latest_int, 2]
        g: list = [x for x in b]
        app_state.INT_2 += 5
    else:
        c = (lambda d: d + 1)(a)

    stuff = [[y := 2, x / y] for x in range(5)]

    y = not (svshi_api.get_latest_value(periodic_return_int) == 31)
    d = {"a": SWITCH_INSTANCE_NAME.is_on()}
    {k: v for k, v in d.items()}

    s = set(SWITCH_INSTANCE_NAME.is_on())

    string = f"this is a beautiful string {SWITCH_INSTANCE_NAME.is_on()}"

    svshi_api.trigger_if_not_running(on_trigger_send_email)(addr="test")


def periodic_iteration() -> None:
    """period: 5"""
    def yield_fun() -> Iterable[bool]:
        yield not SWITCH_INSTANCE_NAME.is_on()

    def return_fun() -> bool:
        return not SWITCH_INSTANCE_NAME.is_on()

    if BINARY_SENSOR_INSTANCE_NAME.is_on() or app_state.INT_0 == 42:
        svshi_api.trigger_if_not_running(on_trigger_send_email)("test@test.com")
        SWITCH_INSTANCE_NAME.on()
    else:
        SWITCH_INSTANCE_NAME.off()

    a = app_state.INT_0 + 1
    latest_int = svshi_api.get_latest_value(periodic_return_int)
    if latest_int == 42:
        b = [latest_int, 2]
        g = [x for x in b]
    else:
        c = (lambda d: d + 1)(a)

    y = not (svshi_api.get_latest_value(periodic_return_int) == 31)
    d = {"a": SWITCH_INSTANCE_NAME.is_on()}
    {k: v for k, v in d.items()}

    s = set(SWITCH_INSTANCE_NAME.is_on())

    string = f"this is a beautiful string {SWITCH_INSTANCE_NAME.is_on()}"

    svshi_api.trigger_if_not_running(on_trigger_send_email)(addr="test")


def on_trigger_send_email(addr: str) -> None:
    # do stuff
    a = 1 + 1


def periodic_return_int() -> int:
    """period: 10"""
    return 42
