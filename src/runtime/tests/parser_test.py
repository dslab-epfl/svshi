from xknx.dpt.dpt import DPTBinary
from xknx.dpt.dpt_2byte_float import DPT2ByteFloat
from xknx.dpt.dpt_string import DPTString
from ..parser import GroupAddressesParser

GROUP_ADDRESSES_JSON_PATH = "tests/fake_app_library/group_addresses.json"


def test_parser_read_group_addresses_dpt():
    parser = GroupAddressesParser(GROUP_ADDRESSES_JSON_PATH)

    dpts = parser.read_group_addresses_dpt()

    assert isinstance(dpts["1/1/1"], DPTBinary) == True
    assert isinstance(dpts["1/1/2"], DPT2ByteFloat) == True
    assert isinstance(dpts["1/1/3"], DPTString) == True
