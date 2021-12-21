import json
from typing import Dict, Union
from xknx.dpt.dpt import DPTBase, DPTBinary
from xknx.dpt.dpt_1byte_signed import DPTSignedRelativeValue
from xknx.dpt.dpt_1byte_uint import DPTValue1ByteUnsigned
from xknx.dpt.dpt_2byte_float import DPT2ByteFloat
from xknx.dpt.dpt_2byte_signed import DPT2ByteSigned
from xknx.dpt.dpt_2byte_uint import DPT2ByteUnsigned
from xknx.dpt.dpt_4byte_float import DPT4ByteFloat
from xknx.dpt.dpt_4byte_int import DPT4ByteUnsigned, DPT4ByteSigned
from xknx.dpt.dpt_date import DPTDate
from xknx.dpt.dpt_datetime import DPTDateTime
from xknx.dpt.dpt_hvac_mode import DPTHVACMode
from xknx.dpt.dpt_string import DPTString
from xknx.dpt.dpt_time import DPTTime


class GroupAddressesParser:
    """
    JSON parser for group addresses file.
    """

    __DPT_DICT: Dict[int, Union[DPTBase, DPTBinary]] = {
        1: DPTBinary(0),  # the value is not used
        5: DPTValue1ByteUnsigned(),
        6: DPTSignedRelativeValue(),
        7: DPT2ByteUnsigned(),
        8: DPT2ByteSigned(),
        9: DPT2ByteFloat(),
        10: DPTTime(),
        11: DPTDate(),
        12: DPT4ByteUnsigned(),
        13: DPT4ByteSigned(),
        14: DPT4ByteFloat(),
        16: DPTString(),
        19: DPTDateTime(),
        20: DPTHVACMode(),  # DPT 20.102 HVAC Mode
    }

    def __init__(self, group_addresses_json_path: str):
        self.__group_addresses_json_path = group_addresses_json_path

    def read_group_addresses_dpt(self) -> Dict[str, Union[DPTBase, DPTBinary]]:
        """
        Reads the DPT class associated to each group address.
        """
        with open(self.__group_addresses_json_path, "r") as file:
            file_dict = json.load(file)
            addresses = file_dict["addresses"]
            group_addresses_dpt: Dict[str, Union[DPTBase, DPTBinary]] = {}
            for address_pair in addresses:
                address = address_pair[0]
                dpt_number = int(address_pair[2].split("-")[1])
                group_addresses_dpt[address] = self.__DPT_DICT[dpt_number]
            return group_addresses_dpt
