import asyncio
import dataclasses
import shutil
import os
import textwrap
import time

import pytest
from pytest_mock import MockerFixture
from dataclasses import dataclass
from typing import Any, Callable, Coroutine, Dict, List, Optional, Union, cast
from xknx.dpt.dpt import DPTArray, DPTBase, DPTBinary
from xknx.dpt.dpt_2byte_float import DPT2ByteFloat
from xknx.telegram.address import GroupAddress
from xknx.telegram.apci import GroupValueWrite
from xknx.telegram.telegram import Telegram
from xknx.xknx import XKNX

from ..verification_file import (
    AppState,
    PhysicalState,
    InternalState,
    IsolatedFunctionsValues,
)
from ..app import App
from ..isolated_functions import RuntimeIsolatedFunction
from ..state import State
from ..joint_apps import JointApps

LOGS_DIR = "tests/logs"
PHYSICAL_STATE_LOG_FILE_PATH = "physical_state.json"
RUNTIME_APP_FILES_FOLDER_PATH = "tests/files"

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


def app1_app_on_trigger_print(s: str, internal_state: InternalState) -> int:
    print(s)
    return 32


def app1_app_periodic_print(internal_state: InternalState) -> None:
    """period: 2"""
    print("Another hello!")


def app2_app_periodic_print(internal_state: InternalState) -> float:
    """period: 1"""
    print("Periodic hello!")
    return 3.4


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
        self.joint_apps_called = False
        self.app_two_code_description = ""

        self.app_one = App(FIRST_APP_NAME, "tests", self.app_one_code)
        self.app_two = App(SECOND_APP_NAME, "tests", self.app_two_code)
        self.app_three = App(THIRD_APP_NAME, "tests", self.app_three_code, timer=2)
        self.app_four = App(FOURTH_APP_NAME, "tests", self.app_four_code, timer=5)
        self.addresses_listeners = {
            FIRST_GROUP_ADDRESS: [
                self.app_one,
                self.app_two,
            ],
            SECOND_GROUP_ADDRESS: [self.app_three],
            THIRD_GROUP_ADDRESS: [self.app_three],
            FOURTH_GROUP_ADDRESS: [self.app_four],
        }
        self.joint_apps = [JointApps("joint_apps", self.joint_apps_code)]
        self.isolated_fns = [
            RuntimeIsolatedFunction(
                "app1_app_on_trigger_print", app1_app_on_trigger_print, int, None
            ),
            RuntimeIsolatedFunction(
                "app1_app_periodic_print", app1_app_periodic_print, type(None), 2
            ),
            RuntimeIsolatedFunction(
                "app2_app_periodic_print", app2_app_periodic_print, float, 1
            ),
        ]

        self.group_address_to_dpt: Dict[str, Union[DPTBase, DPTBinary]] = {
            FIRST_GROUP_ADDRESS: DPT2ByteFloat(),
            SECOND_GROUP_ADDRESS: DPT2ByteFloat(),
            THIRD_GROUP_ADDRESS: DPTBinary(0),
            FOURTH_GROUP_ADDRESS: DPTBinary(0),
        }

        self.xknx_for_initialization = MockXKNX(MockTelegramQueue())
        self.xknx_for_listening = MockXKNX(MockTelegramQueue())
        self.xknx_for_periodic_reads = MockXKNX(MockTelegramQueue())
        self._last_valid_physical_state: PhysicalState

        self.on_trigger_consumer = None

        def register_on_trigger_consumer(consumer: Callable[[Callable], Callable]):
            """Remember the consumer."""
            self.on_trigger_consumer = consumer

        self.register_on_trigger_consumer = register_on_trigger_consumer

    def app_one_code(
        self,
        state1: AppState,
        state2: PhysicalState,
        internal_state: InternalState,
        isolated_fn_values: IsolatedFunctionsValues,
    ):
        self.app_one_called = True

    def app_two_code(
        self,
        state1: AppState,
        state: PhysicalState,
        internal_state: InternalState,
        isolated_fn_values: IsolatedFunctionsValues,
    ):
        self.app_two_called = True

    def app_three_code(
        self,
        state1: AppState,
        state: PhysicalState,
        internal_state: InternalState,
        isolated_fn_values: IsolatedFunctionsValues,
    ):
        self.app_three_called = True

    def app_four_code(
        self,
        state1: AppState,
        physical_state: PhysicalState,
        internal_state: InternalState,
        isolated_fn_values: IsolatedFunctionsValues,
    ):
        self.on_trigger_consumer(app1_app_on_trigger_print)("On trigger hello!")
        self.app_four_called = True

    def joint_apps_code(
        self,
        test1_app_state: AppState,
        test2_app_state: AppState,
        test3_app_state: AppState,
        test4_app_state: AppState,
        physical_state: PhysicalState,
        internal_state: InternalState,
        isolated_fn_values: IsolatedFunctionsValues,
    ):
        self.app_one_called = True
        if self.app_two_code_description == "test_two_code":
            if self.app_two_start_time == None:
                physical_state.GA_1_1_2 = 8.1
                physical_state.GA_1_1_3 = True
                self.app_two_called = True
            else:
                self.app_two_called = True

                if self.app_two_start_time - 5 <= time.mktime(internal_state.date_time):
                    physical_state.GA_1_1_2 = 8.1
                    physical_state.GA_1_1_3 = True
        elif self.app_two_code_description == "app_two_code_that_violates_conditions":
            self.app_two_called = True
            physical_state.GA_1_1_2 = 8
        elif self.app_two_code_description == "test_two_code_app_state":
            self.app_two_called = True
            test2_app_state.INT_0 = 42
            test2_app_state.FLOAT_1 = 42.42
            test2_app_state.BOOL_2 = True
        else:
            self.app_two_called = True
        self.app_three_called = True
        self.on_trigger_consumer(app1_app_on_trigger_print)("On trigger hello!")
        self.app_four_called = True

    def reset(self):
        self = self.__init__()

    def reset_apps_called(self):
        self.app_one_called = False
        self.app_two_called = False
        self.app_three_called = False
        self.app_four_called = False
        self.joint_apps_called = False

    def set_app_two_code(self, code_name, start_time=None):
        self.app_two_code_description = code_name
        self.app_two_start_time = start_time


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
        test_state_holder.joint_apps,
        test_state_holder.xknx_for_initialization,
        test_state_holder.xknx_for_listening,
        test_state_holder.xknx_for_periodic_reads,
        always_valid_conditions,
        test_state_holder.group_address_to_dpt,
        LOGS_DIR,
        RUNTIME_APP_FILES_FOLDER_PATH,
        PHYSICAL_STATE_LOG_FILE_PATH,
        test_state_holder.isolated_fns,
    )
    await state.listen()

    assert test_state_holder.xknx_for_listening.listening == True

    # Cleanup
    await state.stop()


