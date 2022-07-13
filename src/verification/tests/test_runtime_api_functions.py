import dataclasses
import datetime
import time

from ..runtime_svshi_api_functions import check_time_property


@dataclasses.dataclass
class CheckState:
    start_frequency: int = 0
    start_condition_true: int = 0
    condition_was_true: bool = False


@dataclasses.dataclass
class InternalState:
    date_time: time.struct_time  # time object, at local machine time
    check_condition = {0: CheckState(), 1: CheckState()}
    app_files_runtime_folder_path: str  # path to the folder in which files used by apps are stored at runtime


class SvshiApi():
    def __init__(self):
        pass

    class Hour:
        def __init__(self, value, internal_state: InternalState):
            self.value = value * 3600

    class Minute:
        def __init__(self, value, internal_state: InternalState):
            self.value = value * 60

    class Day:
        def __init__(self, value, internal_state: InternalState):
            self.value = value * 3600 * 24

    class Week:
        def __init__(self, value, internal_state: InternalState):
            self.value = value * 7 * 24 * 3600

    class Month:
        def __init__(self, value, internal_state: InternalState):
            self.value = value * 30 * 24 * 3600


svshi_api = SvshiApi()


def test_check_time_property_false_after_end_of_frequency():
    date_time = datetime.datetime.now()
    internal_state = InternalState(date_time.timetuple(), "")
    internal_state.check_condition = {0: CheckState(), 1: CheckState()}
    check_state = CheckState
    date_time = date_time + datetime.timedelta(days=1)
    internal_state.date_time = date_time.timetuple()
    out = check_time_property(None, frequency=svshi_api.Day(1, None), duration=svshi_api.Minute(2, None),
                              condition=False, internal_state=internal_state, check_num=0)
    assert not out


def test_check_time_property_true_before_end_of_frequency():
    date_time = datetime.datetime.now()
    internal_state = InternalState(date_time.timetuple(), "")
    check_state = CheckState()
    internal_state.check_condition = {0: check_state}
    # reset counters
    check_time_property(None, frequency=svshi_api.Day(1, None), duration=svshi_api.Minute(2, None), condition=False,
                        internal_state=internal_state, check_num=0)
    date_time = date_time + datetime.timedelta(minutes=50)
    internal_state.date_time = date_time.timetuple()
    # set condition to true at this time
    out = check_time_property(None, frequency=svshi_api.Day(1, None), duration=svshi_api.Minute(2, None),
                              condition=True, internal_state=internal_state, check_num=0)
    internal_time_first_true = int(time.mktime(internal_state.date_time))
    assert check_state.condition_was_true
    assert check_state.start_condition_true == internal_time_first_true

    # add 5 minute of "True"
    date_time = date_time + datetime.timedelta(minutes=5)
    internal_state.date_time = date_time.timetuple()
    out = check_time_property(None, frequency=svshi_api.Day(1, None), duration=svshi_api.Minute(2, None),
                              condition=True, internal_state=internal_state, check_num=0)
    internal_time = int(time.mktime(internal_state.date_time))
    assert out
    assert check_state.condition_was_true
    assert check_state.start_frequency == internal_time
    assert check_state.start_condition_true == internal_time_first_true


def test_check_time_property_true_when_hold_for_the_given_duration():
    date_time = datetime.datetime.now()
    internal_state = InternalState(date_time.timetuple(), "")
    check_state = CheckState()
    internal_state.check_condition = {0: check_state}
    # reset counters
    check_time_property(None, frequency=svshi_api.Day(1, None), duration=svshi_api.Minute(2, None), condition=False,
                        internal_state=internal_state, check_num=0)
    date_time = date_time + datetime.timedelta(minutes=50)
    internal_state.date_time = date_time.timetuple()
    # set condition to true at this time
    internal_time_first_true = int(time.mktime(internal_state.date_time))
    out = check_time_property(None, frequency=svshi_api.Day(1, None), duration=svshi_api.Minute(2, None),
                              condition=True, internal_state=internal_state, check_num=0)
    date_time = date_time + datetime.timedelta(minutes=5)
    internal_state.date_time = date_time.timetuple()
    out = check_time_property(None, frequency=svshi_api.Day(1, None), duration=svshi_api.Minute(2, None),
                              condition=True, internal_state=internal_state, check_num=0)
    # increase time by 10 min and condition is false
    date_time = date_time + datetime.timedelta(minutes=5)
    internal_state.date_time = date_time.timetuple()
    internal_time_last_true = int(time.mktime(internal_state.date_time))
    out = check_time_property(None, frequency=svshi_api.Day(1, None), duration=svshi_api.Minute(2, None),
                              condition=False, internal_state=internal_state, check_num=0)
    assert out
    assert not check_state.condition_was_true
    assert check_state.start_condition_true == internal_time_first_true
    assert check_state.start_frequency == internal_time_last_true

def test_check_time_property_true_but_less_than_duration():
    date_time = datetime.datetime.now()
    internal_state = InternalState(date_time.timetuple(), "")
    check_state = CheckState()
    internal_state.check_condition = {0: check_state}
    # reset counters
    check_time_property(None, frequency=svshi_api.Month(2, None), duration=svshi_api.Day(4, None), condition=False,
                        internal_state=internal_state, check_num=0)
    date_time = date_time + datetime.timedelta(days=3)
    internal_state.date_time = date_time.timetuple()
    # set condition to true at this time
    internal_time_first_true = int(time.mktime(internal_state.date_time))
    out = check_time_property(None, frequency=svshi_api.Month(2, None), duration=svshi_api.Day(4, None),
                              condition=True, internal_state=internal_state, check_num=0)
    date_time = date_time + datetime.timedelta(hours=65)
    internal_state.date_time = date_time.timetuple()
    out = check_time_property(None, frequency=svshi_api.Month(2, None), duration=svshi_api.Day(4, None),
                              condition=True, internal_state=internal_state, check_num=0)
    # increase time by 1h and condition is false
    date_time = date_time + datetime.timedelta(hours=1)
    internal_state.date_time = date_time.timetuple()
    
    assert check_state.condition_was_true
    
    out = check_time_property(None, frequency=svshi_api.Month(2, None), duration=svshi_api.Day(4, None),
                              condition=False, internal_state=internal_state, check_num=0)

    date_time = date_time + datetime.timedelta(hours=30)
    internal_state.date_time = date_time.timetuple()
    internal_time_last_true = int(time.mktime(internal_state.date_time))
    out = check_time_property(None, frequency=svshi_api.Month(2, None), duration=svshi_api.Day(4, None),
                              condition=False, internal_state=internal_state, check_num=0)
    date_time = date_time + datetime.timedelta(days=54)
    internal_state.date_time = date_time.timetuple()
    out = check_time_property(None, frequency=svshi_api.Month(2, None), duration=svshi_api.Day(4, None),
                              condition=False, internal_state=internal_state, check_num=0)
    assert not out
    assert not check_state.condition_was_true
    assert check_state.start_condition_true == internal_time_first_true
    assert check_state.start_frequency == 0
