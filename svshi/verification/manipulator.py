import ast
import astor
from typing import Dict, List, Set, Tuple, Union, cast


class InvalidUncheckedFunctionCall(Exception):
    """
    An invalid unchecked function call exception.
    """


class Manipulator:
    """
    Python AST manipulator.
    """

    __STATE_ARGUMENT = "physical_state"
    __STATE_TYPE = "PhysicalState"
    __UNCHECKED_FUNC_PREFIX = "unchecked_"
    __PRECOND_FUNC_NAME = "precond"
    __ITERATION_FUNC_NAME = "iteration"

    def __init__(
        self, instances_names_per_app: Dict[Tuple[str, str], Set[str]]
    ) -> None:
        self.__app_names = list(map(lambda t: t[1], instances_names_per_app.keys()))
        self.__instances_names_per_app = instances_names_per_app

    
    def __get_unchecked_functions(
            self,
            op: Union[
                ast.stmt,
                ast.expr,
                ast.comprehension,
                List[ast.stmt],
                List[ast.expr],
                List[ast.comprehension],
            ],
        ) -> Dict[str, str]:
        """
            Go through the AST and return a Dict containing
            "unchecked_func_name" -> "doc_string"
        """
        if isinstance(op, list) or isinstance(op, tuple):
            dicts_list = [
                self.__get_unchecked_functions(v)
                for v in list(op)
            ]
            res = {} 
            for d in dicts_list:
                for k in d.keys():
                    res[k] = d[k]
            return res
        elif isinstance(op, ast.List) or isinstance(op, ast.Tuple):
            return self.__get_unchecked_functions(op.elts)
        elif isinstance(op, ast.FunctionDef) and (
            op.name.startswith(self.__UNCHECKED_FUNC_PREFIX)
        ):
            doc_string = ast.get_docstring(op)
            doc_string = "" if doc_string == None else doc_string
            return {op.name: doc_string}

    def __rename_instances_add_state(
        self,
        op: Union[
            ast.stmt,
            ast.expr,
            ast.comprehension,
            ast.keyword,
            List[ast.stmt],
            List[ast.expr],
            List[ast.comprehension],
            List[ast.keyword],
        ],
        app_name: str,
        accepted_names: Set[str],
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
        elif isinstance(op, ast.NamedExpr) or isinstance(op, ast.Expr):
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
        elif isinstance(op, ast.IfExp) or isinstance(op, ast.If):
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
        elif isinstance(op, ast.Return):
            if op.value:
                self.__rename_instances_add_state(op.value, app_name, accepted_names)
        elif isinstance(op, ast.Call):
            self.__rename_instances_add_state(op.args, app_name, accepted_names)
            self.__rename_instances_add_state(op.keywords, app_name, accepted_names)
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
        elif isinstance(op, ast.keyword):
            self.__rename_instances_add_state(op.value, app_name, accepted_names)
        elif isinstance(op, ast.FunctionDef) and (
            op.name == self.__PRECOND_FUNC_NAME or op.name == self.__ITERATION_FUNC_NAME
        ):
            annotation = ast.Name(self.__STATE_TYPE, ast.Load)
            op.args.args.append(ast.arg(self.__STATE_ARGUMENT, annotation))

            # Rename the function, adding the app name to it
            op.name = f"{app_name}_{op.name}"
            self.__rename_instances_add_state(op.body, app_name, accepted_names)
        elif isinstance(op, ast.FunctionDef) and (
            op.name.startswith(self.__UNCHECKED_FUNC_PREFIX)
        ):
            # TODO
            pass

    def __construct_contracts(self, app_names: List[str], unchecked_apps_dict: Dict[str, str]) -> str:
        pre_str = "pre: "
        post_str = "post: "
        precond_name_str = "precond"
        return_value_name_str = "__return__"
        physical_state_name_str = "physical_state"

        def construct_func_call(func_name: str, arg_names: List[str]) -> str:
            res = func_name + "("
            for i in range(len(arg_names)):
                res += arg_names[i] + ", "
            if len(res) > 2:
                res = res[:-2]
            res += ")"
            return res
        def construct_pre_unchecked_func(unchecked_func_name: str, doc_string: str) -> List[str]:
            list_post_conds = []
            post_str_no_space = "post:"
            for l in doc_string.splitlines():
                if post_str_no_space in l:
                    cond = pre_str + l.replace(post_str_no_space, "").replace(return_value_name_str, unchecked_func_name)
                    list_post_conds.append(cond)
            return list_post_conds


        conditions = []
        sorted_app_names = sorted(app_names)
        for app_name in sorted_app_names:
            precond = pre_str
            precond += construct_func_call(
                app_name + "_" + precond_name_str, [physical_state_name_str]
            )
            conditions.append(precond)
        
        for unchecked_app_name, doc_string in unchecked_apps_dict.items():
            conditions.extend(construct_pre_unchecked_func(unchecked_app_name, doc_string))
            
        for app_name in sorted_app_names:
            postcond = post_str
            postcond += construct_func_call(
                app_name + "_" + precond_name_str, [return_value_name_str]
            )
            conditions.append(postcond)
        res = "\n".join(conditions)
        return res

    def __add_doc_string(self, f: ast.FunctionDef, doc_string: str) -> ast.FunctionDef:
        old_doc_string = ast.get_docstring(f)
        s = ast.Str("\n" + doc_string + "\n")
        new_doc_string_ast = ast.Expr(value=s)
        if old_doc_string:
            f.body[0] = new_doc_string_ast
        else:
            f.body.insert(0, new_doc_string_ast)
        return f

    def __add_return_state(self, f: ast.FunctionDef) -> ast.FunctionDef:
        f.body.append(ast.Return(ast.Name("physical_state", ast.Load)))
        return f

    def __change_unchecked_func_in_iteration(self, iteration_func: ast.FunctionDef, unchecked_function_names: Set[str]) -> ast.FunctionDef:
        """
        Manipulates the iteration function to replace all calls to unchecked functions by a variable with the same name. It also adds
        this variables as arguments
        """
        pass

    def manipulate_mains(self) -> Tuple[List[str], List[str]]:
        """
        Manipulates the `main.py` of all the apps, modifying the names of the functions and instances (specified in `accepted_names`),
        and adding the state argument to the calls. Then, the `precond` and `iteration` functions are extracted, together with their imports,
        and dumped in the verification file.
        """
        imports = []
        functions = []
        for (
            directory,
            app_name,
        ), accepted_names in sorted(self.__instances_names_per_app.items()):
            imps, funcs = self.__manipulate_app_main(
                directory, app_name, accepted_names
            )
            imports.extend(imps)
            functions.append(funcs)

        # Keep only non-empty imports
        return [imp.replace("\n", "") for imp in imports if imp], functions

    def __check_no_unchecked_calls_in_precond(
        self, functions: List[ast.FunctionDef], unchecked_func_names: Set[str]
    ) -> Tuple[bool, str]:
        def check(
            op: Union[
                ast.stmt,
                ast.expr,
                ast.comprehension,
                ast.keyword,
                List[ast.stmt],
                List[ast.expr],
                List[ast.comprehension],
                List[ast.keyword],
            ]
        ) -> bool:
            if isinstance(op, list) or isinstance(op, tuple):
                for o in [check(v) for v in list(op)]:
                    if not o:
                        return False
                return True
            elif isinstance(op, ast.List) or isinstance(op, ast.Tuple):
                return check(op.elts)
            elif isinstance(op, ast.BoolOp):
                return check(op.values)
            elif isinstance(op, ast.UnaryOp):
                return check(op.operand)
            elif isinstance(op, ast.NamedExpr) or isinstance(op, ast.Expr):
                return check(op.value)
            elif isinstance(op, ast.Lambda):
                return check(op.body)
            elif isinstance(op, ast.Assign):
                return check(op.value)
            elif isinstance(op, ast.Return):
                return check(op.value) if op.value else True
            elif isinstance(op, ast.Compare):
                return check(op.left) and check(op.comparators)
            elif isinstance(op, ast.BinOp):
                return check(op.left) and check(op.right)
            elif isinstance(op, ast.IfExp) or isinstance(op, ast.If):
                return check(op.test) and check(op.body) and check(op.orelse)
            elif isinstance(op, ast.Dict):
                return check(op.values)
            elif isinstance(op, ast.Set):
                return check(op.elts)
            elif isinstance(op, ast.comprehension):
                return check(op.iter) and check(op.ifs)
            elif (
                isinstance(op, ast.ListComp)
                or isinstance(op, ast.SetComp)
                or isinstance(op, ast.GeneratorExp)
            ):
                return check(op.generators)
            elif isinstance(op, ast.DictComp):
                return check(op.value) and check(op.generators)
            elif isinstance(op, ast.Yield) or isinstance(op, ast.YieldFrom):
                return check(op.value) if op.value else True
            elif isinstance(op, ast.FormattedValue):
                return check(op.value)
            elif isinstance(op, ast.JoinedStr):
                return check(op.values)
            elif isinstance(op, ast.Return):
                return check(op.value) if op.value else True
            elif isinstance(op, ast.Call):
                f_name = ""
                if isinstance(op.func, ast.Attribute):
                    # op.func.value is a ast.Name in this case
                    f_name = op.func.value.id
                elif isinstance(op.func, ast.Name):
                    f_name = op.func.id

                if f_name in unchecked_func_names:
                    return False
                else:
                    return check(op.args) and check(op.keywords)
            elif isinstance(op, ast.keyword):
                return check(op.value)
            elif isinstance(op, ast.FunctionDef):
                return check(op.body) and (check(op.returns) if op.returns else True)
            else:
                return True

        for function in functions:
            if not check(function):
                return False, function.name

        return True, ""

    def __manipulate_app_main(
        self, directory: str, app_name: str, accepted_names: Set[str]
    ) -> Tuple[List[str], str]:
        def extract_functions_and_imports(module_body: List[ast.stmt]):
            # We only keep precond and iteration functions, and we add to them the docstring with the contracts
            functions_ast = list(
                map(
                    lambda f: self.__add_return_state(
                        self.__add_doc_string(
                            f, self.__construct_contracts(self.__app_names)
                        )
                    )
                    if f.name == f"{app_name}_iteration"
                    else f,
                    cast(
                        List[ast.FunctionDef],
                        filter(
                            lambda n: isinstance(n, ast.FunctionDef)
                            and (
                                n.name == f"{app_name}_precond"
                                or n.name == f"{app_name}_iteration"
                            ),
                            module_body,
                        ),
                    ),
                )
            )
            # We only keep the imports that were added by the user
            imports_ast = list(
                filter(
                    lambda n: isinstance(n, ast.Import),
                    module_body,
                )
            )
            from_imports_ast = list(
                filter(
                    lambda n: isinstance(n, ast.ImportFrom) and n.module != "devices",
                    module_body,
                )
            )
            return functions_ast, imports_ast, from_imports_ast

        with open(f"{directory}/{app_name}/main.py", "r") as file:
            module = ast.parse(file.read())
            module_body = module.body

            # We rename all the device instances and add the state argument to each of their calls
            self.__rename_instances_add_state(module_body, app_name, accepted_names)

            # Extract imports, precond/iteration functions and add the contracts to them
            (
                functions_ast,
                imports_ast,
                from_imports_ast,
            ) = extract_functions_and_imports(module_body)

            # Check if a precondition function contains a call to an unchecked function
            unchecked_func_names = set(self.__get_unchecked_functions(module_body))
            valid, wrong_precond_func = self.__check_no_unchecked_calls_in_precond(
                list(
                    filter(
                        lambda f: f.name.endswith(f"_{self.__PRECOND_FUNC_NAME}"),
                        functions_ast,
                    )
                ),
                unchecked_func_names,
            )
            if not valid:
                raise InvalidUncheckedFunctionCall(
                    f"The precondition function '{wrong_precond_func}' contains a call to an unchecked function."
                )

            # Transform to source code
            functions = astor.to_source(ast.Module(functions_ast))
            imports = astor.to_source(ast.Module(imports_ast))
            from_imports = astor.to_source(ast.Module(from_imports_ast))
            return [imports, from_imports], functions


# TODOs

# if op.name == self.__PRECOND_FUNC_NAME:
#     # Check that there are no function calls to the unchecked ones
#     pass
# else:
#     # Replace all calls to the unchecked by a variable with the same name
#     # Add to the arguments of the function the unchecked functions names
#     # Add postconditions of the unchecked functions to the preconditions of the iteration function
#     pass
