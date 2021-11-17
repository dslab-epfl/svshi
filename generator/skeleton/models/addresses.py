###
### DO NOT TOUCH THIS FILE!!!
###

import json


def __read_group_addresses(filename: str) -> dict:
    with open(filename, "r") as file:
        file_dict = json.load(file)
        addresses = {}
        for address in file_dict["addresses"]:
            device_name = address["name"]
            addresses[device_name] = address

        return addresses


GROUP_ADDRESSES = __read_group_addresses("../assignments/addresses.json")