@pytest.mark.asyncio
async def test_state_stop():
    state = State(
        test_state_holder.addresses_listeners,
        test_state_holder.joint_apps,
        test_state_holder.xknx_for_initialization,
        test_state_holder.xknx_for_listening,
        test_state_holder.xknx_for_periodic_reads,
        always_valid_conditions,
        test_state_holder.group_address_to_dpt,
        LOGS_DIR,
        RUNTIME_APP_FILES_FOLDER_PATH,
        PHYSICAL_STATE_LOG_FILE_PATH,
        test_state_holder.isolated_fns,
    )
    await state.initialize(test_state_holder.register_on_trigger_consumer)
    await state.listen()
    await state.stop()

    assert test_state_holder.xknx_for_listening.listening == False


@pytest.mark.asyncio
async def test_state_initialize():
    state = State(
        test_state_holder.addresses_listeners,
        test_state_holder.joint_apps,
        test_state_holder.xknx_for_initialization,
        test_state_holder.xknx_for_listening,
        test_state_holder.xknx_for_periodic_reads,
        always_valid_conditions,
        test_state_holder.group_address_to_dpt,
        LOGS_DIR,
        RUNTIME_APP_FILES_FOLDER_PATH,
        PHYSICAL_STATE_LOG_FILE_PATH,
        test_state_holder.isolated_fns,
    )
    await state.initialize(test_state_holder.register_on_trigger_consumer)

    assert state._physical_state != None
    assert state._physical_state.GA_1_1_1 == VALUE_READER_RETURN_VALUE
    assert state._physical_state.GA_1_1_2 == VALUE_READER_RETURN_VALUE
    assert state._physical_state.GA_1_1_3 == False
    assert state._physical_state.GA_1_1_4 == True
    assert state._app_states[f"{FIRST_APP_NAME}_app_state"] == AppState()
    assert state._app_states[f"{SECOND_APP_NAME}_app_state"] == AppState()
    assert state._app_states[f"{THIRD_APP_NAME}_app_state"] == AppState()
    assert state._app_states[f"{FOURTH_APP_NAME}_app_state"] == AppState()

    # Cleanup
    await state.stop()


