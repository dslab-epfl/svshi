import dataclasses
from typing import Callable, List


@dataclasses.dataclass
class App:
    name: str
    priority: int
    code: Callable[[], None]
    should_run: bool


def load_apps() -> List[App]:
    return []
