class Device:
    """
    A parsed device.
    """

    def __init__(self, name: str, type: str, import_module_name: str):
        self.name = name
        self.type = type
        self.import_module_name = import_module_name

    def __eq__(self, other):
        if isinstance(other, Device):
            return (
                self.name == other.name
                and self.type == other.type
                and self.import_module_name == other.import_module_name
            )
        return False