@pytest.mark.asyncio
async def test_internal_state_is_updated():
    state = State(
        test_state_holder.addresses_listeners,
        test_state_holder.joint_apps,
        test_state_holder.xknx_for_initialization,
        test_state_holder.xknx_for_listening,
        test_state_holder.xknx_for_periodic_reads,
        always_valid_conditions,
        test_state_holder.group_address_to_dpt,
        LOGS_DIR,
        RUNTIME_APP_FILES_FOLDER_PATH,
        PHYSICAL_STATE_LOG_FILE_PATH,
        test_state_holder.isolated_fns,
    )
    NEW_SECOND_ADRESS_VALUE = 8.1
    NEW_THIRD_ADDRESS_VALUE = True
    start_test_time = time.time()

    test_state_holder.set_app_two_code("test_two_code", start_test_time)
    await state.initialize(test_state_holder.register_on_trigger_consumer)

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
    assert (
        start_test_time - 3
        <= time.mktime(state._internal_state.date_time)
        <= start_test_time + 3
    )
    assert test_state_holder.app_one_called == True
    assert test_state_holder.app_two_called == True
    assert test_state_holder.app_three_called == True
    assert test_state_holder.app_four_called == True
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
    assert (
        current_test_time - 3
        <= time.mktime(state._internal_state.date_time)
        <= current_test_time + 3
    )
    assert test_state_holder.app_one.should_run == True
    assert test_state_holder.app_two.should_run == True
    assert test_state_holder.app_three.should_run == True
    assert test_state_holder.app_four.should_run == True
    assert state._app_states[f"{FIRST_APP_NAME}_app_state"] == AppState()
    assert state._app_states[f"{SECOND_APP_NAME}_app_state"] == AppState()
    assert state._app_states[f"{THIRD_APP_NAME}_app_state"] == AppState()
    assert state._app_states[f"{FOURTH_APP_NAME}_app_state"] == AppState()

    # Cleanup
    await state.stop()


@pytest.mark.asyncio
async def test_state_periodic_apps_are_run():
    state = State(
        test_state_holder.addresses_listeners,
        test_state_holder.joint_apps,
        test_state_holder.xknx_for_initialization,
        test_state_holder.xknx_for_listening,
        test_state_holder.xknx_for_periodic_reads,
        always_valid_conditions,
        test_state_holder.group_address_to_dpt,
        LOGS_DIR,
        RUNTIME_APP_FILES_FOLDER_PATH,
        PHYSICAL_STATE_LOG_FILE_PATH,
        test_state_holder.isolated_fns,
    )
    await state.initialize(test_state_holder.register_on_trigger_consumer)

    assert state._physical_state != None
    assert state._physical_state.GA_1_1_1 == VALUE_READER_RETURN_VALUE
    assert state._physical_state.GA_1_1_2 == VALUE_READER_RETURN_VALUE
    assert state._physical_state.GA_1_1_3 == False
    assert state._physical_state.GA_1_1_4 == True
    assert state._app_states[f"{FIRST_APP_NAME}_app_state"] == AppState()
    assert state._app_states[f"{SECOND_APP_NAME}_app_state"] == AppState()
    assert state._app_states[f"{THIRD_APP_NAME}_app_state"] == AppState()
    assert state._app_states[f"{FOURTH_APP_NAME}_app_state"] == AppState()

    # We wait just to make sure the periodic app was called
    await asyncio.sleep(3)

    assert test_state_holder.app_four_called == True
    assert state._app_states[f"{FIRST_APP_NAME}_app_state"] == AppState()
    assert state._app_states[f"{SECOND_APP_NAME}_app_state"] == AppState()
    assert state._app_states[f"{THIRD_APP_NAME}_app_state"] == AppState()
    assert state._app_states[f"{FOURTH_APP_NAME}_app_state"] == AppState()
    assert state._physical_state == state._last_valid_physical_state

    # Cleanup
    await state.stop()


