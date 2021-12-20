import pytest
from pytest_mock import MockerFixture
from dataclasses import dataclass
from typing import Any, Callable, Coroutine, Optional, Tuple, Union
from xknx.telegram.address import GroupAddress
from xknx.telegram.apci import APCI
from xknx.telegram.telegram import Telegram
from xknx.xknx import XKNX

from ..verification_file import PhysicalState
from ..app import App
from ..state import State


def always_valid_conditions(state: PhysicalState) -> bool:
    return True


def always_invalid_conditions(state: PhysicalState) -> bool:
    return False


class MockTelegramQueue:
    def __init__(self):
        self.telegram_received_cb: Optional[
            Callable[[Telegram], Coroutine[Any, Any, None]]
        ] = None

    def register_telegram_received_cb(
        self, cb: Callable[[Telegram], Coroutine[Any, Any, None]]
    ):
        self.telegram_received_cb = cb

    async def receive_telegram(self, telegram: Telegram):
        if self.telegram_received_cb:
            await self.telegram_received_cb(telegram)


class MockXKNX(XKNX):
    def __init__(self, telegram_queue: MockTelegramQueue) -> None:
        super().__init__()
        self.telegram_queue = telegram_queue
        self.listening = False

    async def start(self):
        self.listening = True

    async def stop(self):
        self.listening = False


@dataclass
class MockAPCIValue:
    value: float


@dataclass
class MockAPCI(APCI):
    value: MockAPCIValue

    def calculated_length(self) -> int:
        return 1

    def from_knx(self, raw: bytes) -> None:
        pass

    def to_knx(self) -> bytes:
        return bytes()


class StateHolder:
    def __init__(self):
        self.app_one_called = False
        self.app_two_called = False
        self.app_three_called = False
        self.raw_value_set_called: Tuple[bool, Union[bool, float, None]] = (False, None)

        self.app_one = App("test1", "tests", self.app_one_code)
        self.app_two = App("test2", "tests", self.app_two_code)
        self.app_three = App("test3", "tests", self.app_three_code)
        self.addresses_listeners = {
            "1/1/1": [
                self.app_one,
                self.app_two,
            ],
            "1/1/2": [self.app_three],
        }

        self.xknx_for_initialization = MockXKNX(MockTelegramQueue())
        self.xknx_for_listening = MockXKNX(MockTelegramQueue())

    def app_one_code(self, state: PhysicalState):
        self.app_one_called = True

    def app_two_code(self, state: PhysicalState):
        self.app_two_called = True

    def app_three_code(self, state: PhysicalState):
        self.app_three_called = True

    async def raw_value_set(self, value: Union[bool, float]):
        self.raw_value_set_called = (True, value)

    def reset(self):
        self = self.__init__()

    def set_app_two_code(self, code):
        self.app_two.code = code


test_state_holder = StateHolder()
VALUE_READER_RETURN_VALUE = 42.0


@pytest.fixture(autouse=True)
def run_before_and_after_tests(mocker: MockerFixture):
    """Fixture to execute setup and cleanup"""
    # Setup
    test_state_holder.reset()

    mocker.patch(
        "xknx.core.value_reader.ValueReader.read",
        return_value=Telegram(
            GroupAddress("1/1/1"),
            payload=MockAPCI(MockAPCIValue(VALUE_READER_RETURN_VALUE)),
        ),
    )
    mocker.patch("xknx.devices.RawValue.set", new=test_state_holder.raw_value_set)

    yield  # this is where the testing happens

    # Cleanup
    test_state_holder.reset()


@pytest.mark.asyncio
async def test_state_listen():
    state = State(
        test_state_holder.addresses_listeners,
        test_state_holder.xknx_for_initialization,
        test_state_holder.xknx_for_listening,
        always_valid_conditions,
    )
    await state.listen()

    assert test_state_holder.xknx_for_listening.listening == True


