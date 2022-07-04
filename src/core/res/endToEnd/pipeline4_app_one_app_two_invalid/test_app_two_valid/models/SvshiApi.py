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
