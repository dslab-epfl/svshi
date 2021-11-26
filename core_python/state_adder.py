import ast
import astor
from typing import List, Union

__STATE_ARGUMENT = "physical_state"
__STATE_TYPE = "PhysicalState"


def __recursively_add_state_argument(
    op: Union[
        ast.stmt,
        ast.expr,
        ast.comprehension,
        List[ast.stmt],
        List[ast.expr],
        List[ast.comprehension],
    ],
    accepted_names: List[str],
):
    if isinstance(op, list) or isinstance(op, tuple):
        [__recursively_add_state_argument(v, accepted_names) for v in list(op)]
    elif isinstance(op, ast.List) or isinstance(op, ast.Tuple):
        __recursively_add_state_argument(op.elts, accepted_names)
    elif isinstance(op, ast.BoolOp):
        __recursively_add_state_argument(op.values, accepted_names)
    elif isinstance(op, ast.UnaryOp):
        __recursively_add_state_argument(op.operand, accepted_names)
    elif isinstance(op, ast.NamedExpr):
        __recursively_add_state_argument(op.value, accepted_names)
    elif isinstance(op, ast.Lambda):
        __recursively_add_state_argument(op.body, accepted_names)
    elif isinstance(op, ast.Assign):
        __recursively_add_state_argument(op.value, accepted_names)
    elif isinstance(op, ast.Return):
        if op.value:
            __recursively_add_state_argument(op.value, accepted_names)
    elif isinstance(op, ast.Compare):
        __recursively_add_state_argument(op.left, accepted_names)
        __recursively_add_state_argument(op.comparators, accepted_names)
    elif isinstance(op, ast.BinOp):
        __recursively_add_state_argument(op.left, accepted_names)
        __recursively_add_state_argument(op.right, accepted_names)
    elif isinstance(op, ast.IfExp):
        __recursively_add_state_argument(op.test, accepted_names)
        __recursively_add_state_argument(op.body, accepted_names)
        __recursively_add_state_argument(op.orelse, accepted_names)
    elif isinstance(op, ast.Dict):
        __recursively_add_state_argument(op.values, accepted_names)
    elif isinstance(op, ast.Set):
        __recursively_add_state_argument(op.elts, accepted_names)
    elif isinstance(op, ast.comprehension):
        __recursively_add_state_argument(op.iter, accepted_names)
        __recursively_add_state_argument(op.ifs, accepted_names)
    elif (
        isinstance(op, ast.ListComp)
        or isinstance(op, ast.SetComp)
        or isinstance(op, ast.GeneratorExp)
    ):
        __recursively_add_state_argument(op.generators, accepted_names)
    elif isinstance(op, ast.DictComp):
        __recursively_add_state_argument(op.value, accepted_names)
        __recursively_add_state_argument(op.generators, accepted_names)
    elif isinstance(op, ast.Yield) or isinstance(op, ast.YieldFrom):
        if op.value:
            __recursively_add_state_argument(op.value, accepted_names)
    elif isinstance(op, ast.FormattedValue):
        __recursively_add_state_argument(op.value, accepted_names)
    elif isinstance(op, ast.JoinedStr):
        __recursively_add_state_argument(op.values, accepted_names)
    elif isinstance(op, ast.Call):
        __recursively_add_state_argument(op.args, accepted_names)
        f_name = ""
        if isinstance(op.func, ast.Attribute):
            # op.func.value is a ast.Name in this case
            f_name = op.func.value.id
        elif isinstance(op.func, ast.Name):
            f_name = op.func.id
        else:
            __recursively_add_state_argument(op.func, accepted_names)

        if f_name in accepted_names:
            # If the function name is in the list of accepted names, add the state argument to the call
            op.args.append(ast.Name(__STATE_ARGUMENT, ast.Load))
    elif isinstance(op, ast.FunctionDef) and (
        op.name == "precond" or op.name == "iteration"
    ):
        annotation = ast.Name(__STATE_TYPE, ast.Load)
        op.args.args.append(ast.arg(__STATE_ARGUMENT, annotation))
        __recursively_add_state_argument(op.body, accepted_names)


def add_state_to_file(filename: str, accepted_names: List[str]):
    """
    Parses the given file, adds the state argument to all the function calls with a name
    in the given list of names, then overwrites the file with the new source code.
    """
    with open(filename, "r+") as file:
        p = ast.parse(file.read())
        __recursively_add_state_argument(p.body, accepted_names)
        modified = astor.to_source(p)
        file.seek(0)
        file.write(modified)
        file.truncate()
