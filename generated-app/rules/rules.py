from internal.rule import EXPR, IF, NOT

RULES = [
    # Write your rules here
    IF(EXPR(1 == 1).AND(EXPR("a" == "b")))
    .THEN(NOT(EXPR("a" != "c")))
    .ELSE(EXPR(1 == 5))
]