@pytest.mark.asyncio
async def test_state_on_trigger_and_periodic_functions_are_run(capsys):
    # Reuse the execution of the periodic apps test
    await test_state_periodic_apps_are_run()

    # Check periodic functions have been executed a correct amount of times
    captured: List[str] = capsys.readouterr().out.split("\n")
    assert 3 <= captured.count("Periodic hello!") <= 4
    assert captured.count("Another hello!") == 2
    assert captured.count("On trigger hello!") == 2


@pytest.mark.asyncio
async def test_state_on_telegram_update_state_and_notify():
    state = State(
        test_state_holder.addresses_listeners,
        test_state_holder.joint_apps,
        test_state_holder.xknx_for_initialization,
        test_state_holder.xknx_for_listening,
        test_state_holder.xknx_for_periodic_reads,
        always_valid_conditions,
        test_state_holder.group_address_to_dpt,
        LOGS_DIR,
        RUNTIME_APP_FILES_FOLDER_PATH,
        PHYSICAL_STATE_LOG_FILE_PATH,
        test_state_holder.isolated_fns,
    )
    await state.initialize(test_state_holder.register_on_trigger_consumer)

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
    assert test_state_holder.app_four_called == True
    assert test_state_holder.xknx_for_listening.telegrams.empty() == True
    assert test_state_holder.app_one.should_run == True
    assert test_state_holder.app_two.should_run == True
    assert test_state_holder.app_three.should_run == True
    assert test_state_holder.app_four.should_run == True
    assert state._app_states[f"{FIRST_APP_NAME}_app_state"] == AppState()
    assert state._app_states[f"{SECOND_APP_NAME}_app_state"] == AppState()
    assert state._app_states[f"{THIRD_APP_NAME}_app_state"] == AppState()
    assert state._app_states[f"{FOURTH_APP_NAME}_app_state"] == AppState()

    # Cleanup
    await state.stop()


@pytest.mark.asyncio
async def test_state_sends_read_request_and_update_state_and_notify(
    mocker: MockerFixture,
):
    state = State(
        test_state_holder.addresses_listeners,
        test_state_holder.joint_apps,
        test_state_holder.xknx_for_initialization,
        test_state_holder.xknx_for_listening,
        test_state_holder.xknx_for_periodic_reads,
        always_valid_conditions,
        test_state_holder.group_address_to_dpt,
        LOGS_DIR,
        RUNTIME_APP_FILES_FOLDER_PATH,
        PHYSICAL_STATE_LOG_FILE_PATH,
        test_state_holder.isolated_fns,
        periodic_read_frequency_second=2.0,
    )
    await state.initialize(test_state_holder.register_on_trigger_consumer)

    read_reply_ga_1 = Telegram(
        GroupAddress(FIRST_GROUP_ADDRESS),
        payload=MockGroupValueWrite(MockAPCIValue(RECEIVED_RAW_VALUE)),
    )

    read_reply_ga_3 = Telegram(
        GroupAddress(THIRD_GROUP_ADDRESS),
        payload=MockGroupValueWrite(DPTBinary(1)),
    )

    mocker.patch(
        "xknx.core.value_reader.ValueReader.read",
        side_effect=[
            read_reply_ga_1,
            read_reply_ga_3,
        ],
    )

    # We wait just to make sure the periodic read requests were sent and replies processed
    await asyncio.sleep(4)

    assert state._physical_state.GA_1_1_1 == RECEIVED_VALUE
    assert state._physical_state.GA_1_1_2 == VALUE_READER_RETURN_VALUE
    assert state._physical_state.GA_1_1_3 == True
    assert state._physical_state.GA_1_1_4 == True
    assert state._physical_state == state._last_valid_physical_state
    assert test_state_holder.app_one_called == True
    assert test_state_holder.app_two_called == True
    assert test_state_holder.app_three_called == True
    assert test_state_holder.app_four_called == True
    assert test_state_holder.xknx_for_listening.telegrams.empty() == True
    assert test_state_holder.app_one.should_run == True
    assert test_state_holder.app_two.should_run == True
    assert test_state_holder.app_three.should_run == True
    assert test_state_holder.app_four.should_run == True
    assert state._app_states[f"{FIRST_APP_NAME}_app_state"] == AppState()
    assert state._app_states[f"{SECOND_APP_NAME}_app_state"] == AppState()
    assert state._app_states[f"{THIRD_APP_NAME}_app_state"] == AppState()
    assert state._app_states[f"{FOURTH_APP_NAME}_app_state"] == AppState()

    # Cleanup
    await state.stop()


