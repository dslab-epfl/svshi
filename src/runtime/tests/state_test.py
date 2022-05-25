import asyncio
import dataclasses
import shutil
import os
import sys
import time

from pyparsing import line
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

from ..verification_file import AppState, PhysicalState
from ..runtime_file import InternalState
from ..app import App
from ..state import State

LOGS_DIR = "tests/logs"

VALUE_READER_RETURN_VALUE = 42.0
VALUE_READER_RAW_RETURN_VALUE = DPT2ByteFloat.to_knx(VALUE_READER_RETURN_VALUE)

RECEIVED_VALUE = 2.29
RECEIVED_RAW_VALUE = DPT2ByteFloat.to_knx(RECEIVED_VALUE)

FIRST_GROUP_ADDRESS = "1/1/1"
SECOND_GROUP_ADDRESS = "1/1/2"
THIRD_GROUP_ADDRESS = "1/1/3"
FOURTH_GROUP_ADDRESS = "1/1/4"

FIRST_APP_NAME = "test1"
SECOND_APP_NAME = "test2"
THIRD_APP_NAME = "test3"
FOURTH_APP_NAME = "test4"


def always_valid_conditions(
    test1_app_state: AppState,
    test2_app_state: AppState,
    test3_app_state: AppState,
    test4_app_state: AppState,
    physical_state: PhysicalState,
    internal_state: InternalState,
) -> bool:
    return True


def always_invalid_conditions(
    test1_app_state: AppState,
    test2_app_state: AppState,
    test3_app_state: AppState,
    test4_app_state: AppState,
    physical_state: PhysicalState,
    internal_state: InternalState,
) -> bool:
    return False


def conditions(
    test1_app_state: AppState,
    test2_app_state: AppState,
    test3_app_state: AppState,
    test4_app_state: AppState,
    physical_state: PhysicalState,
    internal_state: InternalState,
) -> bool:
    return physical_state.GA_1_1_2 != 8


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

        self.app_one = App(FIRST_APP_NAME, "tests", self.app_one_code)
        self.app_two = App(SECOND_APP_NAME, "tests", self.app_two_code)
        self.app_three = App(THIRD_APP_NAME, "tests", self.app_three_code)
        self.app_four = App(FOURTH_APP_NAME, "tests", self.app_four_code, timer=2)
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

    def app_one_code(self, state1: AppState, state2: PhysicalState, internal_state: InternalState):
        self.app_one_called = True

    def app_two_code(self, state1: AppState, state: PhysicalState, internal_state: InternalState):
        self.app_two_called = True

    def app_three_code(self, state1: AppState, state: PhysicalState, internal_state: InternalState):
        self.app_three_called = True

    def app_four_code(self, state1: AppState, state: PhysicalState, internal_state: InternalState):
        self.app_four_called = True

    def reset(self):
        self = self.__init__()

    def reset_apps_called(self):
        self.app_one_called = False
        self.app_two_called = False
        self.app_three_called = False
        self.app_four_called = False

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
    shutil.rmtree(LOGS_DIR)


@pytest.mark.asyncio
async def test_state_listen():
    state = State(
        test_state_holder.addresses_listeners,
        test_state_holder.xknx_for_initialization,
        test_state_holder.xknx_for_listening,
        always_valid_conditions,
        test_state_holder.group_address_to_dpt,
        LOGS_DIR,
    )
    await state.listen()

    assert test_state_holder.xknx_for_listening.listening == True

    # Cleanup
    await state.stop()


@pytest.mark.asyncio
async def test_state_stop():
    state = State(
        test_state_holder.addresses_listeners,
        test_state_holder.xknx_for_initialization,
        test_state_holder.xknx_for_listening,
        always_valid_conditions,
        test_state_holder.group_address_to_dpt,
        LOGS_DIR,
    )
    await state.initialize()
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
        LOGS_DIR,
    )
    await state.initialize()

    assert state._physical_state != None
    assert state._physical_state.GA_1_1_1 == VALUE_READER_RETURN_VALUE
    assert state._physical_state.GA_1_1_2 == VALUE_READER_RETURN_VALUE
    assert state._physical_state.GA_1_1_3 == False
    assert state._physical_state.GA_1_1_4 == True
    assert state._app_states[FIRST_APP_NAME] == AppState()
    assert state._app_states[SECOND_APP_NAME] == AppState()
    assert state._app_states[THIRD_APP_NAME] == AppState()
    assert state._app_states[FOURTH_APP_NAME] == AppState()

    # Cleanup
    await state.stop()


