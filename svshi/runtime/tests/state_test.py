import asyncio
import pytest
from pytest_mock import MockerFixture
from dataclasses import dataclass
from typing import Any, Callable, Coroutine, Dict, Optional, Union, cast
from xknx.dpt.dpt import DPTArray, DPTBase, DPTBinary
from xknx.dpt.dpt_2byte_float import DPT2ByteFloat
from xknx.telegram.address import GroupAddress
from xknx.telegram.apci import GroupValueWrite
from xknx.telegram.telegram import Telegram
from xknx.xknx import XKNX

from ..verification_file import PhysicalState
from ..app import App
from ..state import State

VALUE_READER_RETURN_VALUE = 42.0
VALUE_READER_RAW_RETURN_VALUE = DPT2ByteFloat.to_knx(VALUE_READER_RETURN_VALUE)

RECEIVED_VALUE = 2.29
RECEIVED_RAW_VALUE = DPT2ByteFloat.to_knx(RECEIVED_VALUE)

FIRST_GROUP_ADDRESS = "1/1/1"
SECOND_GROUP_ADDRESS = "1/1/2"
THIRD_GROUP_ADDRESS = "1/1/3"
FOURTH_GROUP_ADDRESS = "1/1/4"


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
    value: tuple[int, ...]


@dataclass
class MockGroupValueWrite(GroupValueWrite):
    value: Union[MockAPCIValue, DPTBinary]


class StateHolder:
    def __init__(self):
        self.app_one_called = False
        self.app_two_called = False
        self.app_three_called = False
        self.app_four_called = False

        self.app_one = App("test1", "tests", self.app_one_code)
        self.app_two = App("test2", "tests", self.app_two_code)
        self.app_three = App("test3", "tests", self.app_three_code)
        self.app_four = App("test4", "tests", self.app_four_code, timer=2)
        self.addresses_listeners = {
            FIRST_GROUP_ADDRESS: [
                self.app_one,
                self.app_two,
            ],
            SECOND_GROUP_ADDRESS: [self.app_three],
            THIRD_GROUP_ADDRESS: [self.app_three],
            FOURTH_GROUP_ADDRESS: [self.app_four],
        }

        self.group_address_to_dpt: Dict[str, Union[DPTBase, DPTBinary]] = {
            FIRST_GROUP_ADDRESS: DPT2ByteFloat(),
            SECOND_GROUP_ADDRESS: DPT2ByteFloat(),
            THIRD_GROUP_ADDRESS: DPTBinary(0),
            FOURTH_GROUP_ADDRESS: DPTBinary(0),
        }

        self.xknx_for_initialization = MockXKNX(MockTelegramQueue())
        self.xknx_for_listening = MockXKNX(MockTelegramQueue())

    def app_one_code(self, state: PhysicalState):
        self.app_one_called = True

    def app_two_code(self, state: PhysicalState):
        self.app_two_called = True

    def app_three_code(self, state: PhysicalState):
        self.app_three_called = True

    def app_four_code(self, state: PhysicalState):
        self.app_four_called = True

    def reset(self):
        self = self.__init__()

    def set_app_two_code(self, code):
        self.app_two.code = code


test_state_holder = StateHolder()


@pytest.fixture(autouse=True)
def run_before_and_after_tests(mocker: MockerFixture):
    """Fixture to execute setup and cleanup"""
    # Setup
    test_state_holder.reset()

    first_address_write_telegram = Telegram(
        GroupAddress(FIRST_GROUP_ADDRESS),
        payload=MockGroupValueWrite(MockAPCIValue(VALUE_READER_RAW_RETURN_VALUE)),
    )
    second_address_write_telegram = Telegram(
        GroupAddress(SECOND_GROUP_ADDRESS),
        payload=MockGroupValueWrite(MockAPCIValue(VALUE_READER_RAW_RETURN_VALUE)),
    )
    third_address_write_telegram = Telegram(
        GroupAddress(THIRD_GROUP_ADDRESS),
        payload=MockGroupValueWrite(DPTBinary(0)),
    )
    fourth_address_write_telegram = Telegram(
        GroupAddress(THIRD_GROUP_ADDRESS),
        payload=MockGroupValueWrite(DPTBinary(1)),
    )

    mocker.patch(
        "xknx.core.value_reader.ValueReader.read",
        side_effect=[
            first_address_write_telegram,
            second_address_write_telegram,
            third_address_write_telegram,
            fourth_address_write_telegram,
        ],
    )

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
        test_state_holder.group_address_to_dpt,
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
        test_state_holder.group_address_to_dpt,
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
        test_state_holder.group_address_to_dpt,
    )
    await state.initialize()

    assert state._physical_state != None
    assert state._physical_state.GA_1_1_1 == VALUE_READER_RETURN_VALUE
    assert state._physical_state.GA_1_1_2 == VALUE_READER_RETURN_VALUE
    assert state._physical_state.GA_1_1_3 == False
    assert state._physical_state.GA_1_1_4 == True


@pytest.mark.asyncio
async def test_state_periodic_apps_are_run():
    state = State(
        test_state_holder.addresses_listeners,
        test_state_holder.xknx_for_initialization,
        test_state_holder.xknx_for_listening,
        always_valid_conditions,
        test_state_holder.group_address_to_dpt,
    )
    await state.initialize()

    assert state._physical_state != None
    assert state._physical_state.GA_1_1_1 == VALUE_READER_RETURN_VALUE
    assert state._physical_state.GA_1_1_2 == VALUE_READER_RETURN_VALUE
    assert state._physical_state.GA_1_1_3 == False
    assert state._physical_state.GA_1_1_4 == True

    # We wait just to make sure the periodic app was called
    await asyncio.sleep(3)

    assert test_state_holder.app_four_called == True


