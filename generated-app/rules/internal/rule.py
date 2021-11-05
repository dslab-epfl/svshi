from abc import ABC


class Rule(ABC):
    """
    A rule.
    """

    def AND(self, rule: "Rule") -> "Rule":
        """
        Computes the AND between this rule and the given one.
        """
        return __And(self, rule)

    def OR(self, rule: "Rule") -> "Rule":
        """
        Computes the OR between this rule and the given one.
        """
        return __Or(self, rule)

    def NOT(self) -> "Rule":
        """
        Computes the NOT expression of the given rule.
        """
        return __Not(self)


class __If(Rule):
    """
    An IF rule.
    """

    def __init__(self, if_condition: "Rule", then_rule: "Rule", else_rule: "Rule"):
        self.if_condition = if_condition
        self.then_rule = then_rule
        self.else_rule = else_rule


class __Then:
    def __init__(self, if_condition: "Rule"):
        self.if_condition = if_condition

    def THEN(self, then_rule: "Rule") -> "__IfThen":
        """
        Returns an IfThen that can be used as a rule or completed with an ELSE condition.
        """
        return __IfThen(self.if_condition, then_rule)


class __IfThen(Rule):
    def __init__(self, if_condition: "Rule", then_rule: "Rule"):
        self.if_condition = if_condition
        self.then_rule = then_rule

    def ELSE(self, else_rule: Rule) -> __If:
        """
        Completes the IfThen with an ELSE condition, returning an IF rule.
        """
        return __If(self.if_condition, self.then_rule, else_rule)


class __Expression(Rule):
    """
    A boolean expression rule.
    """

    def __init__(self, expression: bool):
        self.expression = expression


class __Or(Rule):
    """
    An OR rule.
    """

    def __init__(self, rule1: "Rule", rule2: "Rule"):
        self.rule1 = rule1
        self.rule2 = rule2


class __Not(Rule):
    """
    A NOT rule.
    """

    def __init__(self, rule):
        self.rule = rule


class __And(Rule):
    """
    An AND rule.
    """

    def __init__(self, rule1: "Rule", rule2: "Rule"):
        self.rule1 = rule1
        self.rule2 = rule2


def IF(if_condition: "Rule") -> __Then:
    """
    Returns a Then that can be used to write an IF rule.
    """
    return __Then(if_condition)


def AND(rule1: "Rule", rule2: "Rule") -> __And:
    """
    Returns an AND of the given rules.
    """
    return __And(rule1, rule2)


def OR(rule1: "Rule", rule2: "Rule") -> __Or:
    """
    Returns an OR of the given rules.
    """
    return __Or(rule1, rule2)


def NOT(rule) -> __Not:
    """
    Returns a Not of the given rule.
    """
    return __Not(rule)


def EXPR(expression: bool) -> __Expression:
    """
    Returns an Expression wrapping the given boolean.
    """
    return __Expression(expression)
