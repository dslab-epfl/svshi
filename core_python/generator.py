from typing import List, Tuple


class Generator:
    """
    Code generator.
    """

    def __init__(self, filename: str, group_addresses: List[Tuple[str, str]]):
        self.__filename: str = filename
        self.__group_addresses: List[Tuple[str, str]] = group_addresses
        self.__code: List[str] = []
        self.__imports: List[str] = []

    def __generate_physical_state_class(self):
        fields = ""
        for (name, typee) in self.__group_addresses:
            fields += f" GA_{name.replace('/', '_')}: {typee}\n"

        code = f"""
@dataclasses.dataclass
class PhysicalState:
{fields}
"""
        self.__code.append(code)
        self.__imports.append("import dataclasses")

    def generate_verification_file(self):
        """
        Generates the whole verification file.
        """
        with open(self.__filename, "w") as file:
            self.__generate_physical_state_class()
            file.write("\n".join(self.__imports))
            file.write("\n")
            file.write("\n".join(self.__code))
