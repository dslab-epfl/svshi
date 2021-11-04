from internal.rule import EXPR, IF, NOT, OR, AND, Rule
from typing import List

# The rules that always have to be satisfied in the system
RULES: List[Rule] = [
    # Write your rules here
    # Example:
    # IF(EXPR(1 == 1).AND(EXPR("a" == "b")))
    # .THEN(NOT(EXPR("a" != "c")))
    # .ELSE(EXPR(1 == 5))
]