@pytest.mark.asyncio
async def test_state_on_telegram_update_state_makes_it_invalid_merged_state_invalid_then_runtime_stops_and_raises_interrupt(
    mocker: MockerFixture,
):
    with pytest.raises(KeyboardInterrupt):
        state = State(
            test_state_holder.addresses_listeners,
            test_state_holder.joint_apps,
            test_state_holder.xknx_for_initialization,
            test_state_holder.xknx_for_listening,
            test_state_holder.xknx_for_periodic_reads,
            conditions,
            test_state_holder.group_address_to_dpt,
            LOGS_DIR,
            RUNTIME_APP_FILES_FOLDER_PATH,
            PHYSICAL_STATE_LOG_FILE_PATH,
            test_state_holder.isolated_fns,
        )
        state_stop_spy = mocker.spy(state, "stop")
        await state.initialize(test_state_holder.register_on_trigger_consumer)

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
        assert test_state_holder.app_three_called == True
        assert test_state_holder.app_four_called == True
        assert test_state_holder.xknx_for_listening.telegrams.empty() == True
        assert test_state_holder.app_one.should_run == True
        assert test_state_holder.app_two.should_run == True
        assert test_state_holder.app_three.should_run == True
        assert test_state_holder.app_four.should_run == True
        assert state._app_states[f"{FIRST_APP_NAME}_app_state"] == AppState()
        assert state._app_states[f"{SECOND_APP_NAME}_app_state"] == AppState()
        assert state._app_states[f"{THIRD_APP_NAME}_app_state"] == AppState()
        assert state._app_states[f"{FOURTH_APP_NAME}_app_state"] == AppState()

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
        assert test_state_holder.app_one_called == True
        assert test_state_holder.app_two_called == True
        assert test_state_holder.app_three_called == True
        assert test_state_holder.app_four_called == True
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
        assert state._app_states[f"{FIRST_APP_NAME}_app_state"] == AppState()
        assert state._app_states[f"{SECOND_APP_NAME}_app_state"] == AppState()
        assert state._app_states[f"{THIRD_APP_NAME}_app_state"] == AppState()
        assert state._app_states[f"{FOURTH_APP_NAME}_app_state"] == AppState()
        # Stop is called
        state_stop_spy.assert_called_once()


@pytest.mark.asyncio
async def test_state_on_telegram_update_state_and_notify_and_stop_app_violating_conditions(
    mocker: MockerFixture,
):
    with pytest.raises(KeyboardInterrupt):

        test_state_holder.set_app_two_code("app_two_code_that_violates_conditions")
        state = State(
            test_state_holder.addresses_listeners,
            test_state_holder.joint_apps,
            test_state_holder.xknx_for_initialization,
            test_state_holder.xknx_for_listening,
            test_state_holder.xknx_for_periodic_reads,
            conditions,
            test_state_holder.group_address_to_dpt,
            LOGS_DIR,
            RUNTIME_APP_FILES_FOLDER_PATH,
            PHYSICAL_STATE_LOG_FILE_PATH,
            test_state_holder.isolated_fns,
        )
        state_stop_spy = mocker.spy(state, "stop")
        await state.initialize(test_state_holder.register_on_trigger_consumer)

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
        assert test_state_holder.app_three_called == True
        assert test_state_holder.app_four_called == True
        assert test_state_holder.xknx_for_listening.telegrams.empty() == True
        assert test_state_holder.app_one.should_run == True
        assert test_state_holder.app_two.should_run == False
        assert test_state_holder.app_three.should_run == True
        assert test_state_holder.app_four.should_run == True
        assert state._app_states[f"{FIRST_APP_NAME}_app_state"] == AppState()
        assert state._app_states[f"{SECOND_APP_NAME}_app_state"] == AppState()
        assert state._app_states[f"{THIRD_APP_NAME}_app_state"] == AppState()
        assert state._app_states[f"{FOURTH_APP_NAME}_app_state"] == AppState()

        state_stop_spy.assert_called_once()


