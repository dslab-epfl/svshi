import ast
import astor
from typing import List, Union


class Manipulator:

    __STATE_ARGUMENT = "physical_state"
    __STATE_TYPE = "PhysicalState"

    def __init__(self, app_names: List[str]):
        self.__app_names = app_names

    def __rename_instances_add_state(
        self,
        op: Union[
            ast.stmt,
            ast.expr,
            ast.comprehension,
            List[ast.stmt],
            List[ast.expr],
            List[ast.comprehension],
        ],
        app_name: str,
        accepted_names: List[str],
    ):
        if isinstance(op, list) or isinstance(op, tuple):
            [
                self.__rename_instances_add_state(v, app_name, accepted_names)
                for v in list(op)
            ]
        elif isinstance(op, ast.List) or isinstance(op, ast.Tuple):
            self.__rename_instances_add_state(op.elts, app_name, accepted_names)
        elif isinstance(op, ast.BoolOp):
            self.__rename_instances_add_state(op.values, app_name, accepted_names)
        elif isinstance(op, ast.UnaryOp):
            self.__rename_instances_add_state(op.operand, app_name, accepted_names)
        elif isinstance(op, ast.NamedExpr):
            self.__rename_instances_add_state(op.value, app_name, accepted_names)
        elif isinstance(op, ast.Lambda):
            self.__rename_instances_add_state(op.body, app_name, accepted_names)
        elif isinstance(op, ast.Assign):
            self.__rename_instances_add_state(op.value, app_name, accepted_names)
        elif isinstance(op, ast.Return):
            if op.value:
                self.__rename_instances_add_state(op.value, app_name, accepted_names)
        elif isinstance(op, ast.Compare):
            self.__rename_instances_add_state(op.left, app_name, accepted_names)
            self.__rename_instances_add_state(op.comparators, app_name, accepted_names)
        elif isinstance(op, ast.BinOp):
            self.__rename_instances_add_state(op.left, app_name, accepted_names)
            self.__rename_instances_add_state(op.right, app_name, accepted_names)
        elif isinstance(op, ast.IfExp):
            self.__rename_instances_add_state(op.test, app_name, accepted_names)
            self.__rename_instances_add_state(op.body, app_name, accepted_names)
            self.__rename_instances_add_state(op.orelse, app_name, accepted_names)
        elif isinstance(op, ast.Dict):
            self.__rename_instances_add_state(op.values, app_name, accepted_names)
        elif isinstance(op, ast.Set):
            self.__rename_instances_add_state(op.elts, app_name, accepted_names)
        elif isinstance(op, ast.comprehension):
            self.__rename_instances_add_state(op.iter, app_name, accepted_names)
            self.__rename_instances_add_state(op.ifs, app_name, accepted_names)
        elif (
            isinstance(op, ast.ListComp)
            or isinstance(op, ast.SetComp)
            or isinstance(op, ast.GeneratorExp)
        ):
            self.__rename_instances_add_state(op.generators, app_name, accepted_names)
        elif isinstance(op, ast.DictComp):
            self.__rename_instances_add_state(op.value, app_name, accepted_names)
            self.__rename_instances_add_state(op.generators, app_name, accepted_names)
        elif isinstance(op, ast.Yield) or isinstance(op, ast.YieldFrom):
            if op.value:
                self.__rename_instances_add_state(op.value, app_name, accepted_names)
        elif isinstance(op, ast.FormattedValue):
            self.__rename_instances_add_state(op.value, app_name, accepted_names)
        elif isinstance(op, ast.JoinedStr):
            self.__rename_instances_add_state(op.values, app_name, accepted_names)
        elif isinstance(op, ast.Call):
            self.__rename_instances_add_state(op.args, app_name, accepted_names)
            f_name = ""
            if isinstance(op.func, ast.Attribute):
                # op.func.value is a ast.Name in this case
                f_name = op.func.value.id
            elif isinstance(op.func, ast.Name):
                f_name = op.func.id
            else:
                self.__rename_instances_add_state(op.func, app_name, accepted_names)

            if f_name in accepted_names:
                # If the function name is in the list of accepted names, add the state argument to the call
                op.args.append(ast.Name(self.__STATE_ARGUMENT, ast.Load))

                # Rename
                new_name = f"{app_name.upper()}_{f_name}"
                if isinstance(op.func, ast.Attribute):
                    op.func.value.id = new_name
                elif isinstance(op.func, ast.Name):
                    op.func.id = new_name

        elif isinstance(op, ast.FunctionDef) and (
            op.name == "precond" or op.name == "iteration"
        ):
            annotation = ast.Name(self.__STATE_TYPE, ast.Load)
            op.args.args.append(ast.arg(self.__STATE_ARGUMENT, annotation))

            # Rename
            op.name = f"{app_name}_{op.name}"
            self.__rename_instances_add_state(op.body, app_name, accepted_names)

    def add_state_to_file(self, filename: str, accepted_names: List[str]):
        """
        Parses the given file, adds the state argument to all the function calls with a name
        in the given list of names, then overwrites the file with the new source code.
        """
        with open(filename, "r+") as file:
            p = ast.parse(file.read())
            self.__rename_instances_add_state(p.body, "test", accepted_names)
            modified = astor.to_source(p)
            file.seek(0)
            file.write(modified)
            file.truncate()
