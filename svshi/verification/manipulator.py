import ast
import astor
from dataclasses import dataclass
from typing import Dict, List, Set, Tuple, Union, cast


class InvalidUncheckedFunctionCallException(Exception):
    """
    An invalid unchecked function call exception.
    """


class UntypedUncheckedFunctionException(Exception):
    """
    An unchecked function has no return type.
    """


@dataclass
class UncheckedFunction:
    """
    An unchecked function.
    """

    name: str
    name_with_app_name: str
    doc_string: str
    return_type: str


class Manipulator:
    """
    Python AST manipulator.
    """

    __STATE_ARGUMENT = "physical_state"
    __STATE_TYPE = "PhysicalState"
    __UNCHECKED_FUNC_PREFIX = "unchecked"
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
        app_name: str,
    ) -> Dict[str, UncheckedFunction]:
        """
        Go through the AST and return a Dict containing
        "unchecked_func_name" -> UncheckedFunction
        """
        if isinstance(op, list) or isinstance(op, tuple):
            dicts_list = [self.__get_unchecked_functions(v, app_name) for v in list(op)]
            res = {}
            for d in dicts_list:
                for k in d.keys():
                    res[k] = d[k]
            return res
        elif isinstance(op, ast.List) or isinstance(op, ast.Tuple):
            return self.__get_unchecked_functions(op.elts, app_name)
        elif isinstance(op, ast.FunctionDef) and (
            op.name.startswith(self.__UNCHECKED_FUNC_PREFIX)
        ):
            doc_string = ast.get_docstring(op)
            doc_string = "" if doc_string == None else doc_string
            return_type = op.returns
            if not return_type or not cast(ast.Name, return_type).id:
                raise UntypedUncheckedFunctionException(
                    f"The unchecked function '{op.name}' has no return type."
                )
            func_name = op.name
            new_func_name = f"{app_name}_{func_name}"
            return {
                func_name: UncheckedFunction(
                    func_name, new_func_name, doc_string, cast(ast.Name, return_type).id
                )
            }
        else:
            return {}

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
        unchecked_functions: List[UncheckedFunction],
    ):
        if isinstance(op, list) or isinstance(op, tuple):
            [
                self.__rename_instances_add_state(
                    v, app_name, accepted_names, unchecked_functions
                )
                for v in list(op)
            ]
        elif isinstance(op, ast.List) or isinstance(op, ast.Tuple):
            self.__rename_instances_add_state(
                op.elts, app_name, accepted_names, unchecked_functions
            )
        elif isinstance(op, ast.BoolOp):
            self.__rename_instances_add_state(
                op.values, app_name, accepted_names, unchecked_functions
            )
        elif isinstance(op, ast.UnaryOp):
            self.__rename_instances_add_state(
                op.operand, app_name, accepted_names, unchecked_functions
            )
        elif isinstance(op, ast.NamedExpr) or isinstance(op, ast.Expr):
            self.__rename_instances_add_state(
                op.value, app_name, accepted_names, unchecked_functions
            )
        elif isinstance(op, ast.Lambda):
            self.__rename_instances_add_state(
                op.body, app_name, accepted_names, unchecked_functions
            )
        elif isinstance(op, ast.Assign):
            self.__rename_instances_add_state(
                op.value, app_name, accepted_names, unchecked_functions
            )
        elif isinstance(op, ast.Return):
            if op.value:
                self.__rename_instances_add_state(
                    op.value, app_name, accepted_names, unchecked_functions
                )
        elif isinstance(op, ast.Compare):
            self.__rename_instances_add_state(
                op.left, app_name, accepted_names, unchecked_functions
            )
            self.__rename_instances_add_state(
                op.comparators, app_name, accepted_names, unchecked_functions
            )
        elif isinstance(op, ast.BinOp):
            self.__rename_instances_add_state(
                op.left, app_name, accepted_names, unchecked_functions
            )
            self.__rename_instances_add_state(
                op.right, app_name, accepted_names, unchecked_functions
            )
        elif isinstance(op, ast.IfExp) or isinstance(op, ast.If):
            self.__rename_instances_add_state(
                op.test, app_name, accepted_names, unchecked_functions
            )
            self.__rename_instances_add_state(
                op.body, app_name, accepted_names, unchecked_functions
            )
            self.__rename_instances_add_state(
                op.orelse, app_name, accepted_names, unchecked_functions
            )
        elif isinstance(op, ast.Dict):
            self.__rename_instances_add_state(
                op.values, app_name, accepted_names, unchecked_functions
            )
        elif isinstance(op, ast.Set):
            self.__rename_instances_add_state(
                op.elts, app_name, accepted_names, unchecked_functions
            )
        elif isinstance(op, ast.comprehension):
            self.__rename_instances_add_state(
                op.iter, app_name, accepted_names, unchecked_functions
            )
            self.__rename_instances_add_state(
                op.ifs, app_name, accepted_names, unchecked_functions
            )
        elif (
            isinstance(op, ast.ListComp)
            or isinstance(op, ast.SetComp)
            or isinstance(op, ast.GeneratorExp)
        ):
            self.__rename_instances_add_state(
                op.generators, app_name, accepted_names, unchecked_functions
            )
        elif isinstance(op, ast.DictComp):
            self.__rename_instances_add_state(
                op.value, app_name, accepted_names, unchecked_functions
            )
            self.__rename_instances_add_state(
                op.generators, app_name, accepted_names, unchecked_functions
            )
        elif isinstance(op, ast.Yield) or isinstance(op, ast.YieldFrom):
            if op.value:
                self.__rename_instances_add_state(
                    op.value, app_name, accepted_names, unchecked_functions
                )
        elif isinstance(op, ast.FormattedValue):
            self.__rename_instances_add_state(
                op.value, app_name, accepted_names, unchecked_functions
            )
        elif isinstance(op, ast.JoinedStr):
            self.__rename_instances_add_state(
                op.values, app_name, accepted_names, unchecked_functions
            )
        elif isinstance(op, ast.Return):
            if op.value:
                self.__rename_instances_add_state(
                    op.value, app_name, accepted_names, unchecked_functions
                )
        elif isinstance(op, ast.Call):
            self.__rename_instances_add_state(
                op.args, app_name, accepted_names, unchecked_functions
            )
            self.__rename_instances_add_state(
                op.keywords, app_name, accepted_names, unchecked_functions
            )
            f_name = ""
            if isinstance(op.func, ast.Attribute):
                # op.func.value is a ast.Name in this case
                f_name = cast(ast.Name, op.func.value).id
            elif isinstance(op.func, ast.Name):
                f_name = op.func.id
            else:
                self.__rename_instances_add_state(
                    op.func, app_name, accepted_names, unchecked_functions
                )

            if f_name in accepted_names:
                # If the function name is in the list of accepted names, add the state argument to the call
                op.args.append(ast.Name(self.__STATE_ARGUMENT, ast.Load))

                # Rename the instance calling the function, adding the app name to it
                new_name = f"{app_name.upper()}_{f_name}"
                if isinstance(op.func, ast.Attribute):
                    cast(ast.Name, op.func.value).id = new_name
                elif isinstance(op.func, ast.Name):
                    op.func.id = new_name
            elif f_name in {uf.name for uf in unchecked_functions}:
                # If it is a call to an unchecked function, add the app name to the call
                new_name = f"{app_name}_{f_name}"

                # It can be only a ast.Name since the function is defined
                cast(ast.Name, op.func).id = new_name
        elif isinstance(op, ast.keyword):
            self.__rename_instances_add_state(
                op.value, app_name, accepted_names, unchecked_functions
            )
        elif isinstance(op, ast.FunctionDef) and (
            op.name == self.__PRECOND_FUNC_NAME
            or op.name == self.__ITERATION_FUNC_NAME
            or op.name.startswith(self.__UNCHECKED_FUNC_PREFIX)
        ):
            if (
                op.name == self.__PRECOND_FUNC_NAME
                or op.name == self.__ITERATION_FUNC_NAME
            ):
                # Add the state argument to the function
                annotation = ast.Name(self.__STATE_TYPE, ast.Load)
                op.args.args.append(ast.arg(self.__STATE_ARGUMENT, annotation))

            # Rename the function, adding the app name to it
            op.name = f"{app_name}_{op.name}"
            self.__rename_instances_add_state(
                op.body, app_name, accepted_names, unchecked_functions
            )

    def __construct_contracts(
        self,
        app_names: List[str],
        unchecked_functions: List[UncheckedFunction],
        verification: bool = True,
    ) -> str:
        pre_str = "pre: "
        post_str = "post: "
        return_value_name_str = "__return__"

        def construct_func_call(func_name: str, arg_names: List[str]) -> str:
            res = func_name + "("
            for i in range(len(arg_names)):
                res += arg_names[i] + ", "
            if len(res) > 2:
                res = res[:-2]
            res += ")"
            return res

        def construct_pre_unchecked_func(
            unchecked_func_name: str, doc_string: str
        ) -> List[str]:
            list_post_conds = []
            post_str_no_space = "post:"
            for l in doc_string.splitlines():
                if post_str_no_space in l:
                    cond = (
                        pre_str
                        + l.replace(post_str_no_space, "")
                        .replace(return_value_name_str, unchecked_func_name)
                        .strip()
                    )
                    list_post_conds.append(cond)
            return list_post_conds

        conditions = []
        sorted_app_names = sorted(app_names)
        for app_name in sorted_app_names:
            precond = pre_str
            precond += construct_func_call(
                app_name + "_" + self.__PRECOND_FUNC_NAME, [self.__STATE_ARGUMENT]
            )
            conditions.append(precond)

        if verification:
            for unchecked_func in unchecked_functions:
                conditions.extend(
                    construct_pre_unchecked_func(
                        unchecked_func.name_with_app_name, unchecked_func.doc_string
                    )
                )

        for app_name in sorted_app_names:
            postcond = post_str
            postcond += construct_func_call(
                app_name + "_" + self.__PRECOND_FUNC_NAME, [return_value_name_str]
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
        f.body.append(ast.Return(ast.Name(self.__STATE_ARGUMENT, ast.Load)))
        return f

    def __check_if_func_call_and_applicable_and_replace(
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
        unchecked_func_names: Set[str],
    ) -> Union[ast.Name, None]:
        """
        Checks if the given op is a Function call and if that function is the set of unchecked_function_names,
        if that's the case, it returns a new ast.Name with the function name as id
        None otherwise
        """
        if isinstance(op, ast.Call):
            f_name = (
                "123"  # An illegal function name to be sure it is not in the given set
            )
            if isinstance(op.func, ast.Attribute):
                # op.func.value is a ast.Name in this case
                f_name = cast(ast.Name, op.func.value).id
            elif isinstance(op.func, ast.Name):
                f_name = op.func.id

            if f_name in unchecked_func_names:
                return ast.Name(id=f_name)
        return None

    def __change_unchecked_functions_to_var(
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
        unchecked_functions: List[UncheckedFunction],
    ):
        """
        Manipulates the op to replace all calls to unchecked functions by a Name with id=unchecked_func_name. The replacement is
        done in place, it modifies the object.
        """
        unchecked_func_names = {uf.name_with_app_name for uf in unchecked_functions}
        if isinstance(op, list):
            for index, v in enumerate(op):
                temp_new_ast = self.__check_if_func_call_and_applicable_and_replace(
                    v, unchecked_func_names
                )
                if temp_new_ast:
                    op[index] = temp_new_ast
                self.__change_unchecked_functions_to_var(v, unchecked_functions)
        elif isinstance(op, ast.List) or isinstance(op, ast.Tuple):
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__change_unchecked_functions_to_var(op.elts, unchecked_functions)
        elif isinstance(op, ast.BoolOp):
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__change_unchecked_functions_to_var(op.values, unchecked_functions)
        elif isinstance(op, ast.UnaryOp):
            temp_new_ast = self.__check_if_func_call_and_applicable_and_replace(
                op.operand, unchecked_func_names
            )
            if temp_new_ast:
                op.operand = temp_new_ast
            self.__change_unchecked_functions_to_var(op.operand, unchecked_functions)
        elif isinstance(op, ast.NamedExpr) or isinstance(op, ast.Expr):
            temp_new_ast = self.__check_if_func_call_and_applicable_and_replace(
                op.value, unchecked_func_names
            )
            if temp_new_ast:
                op.value = temp_new_ast
            self.__change_unchecked_functions_to_var(op.value, unchecked_functions)
        elif isinstance(op, ast.Lambda):
            temp_new_ast = self.__check_if_func_call_and_applicable_and_replace(
                op.body, unchecked_func_names
            )
            if temp_new_ast:
                op.body = temp_new_ast
            self.__change_unchecked_functions_to_var(op.body, unchecked_functions)
        elif isinstance(op, ast.Assign):
            temp_new_ast = self.__check_if_func_call_and_applicable_and_replace(
                op.value, unchecked_func_names
            )
            if temp_new_ast:
                op.value = temp_new_ast
            self.__change_unchecked_functions_to_var(op.value, unchecked_functions)
        elif isinstance(op, ast.Return):
            if op.value:
                temp_new_ast = self.__check_if_func_call_and_applicable_and_replace(
                    op.value, unchecked_func_names
                )
                if temp_new_ast:
                    op.value = temp_new_ast
                self.__change_unchecked_functions_to_var(op.value, unchecked_functions)
        elif isinstance(op, ast.Compare):
            temp_new_ast = self.__check_if_func_call_and_applicable_and_replace(
                op.left, unchecked_func_names
            )
            if temp_new_ast:
                op.left = temp_new_ast
            self.__change_unchecked_functions_to_var(op.left, unchecked_functions)
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__change_unchecked_functions_to_var(
                op.comparators, unchecked_functions
            )
        elif isinstance(op, ast.BinOp):
            temp_new_ast = self.__check_if_func_call_and_applicable_and_replace(
                op.left, unchecked_func_names
            )
            if temp_new_ast:
                op.left = temp_new_ast
            self.__change_unchecked_functions_to_var(op.left, unchecked_functions)
            temp_new_ast = self.__check_if_func_call_and_applicable_and_replace(
                op.right, unchecked_func_names
            )
            if temp_new_ast:
                op.right = temp_new_ast
            self.__change_unchecked_functions_to_var(op.right, unchecked_functions)
        elif isinstance(op, ast.IfExp):
            temp_new_ast = self.__check_if_func_call_and_applicable_and_replace(
                op.test, unchecked_func_names
            )
            if temp_new_ast:
                op.test = temp_new_ast
            self.__change_unchecked_functions_to_var(op.test, unchecked_functions)
            temp_new_ast = self.__check_if_func_call_and_applicable_and_replace(
                op.body, unchecked_func_names
            )
            if temp_new_ast:
                op.body = temp_new_ast
            self.__change_unchecked_functions_to_var(op.body, unchecked_functions)
            temp_new_ast = self.__check_if_func_call_and_applicable_and_replace(
                op.orelse, unchecked_func_names
            )
            if temp_new_ast:
                op.orelse = temp_new_ast
            self.__change_unchecked_functions_to_var(op.orelse, unchecked_functions)
        elif isinstance(op, ast.If):
            temp_new_ast = self.__check_if_func_call_and_applicable_and_replace(
                op.test, unchecked_func_names
            )
            if temp_new_ast:
                op.test = temp_new_ast
            self.__change_unchecked_functions_to_var(op.test, unchecked_functions)
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__change_unchecked_functions_to_var(op.body, unchecked_functions)
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__change_unchecked_functions_to_var(op.orelse, unchecked_functions)
        elif isinstance(op, ast.Dict):
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__change_unchecked_functions_to_var(op.values, unchecked_functions)
        elif isinstance(op, ast.Set):
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__change_unchecked_functions_to_var(op.elts, unchecked_functions)
        elif isinstance(op, ast.comprehension):
            temp_new_ast = self.__check_if_func_call_and_applicable_and_replace(
                op.iter, unchecked_func_names
            )
            if temp_new_ast:
                op.iter = temp_new_ast
            self.__change_unchecked_functions_to_var(op.iter, unchecked_functions)
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__change_unchecked_functions_to_var(op.ifs, unchecked_functions)
        elif (
            isinstance(op, ast.ListComp)
            or isinstance(op, ast.SetComp)
            or isinstance(op, ast.GeneratorExp)
        ):
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__change_unchecked_functions_to_var(op.generators, unchecked_functions)
        elif isinstance(op, ast.DictComp):
            temp_new_ast = self.__check_if_func_call_and_applicable_and_replace(
                op.value, unchecked_func_names
            )
            if temp_new_ast:
                op.value = temp_new_ast
            self.__change_unchecked_functions_to_var(op.value, unchecked_functions)
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__change_unchecked_functions_to_var(op.generators, unchecked_functions)
        elif isinstance(op, ast.Yield) or isinstance(op, ast.YieldFrom):
            if op.value:
                temp_new_ast = self.__check_if_func_call_and_applicable_and_replace(
                    op.value, unchecked_func_names
                )
                if temp_new_ast:
                    op.value = temp_new_ast
                self.__change_unchecked_functions_to_var(op.value, unchecked_functions)
        elif isinstance(op, ast.FormattedValue):
            temp_new_ast = self.__check_if_func_call_and_applicable_and_replace(
                op.value, unchecked_func_names
            )
            if temp_new_ast:
                op.value = temp_new_ast
            self.__change_unchecked_functions_to_var(op.value, unchecked_functions)
        elif isinstance(op, ast.JoinedStr):
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__change_unchecked_functions_to_var(op.values, unchecked_functions)
        elif isinstance(op, ast.Return):
            if op.value:
                temp_new_ast = self.__check_if_func_call_and_applicable_and_replace(
                    op.value, unchecked_func_names
                )
                if temp_new_ast:
                    op.value = temp_new_ast
                self.__change_unchecked_functions_to_var(op.value, unchecked_functions)
        elif isinstance(op, ast.Call):
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__change_unchecked_functions_to_var(op.args, unchecked_functions)
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__change_unchecked_functions_to_var(op.keywords, unchecked_functions)
            # Here we cannot have a Call to replace because it would have been done in the parent
            self.__change_unchecked_functions_to_var(op.func, unchecked_functions)
        elif isinstance(op, ast.keyword):
            temp_new_ast = self.__check_if_func_call_and_applicable_and_replace(
                op.value, unchecked_func_names
            )
            if temp_new_ast:
                op.value = temp_new_ast
            self.__change_unchecked_functions_to_var(op.value, unchecked_functions)
        elif isinstance(op, ast.FunctionDef):
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__change_unchecked_functions_to_var(op.body, unchecked_functions)

    def manipulate_mains(self, verification: bool) -> Tuple[List[str], List[str]]:
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
                directory, app_name, accepted_names, verification
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
                    f_name = cast(ast.Name, op.func.value).id
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

    def __add_unchecked_function_arguments_to_iteration(
        self, f: ast.FunctionDef, arguments: List[UncheckedFunction]
    ):
        ast_arguments = list(
            map(
                lambda unchecked_f: ast.arg(
                    unchecked_f.name_with_app_name,
                    ast.Name(unchecked_f.return_type, ast.Load),
                ),
                arguments,
            )
        )
        f.args.args.extend(ast_arguments)

    def __manipulate_app_main(
        self,
        directory: str,
        app_name: str,
        accepted_names: Set[str],
        verification: bool,
    ) -> Tuple[List[str], str]:
        def extract_functions_and_imports(
            module_body: List[ast.stmt],
            unchecked_func_dict: Dict[str, UncheckedFunction],
        ):
            # We only keep precond and iteration functions, and we add to them the docstring with the contracts
            functions_ast = list(
                map(
                    lambda f: self.__add_return_state(
                        self.__add_doc_string(
                            f,
                            self.__construct_contracts(
                                self.__app_names,
                                list(unchecked_func_dict.values()),
                                verification,
                            ),
                        )
                    )
                    if f.name == f"{app_name}_iteration"
                    else f,
                    cast(
                        List[ast.FunctionDef],
                        filter(
                            lambda n: isinstance(n, ast.FunctionDef)
                            and (
                                n.name == f"{app_name}_{self.__PRECOND_FUNC_NAME}"
                                or n.name == f"{app_name}_{self.__ITERATION_FUNC_NAME}"
                                or (
                                    not verification
                                    and n.name.startswith(
                                        f"{app_name}_{self.__UNCHECKED_FUNC_PREFIX}"
                                    )
                                )
                            ),
                            module_body,
                        ),
                    ),
                )
            )
            # We only keep the imports that were added by the user, if it is the runtime file.
            # Otherwise, we remove them as well, so that the verification fails if the user used
            # external libraries in iteration or precond functions
            imports_ast = list(
                filter(
                    lambda n: not verification and isinstance(n, ast.Import),
                    module_body,
                )
            )
            from_imports_ast = list(
                filter(
                    lambda n: not verification
                    and isinstance(n, ast.ImportFrom)
                    and n.module != "devices",
                    module_body,
                )
            )
            return functions_ast, imports_ast, from_imports_ast

        with open(f"{directory}/{app_name}/main.py", "r") as file:
            module = ast.parse(file.read())
            module_body = module.body

            unchecked_func_dict = self.__get_unchecked_functions(module_body, app_name)
            unchecked_funcs = list(unchecked_func_dict.values())

            # We rename all the device instances and add the state argument to each of their calls
            self.__rename_instances_add_state(
                module_body, app_name, accepted_names, unchecked_funcs
            )

            # Extract imports, precond/iteration functions and add the contracts to them
            (
                functions_ast,
                imports_ast,
                from_imports_ast,
            ) = extract_functions_and_imports(module_body, unchecked_func_dict)

            # Check if a precondition function contains a call to an unchecked function
            unchecked_func_names = {
                uf.name_with_app_name for _, uf in unchecked_func_dict.items()
            }
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
                raise InvalidUncheckedFunctionCallException(
                    f"The precondition function '{wrong_precond_func}' contains a call to an unchecked function."
                )

            iteration_functions = list(
                filter(
                    lambda f: f.name.endswith(f"_{self.__ITERATION_FUNC_NAME}"),
                    functions_ast,
                )
            )

            if verification:
                # Replace all calls to unchecked functions by variables in iteration function
                self.__change_unchecked_functions_to_var(
                    iteration_functions,
                    unchecked_funcs,
                )

                # Add unchecked_function_names as arguments of the iteration functions
                for f in iteration_functions:
                    self.__add_unchecked_function_arguments_to_iteration(
                        f, unchecked_funcs
                    )

            # Transform to source code
            functions = astor.to_source(ast.Module(functions_ast))
            imports = astor.to_source(ast.Module(imports_ast))
            from_imports = astor.to_source(ast.Module(from_imports_ast))
            return [imports, from_imports], functions