@pytest.mark.asyncio
async def test_internal_state_is_updated():
    state = State(
        test_state_holder.addresses_listeners,
        test_state_holder.xknx_for_initialization,
        test_state_holder.xknx_for_listening,
        always_valid_conditions,
        test_state_holder.group_address_to_dpt,
        LOGS_DIR,
    )
    NEW_SECOND_ADRESS_VALUE = 8.1
    NEW_THIRD_ADDRESS_VALUE = True
    start_test_time = time.time()

    def test_two_code(app_state: AppState, physical_state: PhysicalState, internal_state: InternalState):
        test_state_holder.app_two_called = True

        if start_test_time - 5 <= time.mktime(internal_state.date_time):
            physical_state.GA_1_1_2 = NEW_SECOND_ADRESS_VALUE
            physical_state.GA_1_1_3 = NEW_THIRD_ADDRESS_VALUE


    test_state_holder.set_app_two_code(test_two_code)
    await state.initialize()

    await test_state_holder.xknx_for_listening.telegram_queue.receive_telegram(
        Telegram(
            GroupAddress(FIRST_GROUP_ADDRESS),
            payload=MockGroupValueWrite(MockAPCIValue(RECEIVED_RAW_VALUE)),
        )
    )

    assert state._physical_state.GA_1_1_1 == RECEIVED_VALUE
    assert state._physical_state.GA_1_1_2 == NEW_SECOND_ADRESS_VALUE
    assert state._physical_state.GA_1_1_3 == NEW_THIRD_ADDRESS_VALUE
    assert state._physical_state.GA_1_1_4 == True
    assert state._physical_state == state._last_valid_physical_state
    assert start_test_time - 3 <= time.mktime(state._internal_state.date_time) <= start_test_time + 3
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
                ).to_knx(NEW_SECOND_ADRESS_VALUE)
            )
        ),
    )
    assert await test_state_holder.xknx_for_listening.telegrams.get() == Telegram(
        destination_address=GroupAddress(THIRD_GROUP_ADDRESS),
        payload=GroupValueWrite(DPTBinary(1)),
    )
    await asyncio.sleep(5)
    current_test_time = time.time()
    assert test_state_holder.app_four_called == True
    assert current_test_time - 3 <= time.mktime(state._internal_state.date_time) <= current_test_time + 3
    assert test_state_holder.app_one.should_run == True
    assert test_state_holder.app_two.should_run == True
    assert test_state_holder.app_three.should_run == True
    assert test_state_holder.app_four.should_run == True
    assert state._app_states[FIRST_APP_NAME] == AppState()
    assert state._app_states[SECOND_APP_NAME] == AppState()
    assert state._app_states[THIRD_APP_NAME] == AppState()
    assert state._app_states[FOURTH_APP_NAME] == AppState()

    # Cleanup
    await state.stop()
@pytest.mark.asyncio
async def test_state_periodic_apps_are_run():
    state = State(
        test_state_holder.addresses_listeners,
        test_state_holder.xknx_for_initialization,
        test_state_holder.xknx_for_listening,
        always_valid_conditions,
        test_state_holder.group_address_to_dpt,
        LOGS_DIR,
    )
    await state.initialize()


    assert state._physical_state != None
    assert state._physical_state.GA_1_1_1 == VALUE_READER_RETURN_VALUE
    assert state._physical_state.GA_1_1_2 == VALUE_READER_RETURN_VALUE
    assert state._physical_state.GA_1_1_3 == False
    assert state._physical_state.GA_1_1_4 == True
    assert state._app_states[FIRST_APP_NAME] == AppState()
    assert state._app_states[SECOND_APP_NAME] == AppState()
    assert state._app_states[THIRD_APP_NAME] == AppState()
    assert state._app_states[FOURTH_APP_NAME] == AppState()

    # We wait just to make sure the periodic app was called
    await asyncio.sleep(3)

    assert test_state_holder.app_four_called == True
    assert state._app_states[FIRST_APP_NAME] == AppState()
    assert state._app_states[SECOND_APP_NAME] == AppState()
    assert state._app_states[THIRD_APP_NAME] == AppState()
    assert state._app_states[FOURTH_APP_NAME] == AppState()
    assert state._physical_state == state._last_valid_physical_state

    # Cleanup
    await state.stop()



