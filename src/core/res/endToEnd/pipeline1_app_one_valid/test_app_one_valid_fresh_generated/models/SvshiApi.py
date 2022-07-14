from io import TextIOWrapper
from typing import Callable, IO, Optional, TypeVar
import sys
if sys.version_info < (3, 10):
    from typing_extensions import ParamSpec
else:
    from typing import ParamSpec


class SvshiApi:
    def __init__(self):
        pass

    def get_hour_of_the_day(self) -> int:
        """
        get the machine's hour in 24 hours format (return is between 0 and 23)
        example : 15 for Wed Apr 13 15:57:17 2022 UTC
        :returns the current hour based on the machine's time
        """
        pass

    def get_minute_in_hour(self) -> int:
        """
        get the machine's minutes in one hour (return is between 0 and 59)
        example : 57 for Wed Apr 13 15:57:17 2022 UTC
        :returns the current minute in the hour based on the machine's time
        """
        pass

    def get_day_of_week(self) -> int:
        """
        get the machine's day of the week, the week starts on Monday (return is between 1 and 7)
        example : 3 for Wed Apr 13 15:57:17 2022 UTC
        :returns the day of the week based on the machine's time
        """
        pass

    def get_day_of_month(self) -> int:
        """
        get the machine's day of the month (return is between 1 and 31)
        example : 13 for Wed Apr 13 15:57:17 2022 UTC
        :returns the day of the month based on the machine's time
        """
        pass

    def get_month_in_year(self) -> int:
        """
        get the machine's month of the year (return is between 1 and 12)
        example : 4 for Wed Apr 13 15:57:17 2022 UTC
        :returns the current month based on the machine's time
        """
        pass

    def get_year(self) -> int:
        """
        get the machine's year
        example : 2022 for Wed Apr 13 15:57:17 2022 UTC
        :returns the current year based on the machine's time
        """
        pass

    _T = TypeVar("_T")
    _P = ParamSpec("_P")

    def get_latest_value(self, function: Callable[..., _T]) -> Optional[_T]:
        """
        Get the latest computed value of a `periodic` or `on_trigger` function.
        Might return `None` if no result is available (i.e. the first execution of the
        function did not terminate yet).

        Note: you should assume the returned value could be any value of the correct
        type or `None`. So, you should treat it as if it was user input and accept any
        value without violating the invariants nor crashing.

        :returns the latest computed value of `function` or `None`
        """
        pass

    def trigger_if_not_running(
        self, on_trigger_function: Callable[_P, _T]
    ) -> Callable[_P, None]:
        """
        Trigger the given `on_trigger_function` to be executed separately with the given
        arguments and keyword arguments.

        Usage: svshi_api.trigger_if_not_running(on_trigger_fn)(arg1, arg2).

        The returned value can be later fetched by using `get_latest_value`.
        """
        pass

    def get_file_text_mode(self, file_name: str, mode: str) -> Optional[IO[str]]:
        """
        open the file with the given name as a text file in the given mode and
        return the file, None if the file does not exist.
        mode can be "r", "w" or "a" or a combination of two of them like "ra", "ar", "wr", ...
        :returns the opened file or None if it does not exist
        """
        m = mode
        if "b" in m:
            m = m.replace("a", "")
        try:
            f = open(f"files/{file_name}", mode)
            return f
        except:
            return None

    def get_file_binary_mode(self, file_name: str, mode: str) -> Optional[IO[bytes]]:
        """
        open the file with the given name as a binary file in the given mode and
        return the file, None if the file does not exist.
        mode can be "r", "w" or "a" or a combination of two of them like "ra", "ar", "wr", ...
        :returns the opened file or None if it does not exist
        """
        m = mode
        if "b" in m:
            m = m.replace("a", "")
        try:
            f = open(f"files/{file_name}", f"{mode}b")
            return f
        except:
            return None

    def get_file_path(self, file_name: str) -> str:
        """
        return the path for the given filename, even if it does not exist.
        The path is managed by SVSHI so it can change from an execution to another.
        The file_name must a valid filename, i.e., containing only alphanumerical
        characters, '-', '_' and '.'
        :returns the path to the given filename
        """
        return f"files/{file_name}"

    class Hour:
        """
        Class used to set an hourly frequency or a duration of hours in svshi api's check_time_property function
        Example: A frequency set to Hour(3) means the condition should be true for a give duration every three hours
        Example: A duration set to Hour(2) means the condition should be true for two hours every given frequency

        Value is between 0 and 23
        """

        def __init__(self, value: int):
            pass

    class Minute:
        """
        Class used to set a frequency of minutes or a duration of minutes in svshi api's check_time_property function
        Example: A frequency set to Minute(30) means the condition should be true for a give duration every half an hour
        Example: A duration set to Minute(10) means the condition should be true for 10 minutes every given frequency

        Value is between 0 and 59
        """

        def __init__(self, value: int):
            pass

    class Day:
        """
        Class used to set a daily frequency or a duration of days in svshi api's check_time_property function
        Example: A frequency set to Day(3) means the condition should be true for a give duration every three days
        Example: A duration set to Day(1) means the condition should be true for one day every given frequency

        Value is between 1 and 7
        """

        def __init__(self, value: int):
            pass

    class Week:
        """
        Class used to set a weekly frequency or a duration of weeks in svshi api's check_time_property function
        Example: A frequency set to Week(3) means the condition should be true for a give duration every three weeks
        Example: A duration set to Week(2) means the condition should be true for two weeks every given frequency

        Value is between 1 and 4
        """

        def __init__(self, value: int):
            pass

    class Month:
        """
        Class used to set a monthly frequency or a duration of months in svshi api's check_time_property function
        Example: A frequency set to Month(5) means the condition should be true for a give duration every five months
        Example: A duration set to Month(2) means the condition should be true for two months every given frequency

        Value is between 1 and 12
        """

        def __init__(self, value: int):
            pass

    def check_time_property(self, frequency, duration, condition: bool) -> bool:
        """
        This function can be used to check time dependent conditions in the apps invariants. It is designed to
        check that for every time period given by the frequency, a condition is True for a certain amount of time,
        given by the duration.
        Example: Check that a window is open for 30 minutes every day
        :param frequency: Either svshi_api's Minute, Hour, Day, Week or Month, depending
                          on how often the condition should hold
        :param duration: Either svshi_api's Minute, Hour, Day, Week or Month, depending
                          on how long the condition should hold
        :param condition: A condition on the svshi system that should hold for the given duration of
                          time within the given frequency
        :return: True if the condition was True for the given duration in the given frequency of time
        """
        pass
