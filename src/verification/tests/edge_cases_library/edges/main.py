from typing import Generator, Iterable
from instances import app_state, BINARY_SENSOR_INSTANCE_NAME, SWITCH_INSTANCE_NAME

def invariant() -> bool:
    return ((BINARY_SENSOR_INSTANCE_NAME.is_on() or app_state.INT_0 == 42) and SWITCH_INSTANCE_NAME.is_on()) or (not (BINARY_SENSOR_INSTANCE_NAME.is_on() or app_state.INT_0 == 42) and not SWITCH_INSTANCE_NAME.is_on())


def iteration():
    def yield_fun()-> Iterable[bool]:
        yield not SWITCH_INSTANCE_NAME.is_on()

    def return_fun()-> bool:
        return not SWITCH_INSTANCE_NAME.is_on()
    

    if BINARY_SENSOR_INSTANCE_NAME.is_on() or app_state.INT_0 == 42:
        unchecked_send_email("test@test.com")
        SWITCH_INSTANCE_NAME.on()
    else:
        SWITCH_INSTANCE_NAME.off()

    a = app_state.INT_0 + 1
    if unchecked_return_int() == 42:
        b = [unchecked_return_int(), 2]
        g = [x for x in b]
    else:
        c = (lambda d: d + 1)(a)
    
    y = not (unchecked_return_int() == 31)
    d = {"a": SWITCH_INSTANCE_NAME.is_on()}
    {k: v for k, v in d.items()}

    s = set(SWITCH_INSTANCE_NAME.is_on())

    string = f"this is a beautiful string {SWITCH_INSTANCE_NAME.is_on()}"

    unchecked_send_email(addr="test")

    
def unchecked_iteration() -> None:
    def yield_fun()-> Iterable[bool]:
        yield not SWITCH_INSTANCE_NAME.is_on()

    def return_fun()-> bool:
        return not SWITCH_INSTANCE_NAME.is_on()
    

    if BINARY_SENSOR_INSTANCE_NAME.is_on() or app_state.INT_0 == 42:
        unchecked_send_email("test@test.com")
        SWITCH_INSTANCE_NAME.on()
    else:
        SWITCH_INSTANCE_NAME.off()

    a = app_state.INT_0 + 1
    if unchecked_return_int() == 42:
        b = [unchecked_return_int(), 2]
        g = [x for x in b]
    else:
        c = (lambda d: d + 1)(a)
    
    y = not (unchecked_return_int() == 31)
    d = {"a": SWITCH_INSTANCE_NAME.is_on()}
    {k: v for k, v in d.items()}

    s = set(SWITCH_INSTANCE_NAME.is_on())

    string = f"this is a beautiful string {SWITCH_INSTANCE_NAME.is_on()}"

    unchecked_send_email(addr="test")
    

    

def unchecked_send_email(addr: str) -> None:
    # do stuff
    a = 1+1
    
def unchecked_return_int() -> int:
    return 42
