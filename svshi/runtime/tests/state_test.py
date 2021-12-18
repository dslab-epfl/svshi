import pytest
from dataclasses import dataclass
from typing import Any, Callable, Coroutine, List, Optional, Tuple, Union
from pytest_mock import MockerFixture
from xknx.telegram.address import GroupAddress
from xknx.telegram.apci import APCI
from xknx.telegram.telegram import Telegram
from xknx.xknx import XKNX

from ..verification_file import PhysicalState

from ..app import App
from ..state import State


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


@pytest.mark.asyncio
async def test_state_listen(mocker: MockerFixture):
    addresses_listeners = {
        "1/1/1": [
            App("test1", "tests", mocker.stub(name="test1_code")),
            App("test2", "tests", mocker.stub(name="test2_code")),
        ],
        "1/1/2": [App("test3", "tests", mocker.stub(name="test3_code"))],
    }
    xknx_for_initialization = MockXKNX(MockTelegramQueue())
    xknx_for_listening = MockXKNX(MockTelegramQueue())
    state = State(addresses_listeners, xknx_for_initialization, xknx_for_listening)
    await state.listen()

    assert xknx_for_listening.listening == True


@pytest.mark.asyncio
async def test_state_stop(mocker: MockerFixture):
    addresses_listeners = {
        "1/1/1": [
            App("test1", "tests", mocker.stub(name="test1_code")),
            App("test2", "tests", mocker.stub(name="test2_code")),
        ],
        "1/1/2": [App("test3", "tests", mocker.stub(name="test3_code"))],
    }
    xknx_for_initialization = MockXKNX(MockTelegramQueue())
    xknx_for_listening = MockXKNX(MockTelegramQueue())
    state = State(addresses_listeners, xknx_for_initialization, xknx_for_listening)
    await state.listen()
    await state.stop()

    assert xknx_for_listening.listening == False


@pytest.mark.asyncio
async def test_state_initialize(mocker: MockerFixture):
    value_reader_return_value = 42.0
    mocker.patch(
        "xknx.core.value_reader.ValueReader.read",
        return_value=Telegram(
            GroupAddress("1/1/1"),
            payload=MockAPCI(MockAPCIValue(value_reader_return_value)),
        ),
    )
    addresses_listeners = {
        "1/1/1": [
            App("test1", "tests", mocker.stub(name="test1_code")),
            App("test2", "tests", mocker.stub(name="test2_code")),
        ],
        "1/1/2": [App("test3", "tests", mocker.stub(name="test3_code"))],
    }
    xknx_for_initialization = MockXKNX(MockTelegramQueue())
    xknx_for_listening = MockXKNX(MockTelegramQueue())
    state = State(addresses_listeners, xknx_for_initialization, xknx_for_listening)
    await state.initialize()

    assert state._physical_state != None
    assert state._physical_state.GA_1_1_1 == value_reader_return_value
    assert state._physical_state.GA_1_1_2 == value_reader_return_value


@pytest.mark.asyncio
async def test_state_on_telegram_update_state_and_notify(mocker: MockerFixture):
    test_one_called = [False]
    test_two_called = [False]
    test_three_called = [False]
    raw_value_set_called: List[Tuple[bool, Union[bool, float, None]]] = [(False, None)]

    def test_one_code(state: PhysicalState):
        test_one_called[0] = True

    def test_two_code(state: PhysicalState):
        test_two_called[0] = True

    def test_three_code(state: PhysicalState):
        test_three_called[0] = True

    async def raw_value_set(self, value: Union[bool, float]):
        raw_value_set_called[0] = (True, value)

    value_reader_return_value = 42.0
    mocker.patch(
        "xknx.core.value_reader.ValueReader.read",
        return_value=Telegram(
            GroupAddress("1/1/1"),
            payload=MockAPCI(MockAPCIValue(value_reader_return_value)),
        ),
    )
    mocker.patch("xknx.devices.RawValue.set", new=raw_value_set)
    addresses_listeners = {
        "1/1/1": [
            App("test1", "tests", test_one_code),
            App("test2", "tests", test_two_code),
        ],
        "1/1/2": [App("test3", "tests", test_three_code)],
    }
    xknx_for_initialization = MockXKNX(MockTelegramQueue())
    xknx_for_listening = MockXKNX(MockTelegramQueue())
    state = State(addresses_listeners, xknx_for_initialization, xknx_for_listening)
    await state.initialize()

    await xknx_for_listening.telegram_queue.receive_telegram(
        Telegram(GroupAddress("1/1/1"), payload=MockAPCI(MockAPCIValue(2.3)))
    )

    assert state._physical_state.GA_1_1_1 == 2.3
    assert state._physical_state.GA_1_1_2 == value_reader_return_value
    assert test_one_called[0] == True
    assert test_two_called[0] == True
    assert test_three_called[0] == False
    assert raw_value_set_called[0] == (False, None)


@pytest.mark.asyncio
async def test_state_on_telegram_update_state_and_notify_and_update_again_and_notify_and_send_to_knx(
    mocker: MockerFixture,
):
    test_one_called = [False]
    test_two_called = [False]
    test_three_called = [False]
    raw_value_set_called: List[Tuple[bool, Union[bool, float, None]]] = [(False, None)]

    def test_one_code(state: PhysicalState):
        test_one_called[0] = True

    def test_two_code(state: PhysicalState):
        test_two_called[0] = True
        state.GA_1_1_2 = 8.1

    def test_three_code(state: PhysicalState):
        test_three_called[0] = True

    async def raw_value_set(self, value: Union[bool, float]):
        raw_value_set_called[0] = (True, value)

    value_reader_return_value = 42.0
    mocker.patch(
        "xknx.core.value_reader.ValueReader.read",
        return_value=Telegram(
            GroupAddress("1/1/1"),
            payload=MockAPCI(MockAPCIValue(value_reader_return_value)),
        ),
    )
    mocker.patch("xknx.devices.RawValue.set", new=raw_value_set)
    addresses_listeners = {
        "1/1/1": [
            App("test1", "tests", test_one_code),
            App("test2", "tests", test_two_code),
        ],
        "1/1/2": [App("test3", "tests", test_three_code)],
    }
    xknx_for_initialization = MockXKNX(MockTelegramQueue())
    xknx_for_listening = MockXKNX(MockTelegramQueue())
    state = State(addresses_listeners, xknx_for_initialization, xknx_for_listening)
    await state.initialize()

    await xknx_for_listening.telegram_queue.receive_telegram(
        Telegram(GroupAddress("1/1/1"), payload=MockAPCI(MockAPCIValue(2.3)))
    )

    assert state._physical_state.GA_1_1_1 == 2.3
    assert state._physical_state.GA_1_1_2 == 8.1
    assert test_one_called[0] == True
    assert test_two_called[0] == True
    assert test_three_called[0] == True
    assert raw_value_set_called[0] == (True, 8)
