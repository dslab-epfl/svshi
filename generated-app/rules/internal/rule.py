from abc import ABC
from rule import Rule, Or, And, Not, Expression, If


class IfThen(Rule):
    def __init__(self, ifCondition: Rule, thenRule: Rule):
        self.ifCondition = ifCondition
        self.thenRule = thenRule

    def ELSE(self, elseRule: Rule) -> If:
        return If(self.ifCondition, self.thenRule, elseRule)


class Then:
    def __init__(self, ifCondition: Rule):
        self.ifCondition = ifCondition

    def THEN(self, thenRule: Rule) -> IfThen:
        return IfThen(self.ifCondition, thenRule)


class If(Rule):
    def __init__(self, ifCondition: Rule, thenRule: Rule, elseRule: Rule):
        self.ifCondition = ifCondition
        self.thenRule = thenRule
        self.elseRule = elseRule


class Expression(Rule):
    def __init__(self, expression: bool):
        self.expression = expression


class Or(Rule):
    def __init__(self, rule1: Rule, rule2: Rule):
        self.rule1 = rule1
        self.rule2 = rule2


class Not(Rule):
    def __init__(self, rule):
        self.rule = rule


class And(Rule):
    def __init__(self, rule1: Rule, rule2: Rule):
        self.rule1 = rule1
        self.rule2 = rule2


class Rule(ABC):
    def AND(self, rule: Rule) -> Rule:
        return And(self, rule)

    def OR(self, rule: Rule) -> Rule:
        return Or(self, rule)

    def NOT(self) -> Rule:
        return Not(self)


def IF(ifCondition: Rule) -> Then:
    return Then(ifCondition)


def AND(rule1: Rule, rule2: Rule) -> And:
    return And(rule1, rule2)


def OR(rule1: Rule, rule2: Rule) -> Or:
    return Or(rule1, rule2)


def NOT(rule) -> Not:
    return Not(rule)


def EXPR(expression: bool) -> Expression:
    return Expression(expression)