@pytest.mark.asyncio
async def test_state_stop():
    state = State(
        test_state_holder.addresses_listeners,
        test_state_holder.xknx_for_initialization,
        test_state_holder.xknx_for_listening,
        always_valid_conditions,
    )
    await state.listen()
    await state.stop()

    assert test_state_holder.xknx_for_listening.listening == False


@pytest.mark.asyncio
async def test_state_initialize():
    state = State(
        test_state_holder.addresses_listeners,
        test_state_holder.xknx_for_initialization,
        test_state_holder.xknx_for_listening,
        always_valid_conditions,
    )
    await state.initialize()

    assert state._physical_state != None
    assert state._physical_state.GA_1_1_1 == VALUE_READER_RETURN_VALUE
    assert state._physical_state.GA_1_1_2 == VALUE_READER_RETURN_VALUE


@pytest.mark.asyncio
async def test_state_on_telegram_update_state_and_notify():
    state = State(
        test_state_holder.addresses_listeners,
        test_state_holder.xknx_for_initialization,
        test_state_holder.xknx_for_listening,
        always_valid_conditions,
    )
    await state.initialize()

    await test_state_holder.xknx_for_listening.telegram_queue.receive_telegram(
        Telegram(GroupAddress("1/1/1"), payload=MockAPCI(MockAPCIValue(2.3)))
    )

    assert state._physical_state.GA_1_1_1 == 2.3
    assert state._physical_state.GA_1_1_2 == VALUE_READER_RETURN_VALUE
    assert test_state_holder.app_one_called == True
    assert test_state_holder.app_two_called == True
    assert test_state_holder.app_three_called == False
    assert test_state_holder.raw_value_set_called == (False, None)
    assert test_state_holder.app_one.should_run == True
    assert test_state_holder.app_two.should_run == True
    assert test_state_holder.app_three.should_run == True


@pytest.mark.asyncio
async def test_state_on_telegram_update_state_and_notify_and_stop_app_violating_conditions():
    state = State(
        test_state_holder.addresses_listeners,
        test_state_holder.xknx_for_initialization,
        test_state_holder.xknx_for_listening,
        always_invalid_conditions,
    )
    await state.initialize()

    await test_state_holder.xknx_for_listening.telegram_queue.receive_telegram(
        Telegram(GroupAddress("1/1/1"), payload=MockAPCI(MockAPCIValue(2.3)))
    )

    assert state._physical_state.GA_1_1_1 == 2.3
    assert state._physical_state.GA_1_1_2 == VALUE_READER_RETURN_VALUE
    assert test_state_holder.app_one_called == True
    assert test_state_holder.app_two_called == True
    assert test_state_holder.app_three_called == False
    assert test_state_holder.raw_value_set_called == (False, None)
    assert test_state_holder.app_one.should_run == False
    assert test_state_holder.app_two.should_run == False
    assert test_state_holder.app_three.should_run == True


@pytest.mark.asyncio
async def test_state_on_telegram_update_state_and_notify_and_update_again_and_notify_and_send_to_knx():
    def test_two_code(state: PhysicalState):
        test_state_holder.app_two_called = True
        state.GA_1_1_2 = 8.1

    test_state_holder.set_app_two_code(test_two_code)

    state = State(
        test_state_holder.addresses_listeners,
        test_state_holder.xknx_for_initialization,
        test_state_holder.xknx_for_listening,
        always_valid_conditions,
    )
    await state.initialize()

    await test_state_holder.xknx_for_listening.telegram_queue.receive_telegram(
        Telegram(GroupAddress("1/1/1"), payload=MockAPCI(MockAPCIValue(2.3)))
    )

    assert state._physical_state.GA_1_1_1 == 2.3
    assert state._physical_state.GA_1_1_2 == 8.1
    assert test_state_holder.app_one_called == True
    assert test_state_holder.app_two_called == True
    assert test_state_holder.app_three_called == True
    assert test_state_holder.raw_value_set_called == (True, 8)
    assert test_state_holder.app_one.should_run == True
    assert test_state_holder.app_two.should_run == True
    assert test_state_holder.app_three.should_run == True