@pytest.mark.asyncio
async def test_state_on_telegram_update_state_and_notify_and_update_again_and_notify_and_send_to_knx():

    new_second_address_value = 8.1
    new_third_address_value = True
    test_state_holder.set_app_two_code("test_two_code")

    state = State(
        test_state_holder.addresses_listeners,
        test_state_holder.joint_apps,
        test_state_holder.xknx_for_initialization,
        test_state_holder.xknx_for_listening,
        test_state_holder.xknx_for_periodic_reads,
        always_valid_conditions,
        test_state_holder.group_address_to_dpt,
        LOGS_DIR,
        RUNTIME_APP_FILES_FOLDER_PATH,
        PHYSICAL_STATE_LOG_FILE_PATH,
        test_state_holder.isolated_fns,
    )
    await state.initialize(test_state_holder.register_on_trigger_consumer)

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
    assert test_state_holder.app_four_called == True
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
    assert state._app_states[f"{FIRST_APP_NAME}_app_state"] == AppState()
    assert state._app_states[f"{SECOND_APP_NAME}_app_state"] == AppState()
    assert state._app_states[f"{THIRD_APP_NAME}_app_state"] == AppState()
    assert state._app_states[f"{FOURTH_APP_NAME}_app_state"] == AppState()

    # Cleanup
    await state.stop()


@pytest.mark.asyncio
async def test_state_update_app_state():

    new_int_0_value = 42
    new_float_1_value = 42.42
    new_bool_2_value = True

    test_state_holder.set_app_two_code("test_two_code_app_state")

    state = State(
        test_state_holder.addresses_listeners,
        test_state_holder.joint_apps,
        test_state_holder.xknx_for_initialization,
        test_state_holder.xknx_for_listening,
        test_state_holder.xknx_for_periodic_reads,
        always_valid_conditions,
        test_state_holder.group_address_to_dpt,
        LOGS_DIR,
        RUNTIME_APP_FILES_FOLDER_PATH,
        PHYSICAL_STATE_LOG_FILE_PATH,
        test_state_holder.isolated_fns,
    )
    await state.initialize(test_state_holder.register_on_trigger_consumer)

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
    assert test_state_holder.app_three_called == True
    assert test_state_holder.app_four_called == True
    assert test_state_holder.app_one.should_run == True
    assert test_state_holder.app_two.should_run == True
    assert test_state_holder.app_three.should_run == True
    assert test_state_holder.app_four.should_run == True
    assert state._app_states[f"{FIRST_APP_NAME}_app_state"] == AppState()
    assert state._app_states[f"{SECOND_APP_NAME}_app_state"] == AppState(
        INT_0=new_int_0_value,
        FLOAT_1=new_float_1_value,
        BOOL_2=new_bool_2_value,
    )
    assert state._app_states[f"{THIRD_APP_NAME}_app_state"] == AppState()
    assert state._app_states[f"{FOURTH_APP_NAME}_app_state"] == AppState()

    # Cleanup
    await state.stop()


