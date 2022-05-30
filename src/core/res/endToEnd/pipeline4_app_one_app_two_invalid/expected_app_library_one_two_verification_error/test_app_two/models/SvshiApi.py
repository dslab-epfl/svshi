from io import TextIOWrapper
from typing import IO, Optional


class SvshiApi:
    def __init__(self):
        pass

    def get_time(self) -> int:
        """
        get the machine's time (ex 1649865437 for Wed Apr 13 15:57:17 2022 UTC)
        :returns the current UNIX time of the machine
        """
        pass

    def get_hour_of_the_day(self) -> int:
        """
        get the machine's hour in 24 hours format (return is between 0 and 23)
        example : 15 for Wed Apr 13 15:57:17 2022 UTC
        :returns the current hour based on the machine's time
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