import json
from typing import List, Tuple


class Parser:
    """
    Parser.
    """

    def __init__(self, group_addresses_filename: str):
        self.__group_addresses_filename = group_addresses_filename

    def parse_group_addresses(self) -> List[Tuple[str, str]]:
        """
        Parses the group addresses file, returning a list of (address, type) pairs.
        """
        with open(self.__group_addresses_filename, "r") as file:
            addrs_dict = json.load(file)
            return [(ga["address"], ga["type"]) for ga in addrs_dict["addresses"]]
