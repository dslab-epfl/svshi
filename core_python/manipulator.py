import ast
import astor
import itertools
from typing import List, Tuple, Union


class Manipulator:

    __STATE_ARGUMENT = "physical_state"
    __STATE_TYPE = "PhysicalState"

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

                # Rename the instance calling the function, adding the app name to it
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

            # Rename the function, adding the app name to it
            op.name = f"{app_name}_{op.name}"
            self.__rename_instances_add_state(op.body, app_name, accepted_names)

    def manipulate_app_main(
        self, app_name: str, accepted_names: List[str]
    ) -> Tuple[List[str], str]:
        """
        Manipulates the `main.py` of the given file, modifying the names of the functions and instances (specified in `accepted_names`),
        and adding the state argument to the calls. Then, the `precond` and `iteration` functions are extracted, together with their imports,
        and dumps them in the verification file.
        """

        def flatmap(func, *iterable):
            return itertools.chain.from_iterable(map(func, *iterable))

        def extract_functions_and_imports(module_body: List[ast.stmt]):
            functions_ast = list(
                filter(
                    lambda n: isinstance(n, ast.FunctionDef)
                    and (
                        n.name == f"{app_name}_precond"
                        or n.name == f"{app_name}_iteration"
                    ),
                    module_body,
                )
            )
            imports_ast = list(
                filter(
                    lambda n: isinstance(n, ast.Import)
                    and (
                        set(flatmap(lambda a: [a.name], n.names))
                        - set(["asyncio", "time"])
                    ),
                    module_body,
                )
            )
            from_imports_ast = list(
                filter(
                    lambda n: isinstance(n, ast.ImportFrom)
                    and not n.module in ["devices", "communication.client"],
                    module_body,
                )
            )
            return functions_ast, imports_ast, from_imports_ast

        with open(f"generated/{app_name}/main.py", "r") as file:
            p = ast.parse(file.read())
            module_body = p.body
            self.__rename_instances_add_state(module_body, app_name, accepted_names)
            (
                functions_ast,
                imports_ast,
                from_imports_ast,
            ) = extract_functions_and_imports(module_body)
            functions = astor.to_source(ast.Module(functions_ast))
            imports = astor.to_source(ast.Module(imports_ast))
            from_imports = astor.to_source(ast.Module(from_imports_ast))
            return [imports, from_imports], functions
