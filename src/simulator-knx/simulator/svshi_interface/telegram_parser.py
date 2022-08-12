"""
Class that allows translation of telegrams from KNX to simulated and oppositely
"""

import sys
import logging
from typing import Dict, Final, Union
from numpy import isin

from xknx.dpt.dpt import DPTArray, DPTBinary, DPTNumeric
from xknx.telegram.apci import GroupValueWrite
from xknx.telegram.telegram import Telegram
from xknx.telegram.address import GroupAddress, IndividualAddress, GroupAddressType
from xknx.dpt.dpt_4byte_float import DPT4ByteFloat
from xknx.dpt.dpt_2byte_float import DPT2ByteFloat
from xknx.dpt.dpt_1byte_uint import DPTValue1ByteUnsigned
from xknx.xknx import XKNX
import math

import system.telegrams as sim_t

sys.path.append(".")
sys.path.append("..")


class TelegramParser:
    """Class that implements a parser for telegrams, from simulated telegrams to real telegrams and the other way around"""

    # TODO : Here the dictionary is taken and used below
    def __init__(self) -> None:
        """Initializes an instance of a telegram parser"""
        self.group_address_to_payload: Dict[
            str, sim_t.Payload
        ] = {}  # dict set later when group addr are parsed from file
        self.payload_to_dpt: Dict[sim_t.Payload, Union[DPTBinary, DPTArray]] = {
            sim_t.FloatPayload: DPTArray,
            sim_t.BinaryPayload: DPTBinary,
            sim_t.DimmerPayload: DPTArray,
        }

        # KNX Communication variables
        self.__TRUE: Final = 1
        self.__FALSE: Final = 0

        self.__FREE: Final = GroupAddressType.FREE
        self.__SHORT: Final = GroupAddressType.SHORT
        self.__LONG: Final = GroupAddressType.LONG

        self.__sim_encoding_to_xknx: Final = {
            "free": self.__FREE,
            "2-levels": self.__SHORT,
            "3-levels": self.__LONG,
        }

    def from_knx_telegram(self, telegram: Telegram) -> Union[sim_t.Telegram, None]:
        """Creates a simulator telegram from a knx telegram if possible"""
        from system import GroupAddress, IndividualAddress

        payload = telegram.payload

        print(
            "In from_knx_telegram, payload = ",
            payload,
            " address = ",
            telegram.destination_address,
        )
        ga_split = str(telegram.destination_address).split("/")

        if telegram.destination_address.levels == self.__sim_encoding_to_xknx.get(
            "free"
        ):
            address = GroupAddress("free", ga_split[0])
        elif telegram.destination_address.levels == self.__sim_encoding_to_xknx.get(
            "2-levels"
        ):
            address = GroupAddress("2-levels", ga_split[0], sub=ga_split[1])
        else:
            address = GroupAddress("3-levels", ga_split[0], ga_split[1], ga_split[2])

        source = IndividualAddress(
            telegram.source_address.area,
            telegram.source_address.main,
            telegram.source_address.line,
        )
        output = None

        if isinstance(payload, GroupValueWrite):
            # For the moment, similarly to SVSHI, we only support GroupValueWrtie as there is no reading involved
            v = payload.value

            if v:
                # We assume that we receive by default a Binary value
                # TODO Maybe remove the default value to not have this kind of surprise later again?
                print(self.group_address_to_payload)
                # dpt = self.payload_to_dpt.get(
                #     self.group_address_to_payload.get(
                #         str(address), sim_t.BinaryPayload
                #     ),
                #     None,
                # )
                dpt = self.payload_to_dpt.get(
                    self.group_address_to_payload.get(str(address))
                )
                print("In from_knx_telegram, dpt = ", dpt)
                if dpt == DPTBinary:
                    payload = self.group_address_to_payload.get(
                        str(address), sim_t.BinaryPayload
                    )(binary_state=True if v.value == self.__TRUE else False)
                    output = sim_t.Telegram(source, address, payload)

                elif dpt == DPTArray:
                    payload_type = self.group_address_to_payload.get(
                        str(address), sim_t.BinaryPayload
                    )
                    print("In from_knx_telegram, payload type = ", payload_type)
                    if payload_type == sim_t.DimmerPayload:
                        decoder = DPTValue1ByteUnsigned()
                        conv_v = decoder.from_knx(v.value)  # 0 <= x <= 255
                        print("In from_knx_telegram, conv_v = ", conv_v)
                        mapped_0_100 = (conv_v / 255.0) * 100
                        print("In from_knx_telegram, mapped = ", mapped_0_100)
                        binary_state = mapped_0_100 != 0
                        print("In from_knx_telegram, binary_state = ", binary_state)
                        payload = sim_t.DimmerPayload(
                            binary_state=binary_state, state_ratio=mapped_0_100
                        )
                        print("In from_knx_telegram, payload = ", payload_type)
                        output = sim_t.Telegram(source, address, payload)
                    elif payload_type == sim_t.FloatPayload:
                        decoder = DPT2ByteFloat()

                        conv_v = decoder.from_knx(v.value)
                        payload = sim_t.FloatPayload(conv_v)
                        output = sim_t.Telegram(source, address, payload)
                else:  # if dpt is None
                    logging.warning("The DataType is neither Binary nor Array.")
        return output

    def from_simulator_telegram(
        self, telegram: sim_t.Telegram
    ) -> Union[Telegram, None]:
        """Creates a knx telegram from a simulator telegram, if possible"""
        address = str(telegram.destination)
        payload = telegram.payload

        encoding = telegram.destination.encoding_style

        dpt = None
        value = None
        if payload.content is not None:
            if isinstance(payload, sim_t.BinaryPayload) and not isinstance(
                payload, sim_t.DimmerPayload
            ):
                dpt = self.payload_to_dpt.get(sim_t.BinaryPayload)
                value = payload.content

            elif isinstance(payload, sim_t.FloatPayload):
                dpt = self.payload_to_dpt.get(sim_t.FloatPayload)
                decoder = DPT2ByteFloat()
                value = decoder.to_knx(payload.content)
            elif isinstance(payload, sim_t.DimmerPayload):
                # Convert to DPT5 becasue XKNx (and thus SVSHI) only support this one
                # So if state = False, send 0
                # else send the float (0 <= x <= 100) mapped to uint 0 <= y <= 255
                dpt = self.payload_to_dpt.get(sim_t.DimmerPayload)
                decoder = DPTValue1ByteUnsigned()
                mapped = 0
                if payload.content:
                    mapped = round((payload.state_ratio / 100) * 255)
                value = decoder.to_knx(mapped)

        else:
            return None

        if dpt != None and value != None:

            if dpt == DPTBinary:
                binary_value = self.__TRUE if value else self.__FALSE
                write_content = DPTBinary(value=binary_value)
            else:
                write_content = DPTArray(value)
            ga = GroupAddress(address)
            ga.levels = self.__sim_encoding_to_xknx.get(encoding)

            newTelegram = Telegram(
                source_address=IndividualAddress(
                    f"{telegram.source.area}.{telegram.source.line}.{telegram.source.device}"
                ),
                destination_address=ga,
                payload=GroupValueWrite(write_content),
            )
            return newTelegram
        else:
            return None