@pytest.mark.asyncio
async def test_state_on_telegram_append_to_logs_received_telegrams_after_33_telegrams():

    test_state_holder.set_app_two_code("test_two_code")

    state = State(
        test_state_holder.addresses_listeners,
        test_state_holder.joint_apps,
        test_state_holder.xknx_for_initialization,
        test_state_holder.xknx_for_listening,
        test_state_holder.xknx_for_periodic_reads,
        always_valid_conditions,
        test_state_holder.group_address_to_dpt,
        LOGS_DIR,
        RUNTIME_APP_FILES_FOLDER_PATH,
        PHYSICAL_STATE_LOG_FILE_PATH,
        test_state_holder.isolated_fns,
    )
    await state.initialize(test_state_holder.register_on_trigger_consumer)

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

    test_state_holder.set_app_two_code("test_two_code")

    state = State(
        test_state_holder.addresses_listeners,
        test_state_holder.joint_apps,
        test_state_holder.xknx_for_initialization,
        test_state_holder.xknx_for_listening,
        test_state_holder.xknx_for_periodic_reads,
        always_valid_conditions,
        test_state_holder.group_address_to_dpt,
        LOGS_DIR,
        RUNTIME_APP_FILES_FOLDER_PATH,
        PHYSICAL_STATE_LOG_FILE_PATH,
        test_state_holder.isolated_fns,
    )
    await state.initialize(test_state_holder.register_on_trigger_consumer)

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

    test_state_holder.set_app_two_code("test_two_code")

    state = State(
        test_state_holder.addresses_listeners,
        test_state_holder.joint_apps,
        test_state_holder.xknx_for_initialization,
        test_state_holder.xknx_for_listening,
        test_state_holder.xknx_for_periodic_reads,
        always_valid_conditions,
        test_state_holder.group_address_to_dpt,
        LOGS_DIR,
        RUNTIME_APP_FILES_FOLDER_PATH,
        PHYSICAL_STATE_LOG_FILE_PATH,
        test_state_holder.isolated_fns,
    )
    await state.initialize(test_state_holder.register_on_trigger_consumer)

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
        "tests/fake_logs/telegrams_received.log_fake",
        f"{LOGS_DIR}/telegrams_received.log",
    )

    test_state_holder.set_app_two_code("test_two_code")

    state = State(
        test_state_holder.addresses_listeners,
        test_state_holder.joint_apps,
        test_state_holder.xknx_for_initialization,
        test_state_holder.xknx_for_listening,
        test_state_holder.xknx_for_periodic_reads,
        always_valid_conditions,
        test_state_holder.group_address_to_dpt,
        LOGS_DIR,
        RUNTIME_APP_FILES_FOLDER_PATH,
        PHYSICAL_STATE_LOG_FILE_PATH,
        test_state_holder.isolated_fns,
    )
    await state.initialize(test_state_holder.register_on_trigger_consumer)

    telegram1 = Telegram(
        GroupAddress(FIRST_GROUP_ADDRESS),
        payload=MockGroupValueWrite(MockAPCIValue(RECEIVED_RAW_VALUE)),
    )
    for _ in range(9000):
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


@pytest.mark.asyncio
async def test_state_on_telegram_update_state_and_write_physical_state_to_file():
    state = State(
        test_state_holder.addresses_listeners,
        test_state_holder.joint_apps,
        test_state_holder.xknx_for_initialization,
        test_state_holder.xknx_for_listening,
        test_state_holder.xknx_for_periodic_reads,
        always_valid_conditions,
        test_state_holder.group_address_to_dpt,
        LOGS_DIR,
        RUNTIME_APP_FILES_FOLDER_PATH,
        PHYSICAL_STATE_LOG_FILE_PATH,
        test_state_holder.isolated_fns,
    )
    await state.initialize(test_state_holder.register_on_trigger_consumer)

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

    expected_json = textwrap.dedent(
        f"""\
        {{
            "GA_1_1_1": {{
                "value": {RECEIVED_VALUE},
                "dpt": "DPT9"
            }},
            "GA_1_1_2": {{
                "value": {VALUE_READER_RETURN_VALUE},
                "dpt": "DPT9"
            }},
            "GA_1_1_3": {{
                "value": true,
                "dpt": "DPT1"
            }},
            "GA_1_1_4": {{
                "value": true,
                "dpt": "DPT1"
            }}
        }}"""
    )
    with open("physical_state.json", "r") as f:
        assert f.read() == expected_json

    await test_state_holder.xknx_for_listening.telegram_queue.receive_telegram(
        Telegram(
            GroupAddress(THIRD_GROUP_ADDRESS),
            payload=MockGroupValueWrite(DPTBinary(0)),
        )
    )

    assert state._physical_state.GA_1_1_1 == RECEIVED_VALUE
    assert state._physical_state.GA_1_1_2 == VALUE_READER_RETURN_VALUE
    assert state._physical_state.GA_1_1_3 == False
    assert state._physical_state.GA_1_1_4 == True

    expected_json = textwrap.dedent(
        f"""\
        {{
            "GA_1_1_1": {{
                "value": {RECEIVED_VALUE},
                "dpt": "DPT9"
            }},
            "GA_1_1_2": {{
                "value": {VALUE_READER_RETURN_VALUE},
                "dpt": "DPT9"
            }},
            "GA_1_1_3": {{
                "value": false,
                "dpt": "DPT1"
            }},
            "GA_1_1_4": {{
                "value": true,
                "dpt": "DPT1"
            }}
        }}"""
    )
    with open("physical_state.json", "r") as f:
        assert f.read() == expected_json

    # Cleanup
    await state.stop()
