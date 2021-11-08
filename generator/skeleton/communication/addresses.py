###
### DO NOT TOUCH THIS FILE!!!
###

import json


def __read_group_addresses(filename: str) -> dict:
    with open(filename, "r") as file:
        file_dict = json.load(file)
        addresses = {}
        for address in file_dict["addresses"]:
            channel_name = address["channelName"]
            group_address = address["groupAddress"]
            addresses[channel_name] = group_address

        return addresses


GROUP_ADDRESSES = __read_group_addresses("addresses.json")