@pytest.mark.asyncio
async def test_state_on_telegram_update_state_and_notify():
    state = State(
        test_state_holder.addresses_listeners,
        test_state_holder.xknx_for_initialization,
        test_state_holder.xknx_for_listening,
        always_valid_conditions,
        test_state_holder.group_address_to_dpt,
        LOGS_DIR,
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
    assert state._physical_state == state._last_valid_physical_state
    assert test_state_holder.app_one_called == True
    assert test_state_holder.app_two_called == True
    assert test_state_holder.app_three_called == True
    assert test_state_holder.app_four_called == False
    assert test_state_holder.xknx_for_listening.telegrams.empty() == True
    assert test_state_holder.app_one.should_run == True
    assert test_state_holder.app_two.should_run == True
    assert test_state_holder.app_three.should_run == True
    assert test_state_holder.app_four.should_run == True
    assert state._app_states[FIRST_APP_NAME] == AppState()
    assert state._app_states[SECOND_APP_NAME] == AppState()
    assert state._app_states[THIRD_APP_NAME] == AppState()
    assert state._app_states[FOURTH_APP_NAME] == AppState()

    # Cleanup
    await state.stop()


@pytest.mark.asyncio
async def test_state_on_telegram_update_state_makes_it_invalid_merged_state_invalid_then_runtime_stops_and_raises_interrupt(
    mocker: MockerFixture,
):
    with pytest.raises(KeyboardInterrupt):
        state = State(
            test_state_holder.addresses_listeners,
            test_state_holder.xknx_for_initialization,
            test_state_holder.xknx_for_listening,
            conditions,
            test_state_holder.group_address_to_dpt,
            LOGS_DIR,
        )
        state_stop_spy = mocker.spy(state, "stop")
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
        assert state._physical_state == state._last_valid_physical_state
        assert test_state_holder.app_one_called == True
        assert test_state_holder.app_two_called == True
        assert test_state_holder.app_three_called == False
        assert test_state_holder.app_four_called == False
        assert test_state_holder.xknx_for_listening.telegrams.empty() == True
        assert test_state_holder.app_one.should_run == True
        assert test_state_holder.app_two.should_run == True
        assert test_state_holder.app_three.should_run == True
        assert test_state_holder.app_four.should_run == True
        assert state._app_states[FIRST_APP_NAME] == AppState()
        assert state._app_states[SECOND_APP_NAME] == AppState()
        assert state._app_states[THIRD_APP_NAME] == AppState()
        assert state._app_states[FOURTH_APP_NAME] == AppState()

        last_valid_state = dataclasses.replace(state._physical_state)

        test_state_holder.reset_apps_called()

        await test_state_holder.xknx_for_listening.telegram_queue.receive_telegram(
            Telegram(
                GroupAddress(SECOND_GROUP_ADDRESS),
                payload=MockGroupValueWrite(MockAPCIValue(DPT2ByteFloat.to_knx(8))),
            )
        )

        assert state._physical_state.GA_1_1_1 == RECEIVED_VALUE
        assert state._physical_state.GA_1_1_2 == 8
        assert state._physical_state.GA_1_1_3 == False
        assert state._physical_state.GA_1_1_4 == True
        # The last valid state has not changed
        assert state._last_valid_physical_state == last_valid_state
        assert test_state_holder.app_one_called == False
        assert test_state_holder.app_two_called == False
        assert test_state_holder.app_three_called == True
        assert test_state_holder.app_four_called == False
        # We sent the last valid state to KNX
        assert test_state_holder.xknx_for_listening.telegrams.empty() == False
        assert await test_state_holder.xknx_for_listening.telegrams.get() == Telegram(
            destination_address=GroupAddress(FIRST_GROUP_ADDRESS),
            payload=GroupValueWrite(
                DPTArray(
                    cast(
                        DPT2ByteFloat,
                        test_state_holder.group_address_to_dpt[FIRST_GROUP_ADDRESS],
                    ).to_knx(RECEIVED_VALUE)
                )
            ),
        )
        assert await test_state_holder.xknx_for_listening.telegrams.get() == Telegram(
            destination_address=GroupAddress(SECOND_GROUP_ADDRESS),
            payload=GroupValueWrite(
                DPTArray(
                    cast(
                        DPT2ByteFloat,
                        test_state_holder.group_address_to_dpt[SECOND_GROUP_ADDRESS],
                    ).to_knx(VALUE_READER_RETURN_VALUE)
                )
            ),
        )
        assert await test_state_holder.xknx_for_listening.telegrams.get() == Telegram(
            destination_address=GroupAddress(THIRD_GROUP_ADDRESS),
            payload=GroupValueWrite(DPTBinary(0)),
        )
        assert await test_state_holder.xknx_for_listening.telegrams.get() == Telegram(
            destination_address=GroupAddress(FOURTH_GROUP_ADDRESS),
            payload=GroupValueWrite(DPTBinary(1)),
        )
        # The apps are stopped
        assert test_state_holder.app_one.should_run == False
        assert test_state_holder.app_two.should_run == False
        assert test_state_holder.app_three.should_run == False
        assert test_state_holder.app_four.should_run == False
        assert state._app_states[FIRST_APP_NAME] == AppState()
        assert state._app_states[SECOND_APP_NAME] == AppState()
        assert state._app_states[THIRD_APP_NAME] == AppState()
        assert state._app_states[FOURTH_APP_NAME] == AppState()
        # Stop is called
        state_stop_spy.assert_called_once()


@pytest.mark.asyncio
async def test_state_on_telegram_update_state_and_notify_and_stop_app_violating_conditions():
    def app_two_code_that_violates_conditions(
        app_state: AppState, physical_state: PhysicalState, internal_state: InternalState
    ):
        test_state_holder.app_two_called = True
        physical_state.GA_1_1_2 = 8

    test_state_holder.set_app_two_code(app_two_code_that_violates_conditions)
    state = State(
        test_state_holder.addresses_listeners,
        test_state_holder.xknx_for_initialization,
        test_state_holder.xknx_for_listening,
        conditions,
        test_state_holder.group_address_to_dpt,
        LOGS_DIR,
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
    assert test_state_holder.app_one.should_run == True
    assert test_state_holder.app_two.should_run == False
    assert test_state_holder.app_three.should_run == True
    assert test_state_holder.app_four.should_run == True
    assert state._app_states[FIRST_APP_NAME] == AppState()
    assert state._app_states[SECOND_APP_NAME] == AppState()
    assert state._app_states[THIRD_APP_NAME] == AppState()
    assert state._app_states[FOURTH_APP_NAME] == AppState()

    # Cleanup
    await state.stop()


@pytest.mark.asyncio
async def test_state_on_telegram_update_state_and_notify_and_update_again_and_notify_and_send_to_knx():
    new_second_address_value = 8.1
    new_third_address_value = True

    def test_two_code(app_state: AppState, physical_state: PhysicalState, internal_state: InternalState):
        test_state_holder.app_two_called = True
        physical_state.GA_1_1_2 = new_second_address_value
        physical_state.GA_1_1_3 = new_third_address_value

    test_state_holder.set_app_two_code(test_two_code)

    state = State(
        test_state_holder.addresses_listeners,
        test_state_holder.xknx_for_initialization,
        test_state_holder.xknx_for_listening,
        always_valid_conditions,
        test_state_holder.group_address_to_dpt,
        LOGS_DIR,
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
    assert state._physical_state == state._last_valid_physical_state
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
    assert state._app_states[FIRST_APP_NAME] == AppState()
    assert state._app_states[SECOND_APP_NAME] == AppState()
    assert state._app_states[THIRD_APP_NAME] == AppState()
    assert state._app_states[FOURTH_APP_NAME] == AppState()

    # Cleanup
    await state.stop()


@pytest.mark.asyncio
async def test_state_update_app_state():
    new_int_0_value = 42
    new_float_1_value = 42.42
    new_bool_2_value = True
    new_str_3_value = "the answer to everything is 42"

    def test_two_code(app_state: AppState, physical_state: PhysicalState, internal_state: InternalState):
        test_state_holder.app_two_called = True
        app_state.INT_0 = new_int_0_value
        app_state.FLOAT_1 = new_float_1_value
        app_state.BOOL_2 = new_bool_2_value
        app_state.STR_3 = new_str_3_value

    test_state_holder.set_app_two_code(test_two_code)

    state = State(
        test_state_holder.addresses_listeners,
        test_state_holder.xknx_for_initialization,
        test_state_holder.xknx_for_listening,
        always_valid_conditions,
        test_state_holder.group_address_to_dpt,
        LOGS_DIR,
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
    assert state._physical_state == state._last_valid_physical_state
    assert test_state_holder.app_one_called == True
    assert test_state_holder.app_two_called == True
    assert test_state_holder.app_three_called == False
    assert test_state_holder.app_four_called == False
    assert test_state_holder.app_one.should_run == True
    assert test_state_holder.app_two.should_run == True
    assert test_state_holder.app_three.should_run == True
    assert test_state_holder.app_four.should_run == True
    assert state._app_states[FIRST_APP_NAME] == AppState()
    assert state._app_states[SECOND_APP_NAME] == AppState(
        INT_0=new_int_0_value,
        FLOAT_1=new_float_1_value,
        BOOL_2=new_bool_2_value,
        STR_3=new_str_3_value,
    )
    assert state._app_states[THIRD_APP_NAME] == AppState()
    assert state._app_states[FOURTH_APP_NAME] == AppState()

    # Cleanup
    await state.stop()


@pytest.mark.asyncio
async def test_state_on_telegram_append_to_logs_received_telegrams_after_33_telegrams():
    new_second_address_value = 8.1
    new_third_address_value = True

    def test_two_code(app_state: AppState, physical_state: PhysicalState, internal_state: InternalState):
        test_state_holder.app_two_called = True
        physical_state.GA_1_1_2 = new_second_address_value
        physical_state.GA_1_1_3 = new_third_address_value

    test_state_holder.set_app_two_code(test_two_code)

    state = State(
        test_state_holder.addresses_listeners,
        test_state_holder.xknx_for_initialization,
        test_state_holder.xknx_for_listening,
        always_valid_conditions,
        test_state_holder.group_address_to_dpt,
        LOGS_DIR,
    )
    await state.initialize()

    telegram1 = Telegram(
        GroupAddress(FIRST_GROUP_ADDRESS),
        payload=MockGroupValueWrite(MockAPCIValue(RECEIVED_RAW_VALUE)),
    )
    telegram2 = Telegram(
        GroupAddress(SECOND_GROUP_ADDRESS),
        payload=MockGroupValueWrite(MockAPCIValue(RECEIVED_RAW_VALUE)),
    )
    for _ in range(12):
        await test_state_holder.xknx_for_listening.telegram_queue.receive_telegram(
            telegram1
        )
    for _ in range(21):
        await test_state_holder.xknx_for_listening.telegram_queue.receive_telegram(
            telegram2
        )

    log_file_path = f"{LOGS_DIR}/telegrams_received.log"

    with open(log_file_path, "r") as log_file:
        lines = log_file.readlines()
        assert len(lines) == 33
        for i in range(12):
            assert str(telegram1) in lines[i]
        for i in range(12, 21):
            assert str(telegram2) in lines[i]

    # Cleanup
    await state.stop()


@pytest.mark.asyncio
async def test_state_correct_execution_log_after_33_telegrams():
    new_second_address_value = 8.1
    new_third_address_value = True

    def test_two_code(app_state: AppState, physical_state: PhysicalState, internal_state: InternalState):
        test_state_holder.app_two_called = True
        physical_state.GA_1_1_2 = new_second_address_value
        physical_state.GA_1_1_3 = new_third_address_value

    test_state_holder.set_app_two_code(test_two_code)

    state = State(
        test_state_holder.addresses_listeners,
        test_state_holder.xknx_for_initialization,
        test_state_holder.xknx_for_listening,
        always_valid_conditions,
        test_state_holder.group_address_to_dpt,
        LOGS_DIR,
    )
    await state.initialize()

    telegram1 = Telegram(
        GroupAddress(FIRST_GROUP_ADDRESS),
        payload=MockGroupValueWrite(MockAPCIValue(RECEIVED_RAW_VALUE)),
    )
    telegram2 = Telegram(
        GroupAddress(SECOND_GROUP_ADDRESS),
        payload=MockGroupValueWrite(MockAPCIValue(RECEIVED_RAW_VALUE)),
    )
    for _ in range(12):
        await test_state_holder.xknx_for_listening.telegram_queue.receive_telegram(
            telegram1
        )
    for _ in range(21):
        await test_state_holder.xknx_for_listening.telegram_queue.receive_telegram(
            telegram2
        )

    log_file_path = f"{LOGS_DIR}/execution.log"
    expected_log_file_path = f"tests/expected/execution.log_expected"

    with open(log_file_path, "r") as log_file, open(
        expected_log_file_path, "r"
    ) as expected_log_file:
        expected_lines = expected_log_file.readlines()
        lines = log_file.readlines()
        n_lines = len(lines)
        assert n_lines == len(expected_lines)

        for i in range(n_lines):
            assert expected_lines[i][:-1] in lines[i][:-1]

    # Cleanup
    await state.stop()


@pytest.mark.asyncio
async def test_state_logger_remove_first_1000_lines_when_file_exceeds_20MB():

    os.makedirs(LOGS_DIR)
    shutil.copy("tests/fake_logs/execution.log_fake", f"{LOGS_DIR}/execution.log")

    new_second_address_value = 8.1
    new_third_address_value = True

    def test_two_code(app_state: AppState, physical_state: PhysicalState, internal_state: InternalState):
        test_state_holder.app_two_called = True
        physical_state.GA_1_1_2 = new_second_address_value
        physical_state.GA_1_1_3 = new_third_address_value

    test_state_holder.set_app_two_code(test_two_code)

    state = State(
        test_state_holder.addresses_listeners,
        test_state_holder.xknx_for_initialization,
        test_state_holder.xknx_for_listening,
        always_valid_conditions,
        test_state_holder.group_address_to_dpt,
        LOGS_DIR,
    )
    await state.initialize()

    telegram1 = Telegram(
        GroupAddress(FIRST_GROUP_ADDRESS),
        payload=MockGroupValueWrite(MockAPCIValue(RECEIVED_RAW_VALUE)),
    )

    await test_state_holder.xknx_for_listening.telegram_queue.receive_telegram(
        telegram1
    )

    # stops the state to flush logs
    await state.stop()

    log_file_path = f"{LOGS_DIR}/execution.log"

    with open(log_file_path, "r") as log_file:
        first_line = log_file.readline()
        assert first_line.startswith("1001")


@pytest.mark.asyncio
async def test_state_log_files_are_bounded():

    os.makedirs(LOGS_DIR)
    shutil.copy("tests/fake_logs/execution.log_fake", f"{LOGS_DIR}/execution.log")
    shutil.copy(
        "tests/fake_logs/telegrams_received.log_fake", f"{LOGS_DIR}/telegrams_received.log"
    )

    new_second_address_value = 8.1
    new_third_address_value = True

    def test_two_code(app_state: AppState, physical_state: PhysicalState, internal_state: InternalState):
        test_state_holder.app_two_called = True
        physical_state.GA_1_1_2 = new_second_address_value
        physical_state.GA_1_1_3 = new_third_address_value

    test_state_holder.set_app_two_code(test_two_code)

    state = State(
        test_state_holder.addresses_listeners,
        test_state_holder.xknx_for_initialization,
        test_state_holder.xknx_for_listening,
        always_valid_conditions,
        test_state_holder.group_address_to_dpt,
        LOGS_DIR,
    )
    await state.initialize()

    telegram1 = Telegram(
        GroupAddress(FIRST_GROUP_ADDRESS),
        payload=MockGroupValueWrite(MockAPCIValue(RECEIVED_RAW_VALUE)),
    )
    for _ in range(8000):
        await test_state_holder.xknx_for_listening.telegram_queue.receive_telegram(
            telegram1
        )

    # stops the state to flush logs
    await state.stop()

    execution_log_file_path = f"{LOGS_DIR}/execution.log"
    telegrams_received_log_file_path = f"{LOGS_DIR}/telegrams_received.log"

    max_size = 20 * 1024 * 1024
    assert os.path.getsize(execution_log_file_path) < max_size
    assert os.path.getsize(telegrams_received_log_file_path) < max_size