@pytest.mark.asyncio
async def test_state_on_telegram_update_state_and_notify():
    state = State(
        test_state_holder.addresses_listeners,
        test_state_holder.xknx_for_initialization,
        test_state_holder.xknx_for_listening,
        always_valid_conditions,
        test_state_holder.group_address_to_dpt,
    )
    await state.initialize()

    await test_state_holder.xknx_for_listening.telegram_queue.receive_telegram(
        Telegram(
            GroupAddress(FIRST_GROUP_ADDRESS),
            payload=MockGroupValueWrite(MockAPCIValue(RECEIVED_RAW_VALUE)),
        )
    )

    await test_state_holder.xknx_for_listening.telegram_queue.receive_telegram(
        Telegram(
            GroupAddress(THIRD_GROUP_ADDRESS),
            payload=MockGroupValueWrite(DPTBinary(1)),
        )
    )

    assert state._physical_state.GA_1_1_1 == RECEIVED_VALUE
    assert state._physical_state.GA_1_1_2 == VALUE_READER_RETURN_VALUE
    assert state._physical_state.GA_1_1_3 == True
    assert state._physical_state.GA_1_1_4 == True
    assert test_state_holder.app_one_called == True
    assert test_state_holder.app_two_called == True
    assert test_state_holder.app_three_called == True
    assert test_state_holder.app_four_called == False
    assert test_state_holder.xknx_for_listening.telegrams.empty() == True
    assert test_state_holder.app_one.should_run == True
    assert test_state_holder.app_two.should_run == True
    assert test_state_holder.app_three.should_run == True
    assert test_state_holder.app_four.should_run == True


@pytest.mark.asyncio
async def test_state_on_telegram_update_state_and_notify_and_stop_app_violating_conditions():
    state = State(
        test_state_holder.addresses_listeners,
        test_state_holder.xknx_for_initialization,
        test_state_holder.xknx_for_listening,
        always_invalid_conditions,
        test_state_holder.group_address_to_dpt,
    )
    await state.initialize()

    await test_state_holder.xknx_for_listening.telegram_queue.receive_telegram(
        Telegram(
            GroupAddress(FIRST_GROUP_ADDRESS),
            payload=MockGroupValueWrite(MockAPCIValue(RECEIVED_RAW_VALUE)),
        )
    )

    assert state._physical_state.GA_1_1_1 == RECEIVED_VALUE
    assert state._physical_state.GA_1_1_2 == VALUE_READER_RETURN_VALUE
    assert state._physical_state.GA_1_1_3 == False
    assert state._physical_state.GA_1_1_4 == True
    assert test_state_holder.app_one_called == True
    assert test_state_holder.app_two_called == True
    assert test_state_holder.app_three_called == False
    assert test_state_holder.app_four_called == False
    assert test_state_holder.xknx_for_listening.telegrams.empty() == True
    assert test_state_holder.app_one.should_run == False
    assert test_state_holder.app_two.should_run == False
    assert test_state_holder.app_three.should_run == True
    assert test_state_holder.app_four.should_run == True


@pytest.mark.asyncio
async def test_state_on_telegram_update_state_and_notify_and_update_again_and_notify_and_send_to_knx():
    new_second_address_value = 8.1
    new_third_address_value = True

    def test_two_code(state: PhysicalState):
        test_state_holder.app_two_called = True
        state.GA_1_1_2 = new_second_address_value
        state.GA_1_1_3 = new_third_address_value

    test_state_holder.set_app_two_code(test_two_code)

    state = State(
        test_state_holder.addresses_listeners,
        test_state_holder.xknx_for_initialization,
        test_state_holder.xknx_for_listening,
        always_valid_conditions,
        test_state_holder.group_address_to_dpt,
    )
    await state.initialize()

    await test_state_holder.xknx_for_listening.telegram_queue.receive_telegram(
        Telegram(
            GroupAddress(FIRST_GROUP_ADDRESS),
            payload=MockGroupValueWrite(MockAPCIValue(RECEIVED_RAW_VALUE)),
        )
    )

    assert state._physical_state.GA_1_1_1 == RECEIVED_VALUE
    assert state._physical_state.GA_1_1_2 == new_second_address_value
    assert state._physical_state.GA_1_1_3 == new_third_address_value
    assert state._physical_state.GA_1_1_4 == True
    assert test_state_holder.app_one_called == True
    assert test_state_holder.app_two_called == True
    assert test_state_holder.app_three_called == True
    assert test_state_holder.app_four_called == False
    assert test_state_holder.xknx_for_listening.telegrams.empty() == False
    assert await test_state_holder.xknx_for_listening.telegrams.get() == Telegram(
        destination_address=GroupAddress(SECOND_GROUP_ADDRESS),
        payload=GroupValueWrite(
            DPTArray(
                cast(
                    DPT2ByteFloat,
                    test_state_holder.group_address_to_dpt[SECOND_GROUP_ADDRESS],
                ).to_knx(new_second_address_value)
            )
        ),
    )
    assert await test_state_holder.xknx_for_listening.telegrams.get() == Telegram(
        destination_address=GroupAddress(THIRD_GROUP_ADDRESS),
        payload=GroupValueWrite(DPTBinary(1)),
    )
    assert test_state_holder.app_one.should_run == True
    assert test_state_holder.app_two.should_run == True
    assert test_state_holder.app_three.should_run == True
    assert test_state_holder.app_four.should_run == True
