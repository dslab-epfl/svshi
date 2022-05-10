import ast
import astor
from dataclasses import dataclass
from typing import Dict, List, Set, Tuple, Union, Final, cast


class InvalidFunctionCallException(Exception):
    """
    An invalid function call exception.
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

    __PHYSICAL_STATE_ARGUMENT: Final = "physical_state"
    __PHYSICAL_STATE_TYPE: Final = "PhysicalState"
    __INTERNAL_STATE_ARGUMENT: Final = "internal_state"
    __INTERNAL_STATE_TYPE: Final = "InternalState"
    __APP_STATE_ARGUMENT: Final = "app_state"
    __APP_STATE_TYPE: Final = "AppState"
    __UNCHECKED_FUNC_PREFIX: Final = "unchecked"
    __INVARIANT_FUNC_NAME: Final = "invariant"
    __ITERATION_FUNC_NAME: Final = "iteration"
    __PRINT_FUNC_NAME: Final = "print"

    def __init__(
        self,
        instances_names_per_app: Dict[Tuple[str, str], Set[str]],
        filenames_per_app: Dict[str, Set[str]],
        files_folder_path: str,
    ):
        self.__app_names = list(
            sorted(map(lambda t: t[1], instances_names_per_app.keys()))
        )
        self.__app_states_names = list(
            map(lambda n: f"{n}_{self.__APP_STATE_ARGUMENT}", self.__app_names)
        )
        self.__instances_names_per_app = instances_names_per_app
        self.__filenames_per_app = filenames_per_app
        self.__files_folder_path = files_folder_path

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
        Goes through the AST and returns a Dict containing entries of the form
        "unchecked_func_name" -> UncheckedFunction.
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
            if not return_type:
                raise UntypedUncheckedFunctionException(
                    f"The unchecked function '{op.name}' has no return type."
                )
            # When the return type is 'None', return_type is ast.Constant
            return_type_str = (
                cast(ast.Name, return_type).id
                if isinstance(return_type, ast.Name)
                else cast(ast.Constant, return_type).value
            )
            if return_type_str != None and len(return_type_str) == 0:
                raise UntypedUncheckedFunctionException(
                    f"The unchecked function '{op.name}' has no return type."
                )
            func_name = op.name
            new_func_name = f"{app_name}_{func_name}"
            return {
                func_name: UncheckedFunction(
                    func_name, new_func_name, doc_string, return_type_str
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
        verification: bool,
    ):
        """
        Renames the instances calling functions with name in accepted_names
        and unchecked_functions by adding the app_name in front of them. Additionally, it also adds the
        "state" argument to the calls with name in accepted_names.
        Furthermore, "invariant", "iteration" and "unchecked" functions are modified with the app_name added to them
        and with the state parameters added for the first two.
        """
        if isinstance(op, list) or isinstance(op, tuple):
            for v in list(op):
                self.__rename_instances_add_state(
                    v, app_name, accepted_names, unchecked_functions, verification
                )
        elif isinstance(op, ast.List) or isinstance(op, ast.Tuple):
            self.__rename_instances_add_state(
                op.elts, app_name, accepted_names, unchecked_functions, verification
            )
        elif isinstance(op, ast.BoolOp):
            self.__rename_instances_add_state(
                op.values, app_name, accepted_names, unchecked_functions, verification
            )
        elif isinstance(op, ast.UnaryOp):
            self.__rename_instances_add_state(
                op.operand, app_name, accepted_names, unchecked_functions, verification
            )
        elif isinstance(op, ast.NamedExpr):
            self.__rename_instances_add_state(
                op.target, app_name, accepted_names, unchecked_functions, verification
            )
            self.__rename_instances_add_state(
                op.value, app_name, accepted_names, unchecked_functions, verification
            )
        elif isinstance(op, ast.Expr):
            self.__rename_instances_add_state(
                op.value, app_name, accepted_names, unchecked_functions, verification
            )
        elif isinstance(op, ast.Lambda):
            self.__rename_instances_add_state(
                op.body, app_name, accepted_names, unchecked_functions, verification
            )
        elif isinstance(op, ast.Assign):
            self.__rename_instances_add_state(
                op.targets, app_name, accepted_names, unchecked_functions, verification
            )
            self.__rename_instances_add_state(
                op.value, app_name, accepted_names, unchecked_functions, verification
            )
        elif isinstance(op, ast.AugAssign):
            self.__rename_instances_add_state(
                op.target, app_name, accepted_names, unchecked_functions, verification
            )
            self.__rename_instances_add_state(
                op.value, app_name, accepted_names, unchecked_functions, verification
            )
        elif isinstance(op, ast.AnnAssign):
            self.__rename_instances_add_state(
                op.target, app_name, accepted_names, unchecked_functions, verification
            )
            self.__rename_instances_add_state(
                op.annotation,
                app_name,
                accepted_names,
                unchecked_functions,
                verification,
            )
            if op.value:
                self.__rename_instances_add_state(
                    op.value,
                    app_name,
                    accepted_names,
                    unchecked_functions,
                    verification,
                )
        elif isinstance(op, ast.Return):
            if op.value:
                self.__rename_instances_add_state(
                    op.value,
                    app_name,
                    accepted_names,
                    unchecked_functions,
                    verification,
                )
        elif isinstance(op, ast.Compare):
            self.__rename_instances_add_state(
                op.left, app_name, accepted_names, unchecked_functions, verification
            )
            self.__rename_instances_add_state(
                op.comparators,
                app_name,
                accepted_names,
                unchecked_functions,
                verification,
            )
        elif isinstance(op, ast.BinOp):
            self.__rename_instances_add_state(
                op.left, app_name, accepted_names, unchecked_functions, verification
            )
            self.__rename_instances_add_state(
                op.right, app_name, accepted_names, unchecked_functions, verification
            )
        elif isinstance(op, ast.IfExp) or isinstance(op, ast.If):
            self.__rename_instances_add_state(
                op.test, app_name, accepted_names, unchecked_functions, verification
            )
            self.__rename_instances_add_state(
                op.body, app_name, accepted_names, unchecked_functions, verification
            )
            self.__rename_instances_add_state(
                op.orelse, app_name, accepted_names, unchecked_functions, verification
            )
        elif isinstance(op, ast.Dict):
            self.__rename_instances_add_state(
                op.values, app_name, accepted_names, unchecked_functions, verification
            )
        elif isinstance(op, ast.Set):
            self.__rename_instances_add_state(
                op.elts, app_name, accepted_names, unchecked_functions, verification
            )
        elif isinstance(op, ast.comprehension):
            self.__rename_instances_add_state(
                op.iter, app_name, accepted_names, unchecked_functions, verification
            )
            self.__rename_instances_add_state(
                op.ifs, app_name, accepted_names, unchecked_functions, verification
            )
        elif (
            isinstance(op, ast.ListComp)
            or isinstance(op, ast.SetComp)
            or isinstance(op, ast.GeneratorExp)
        ):
            self.__rename_instances_add_state(
                op.generators,
                app_name,
                accepted_names,
                unchecked_functions,
                verification,
            )
        elif isinstance(op, ast.DictComp):
            self.__rename_instances_add_state(
                op.value, app_name, accepted_names, unchecked_functions, verification
            )
            self.__rename_instances_add_state(
                op.generators,
                app_name,
                accepted_names,
                unchecked_functions,
                verification,
            )
        elif isinstance(op, ast.Yield) or isinstance(op, ast.YieldFrom):
            if op.value:
                self.__rename_instances_add_state(
                    op.value,
                    app_name,
                    accepted_names,
                    unchecked_functions,
                    verification,
                )
        elif isinstance(op, ast.FormattedValue):
            self.__rename_instances_add_state(
                op.value, app_name, accepted_names, unchecked_functions, verification
            )
        elif isinstance(op, ast.JoinedStr):
            self.__rename_instances_add_state(
                op.values, app_name, accepted_names, unchecked_functions, verification
            )
        elif isinstance(op, ast.Return):
            if op.value:
                self.__rename_instances_add_state(
                    op.value,
                    app_name,
                    accepted_names,
                    unchecked_functions,
                    verification,
                )
        elif isinstance(op, ast.Call):
            self.__rename_instances_add_state(
                op.args, app_name, accepted_names, unchecked_functions, verification
            )
            self.__rename_instances_add_state(
                op.keywords, app_name, accepted_names, unchecked_functions, verification
            )
            f_name = ""
            if isinstance(op.func, ast.Attribute):
                # op.func.value is a ast.Name in this case
                f_name = cast(ast.Name, op.func.value).id
                f = op.func
                if isinstance(f.value, ast.Name) and f.value.id in {"svshi_api"}:
                    #functions of the svshi_api object is called
                    op.args.append(ast.Name(self.__INTERNAL_STATE_ARGUMENT, ast.Load))
            elif isinstance(op.func, ast.Name):
                f_name = op.func.id
            else:
                self.__rename_instances_add_state(
                    op.func, app_name, accepted_names, unchecked_functions, verification
                )
            if f_name in accepted_names:
                # If the function name is in the list of accepted names, add the state argument to the call
                op.args.append(ast.Name(self.__PHYSICAL_STATE_ARGUMENT, ast.Load))
                if(verification):
                    op.args.append(ast.Name(self.__INTERNAL_STATE_ARGUMENT, ast.Load))

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
                op.value, app_name, accepted_names, unchecked_functions, verification
            )
        elif isinstance(op, ast.Attribute):
            name = cast(ast.Name, op.value).id
            if name == self.__APP_STATE_ARGUMENT:
                # If it is the app_state variable, we rename it
                new_name = f"{app_name}_{name}"
                cast(ast.Name, op.value).id = new_name
        elif isinstance(op, ast.FunctionDef) and (
            op.name == self.__INVARIANT_FUNC_NAME
            or op.name == self.__ITERATION_FUNC_NAME
            or op.name.startswith(self.__UNCHECKED_FUNC_PREFIX)
        ):
            if (
                op.name == self.__INVARIANT_FUNC_NAME
                or op.name == self.__ITERATION_FUNC_NAME
            ):
                # Add the state arguments to the function
                # For the verification file we pass all the app states, for the runtime one just one is enough
                state_args = (
                    list(
                        map(
                            lambda n: ast.arg(
                                n,
                                ast.Name(self.__APP_STATE_TYPE, ast.Load),
                            ),
                            filter(
                                lambda n: n.startswith(app_name),
                                self.__app_states_names,
                            ),
                        )
                    )
                    if not verification
                    else list(
                        map(
                            lambda n: ast.arg(
                                n,
                                ast.Name(self.__APP_STATE_TYPE, ast.Load),
                            ),
                            self.__app_states_names,
                        )
                    )
                )
                state_args.append(
                    ast.arg(
                        self.__PHYSICAL_STATE_ARGUMENT,
                        ast.Name(self.__PHYSICAL_STATE_TYPE, ast.Load),
                    )
                )
                state_args.append(
                        ast.arg(
                            self.__INTERNAL_STATE_ARGUMENT,
                            ast.Name(self.__INTERNAL_STATE_TYPE, ast.Load),
                        )
                    )
                op.args.args.extend(state_args)

            # Rename the function, adding the app name to it
            op.name = f"{app_name}_{op.name}"
            self.__rename_instances_add_state(
                op.body, app_name, accepted_names, unchecked_functions, verification
            )

    def __construct_contracts(
        self,
        app_names: List[str],
        unchecked_functions: List[UncheckedFunction],
        verification: bool = True,
    ) -> str:
        """
        Returns the contract as a docstring using the given app names. If the verification flag is set,
        it also adds to the pre-conditions the unchecked functions' post-conditions.
        """
        pre_str = "pre: "
        post_str = "post: "
        return_value_name_str = "**__return__"
        return_stmt_postcond = "__return__"

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
                        .replace(return_stmt_postcond, unchecked_func_name)
                        .strip()
                    )
                    list_post_conds.append(cond)
            return list_post_conds

        conditions = []
        sorted_app_names = sorted(app_names)
        for app_name in sorted_app_names:
            arg_names = (
                list(filter(lambda n: n.startswith(app_name), self.__app_states_names))
                if not verification
                else list(self.__app_states_names)
            )
            arg_names.append(self.__PHYSICAL_STATE_ARGUMENT)
            if(verification):
                arg_names.append(self.__INTERNAL_STATE_ARGUMENT)
            invariant = pre_str
            invariant += construct_func_call(
                app_name + "_" + self.__INVARIANT_FUNC_NAME,
                arg_names,
            )
            conditions.append(invariant)

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
                app_name + "_" + self.__INVARIANT_FUNC_NAME, [return_value_name_str]
            )
            conditions.append(postcond)
        res = "\n".join(conditions)
        return res

    def __add_doc_string(self, f: ast.FunctionDef, doc_string: str) -> ast.FunctionDef:
        """
        Adds the given docstring to the given function, returning it.
        """
        old_doc_string = ast.get_docstring(f)
        s = ast.Str("\n" + doc_string + "\n")
        new_doc_string_ast = ast.Expr(value=s)
        if old_doc_string:
            f.body[0] = new_doc_string_ast
        else:
            f.body.insert(0, new_doc_string_ast)
        return f

    def __add_return_states(self, f: ast.FunctionDef) -> ast.FunctionDef:
        """
        Adds a statement that returns app, internal and physical states as a dict to the end of the body of the given function, returning it.
        """
        keys = list(map(lambda n: ast.Constant(n), self.__app_states_names))
        keys.append(ast.Constant("physical_state"))
        keys.append(ast.Constant("internal_state"))
        values = list(map(lambda n: ast.Name(n, ast.Load), self.__app_states_names))
        values.append(ast.Name("physical_state", ast.Load))
        values.append(ast.Name("internal_state", ast.Load))
        return_value = ast.Dict(keys, values)
        f.body.append(ast.Return(return_value))
        return f

    def __check_if_func_call_is_applicable_and_replace(
        self,
        op: Union[
            ast.stmt,
            ast.expr,
            ast.comprehension,
            ast.keyword,
            ast.NamedExpr,
            List[ast.stmt],
            List[ast.expr],
            List[ast.comprehension],
            List[ast.keyword],
        ],
        unchecked_functions: List[UncheckedFunction],
    ) -> Union[
        ast.stmt,
        ast.expr,
        ast.comprehension,
        ast.keyword,
        ast.NamedExpr,
        List[ast.stmt],
        List[ast.expr],
        List[ast.comprehension],
        List[ast.keyword],
        None,
    ]:
        """
        Checks if the given op is a Function call and if that function is in the set of unchecked_function_names.
        If that's the case, it returns a new ast.Name with the function name as id if the function has a return type,
        otherwise it returns a ast.Constant with None. In all either cases, it returns the op.
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

            unchecked_func_names_to_return_type = {
                uf.name_with_app_name: uf.return_type for uf in unchecked_functions
            }
            if f_name in unchecked_func_names_to_return_type:
                if unchecked_func_names_to_return_type[f_name] != None:
                    # We replace the function call by a variable only if it returns something
                    return ast.Name(id=f_name)
                else:
                    # It does not return anything, we can replace it by None
                    return ast.Constant(None, "None")
        return op

    def __change_unchecked_functions_to_var(
        self,
        op: Union[
            ast.stmt,
            ast.expr,
            ast.comprehension,
            ast.keyword,
            ast.NamedExpr,
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
        if isinstance(op, list):
            for index, v in enumerate(op):
                temp_new_ast = self.__check_if_func_call_is_applicable_and_replace(
                    v, unchecked_functions
                )
                op[index] = temp_new_ast
                self.__change_unchecked_functions_to_var(v, unchecked_functions)
        elif isinstance(op, ast.List) or isinstance(op, ast.Tuple):
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__change_unchecked_functions_to_var(op.elts, unchecked_functions)
        elif isinstance(op, ast.BoolOp):
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__change_unchecked_functions_to_var(op.values, unchecked_functions)
        elif isinstance(op, ast.UnaryOp):
            op.operand = self.__check_if_func_call_is_applicable_and_replace(
                op.operand, unchecked_functions
            )
            self.__change_unchecked_functions_to_var(op.operand, unchecked_functions)
        elif isinstance(op, ast.NamedExpr):
            op.target = self.__check_if_func_call_is_applicable_and_replace(
                op.target, unchecked_functions
            )
            self.__change_unchecked_functions_to_var(op.target, unchecked_functions)
            op.value = self.__check_if_func_call_is_applicable_and_replace(
                op.value, unchecked_functions
            )
            self.__change_unchecked_functions_to_var(op.value, unchecked_functions)
        elif isinstance(op, ast.Expr):
            op.value = self.__check_if_func_call_is_applicable_and_replace(
                op.value, unchecked_functions
            )
            self.__change_unchecked_functions_to_var(op.value, unchecked_functions)
        elif isinstance(op, ast.Lambda):
            op.body = self.__check_if_func_call_is_applicable_and_replace(
                op.body, unchecked_functions
            )
            self.__change_unchecked_functions_to_var(op.body, unchecked_functions)
        elif isinstance(op, ast.Assign):
            op.targets = self.__check_if_func_call_is_applicable_and_replace(
                op.targets, unchecked_functions
            )
            self.__change_unchecked_functions_to_var(op.targets, unchecked_functions)
            op.value = self.__check_if_func_call_is_applicable_and_replace(
                op.value, unchecked_functions
            )
            self.__change_unchecked_functions_to_var(op.value, unchecked_functions)
        elif isinstance(op, ast.AugAssign):
            op.target = self.__check_if_func_call_is_applicable_and_replace(
                op.target, unchecked_functions
            )
            self.__change_unchecked_functions_to_var(op.target, unchecked_functions)
            op.value = self.__check_if_func_call_is_applicable_and_replace(
                op.value, unchecked_functions
            )
            self.__change_unchecked_functions_to_var(op.value, unchecked_functions)
        elif isinstance(op, ast.AnnAssign):
            op.target = self.__check_if_func_call_is_applicable_and_replace(
                op.target, unchecked_functions
            )
            self.__change_unchecked_functions_to_var(op.target, unchecked_functions)
            op.annotation = self.__check_if_func_call_is_applicable_and_replace(
                op.annotation, unchecked_functions
            )
            self.__change_unchecked_functions_to_var(op.annotation, unchecked_functions)
            op.value = self.__check_if_func_call_is_applicable_and_replace(
                op.value, unchecked_functions
            )
            self.__change_unchecked_functions_to_var(op.value, unchecked_functions)
        elif isinstance(op, ast.Return):
            if op.value:
                op.value = (
                    temp_new_ast
                ) = self.__check_if_func_call_is_applicable_and_replace(
                    op.value, unchecked_functions
                )
                self.__change_unchecked_functions_to_var(op.value, unchecked_functions)
        elif isinstance(op, ast.Compare):
            op.left = self.__check_if_func_call_is_applicable_and_replace(
                op.left, unchecked_functions
            )
            self.__change_unchecked_functions_to_var(op.left, unchecked_functions)
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__change_unchecked_functions_to_var(
                op.comparators, unchecked_functions
            )
        elif isinstance(op, ast.BinOp):
            op.left = self.__check_if_func_call_is_applicable_and_replace(
                op.left, unchecked_functions
            )
            self.__change_unchecked_functions_to_var(op.left, unchecked_functions)
            op.right = self.__check_if_func_call_is_applicable_and_replace(
                op.right, unchecked_functions
            )
            self.__change_unchecked_functions_to_var(op.right, unchecked_functions)
        elif isinstance(op, ast.IfExp):
            op.test = self.__check_if_func_call_is_applicable_and_replace(
                op.test, unchecked_functions
            )
            self.__change_unchecked_functions_to_var(op.test, unchecked_functions)
            op.body = self.__check_if_func_call_is_applicable_and_replace(
                op.body, unchecked_functions
            )
            self.__change_unchecked_functions_to_var(op.body, unchecked_functions)
            op.orelse = self.__check_if_func_call_is_applicable_and_replace(
                op.orelse, unchecked_functions
            )
            self.__change_unchecked_functions_to_var(op.orelse, unchecked_functions)
        elif isinstance(op, ast.If):
            op.test = self.__check_if_func_call_is_applicable_and_replace(
                op.test, unchecked_functions
            )
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
            op.iter = self.__check_if_func_call_is_applicable_and_replace(
                op.iter, unchecked_functions
            )
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
            op.value = self.__check_if_func_call_is_applicable_and_replace(
                op.value, unchecked_functions
            )
            self.__change_unchecked_functions_to_var(op.value, unchecked_functions)
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__change_unchecked_functions_to_var(op.generators, unchecked_functions)
        elif isinstance(op, ast.Yield) or isinstance(op, ast.YieldFrom):
            if op.value:
                op.value = self.__check_if_func_call_is_applicable_and_replace(
                    op.value, unchecked_functions
                )
                self.__change_unchecked_functions_to_var(op.value, unchecked_functions)
        elif isinstance(op, ast.FormattedValue):
            op.value = self.__check_if_func_call_is_applicable_and_replace(
                op.value, unchecked_functions
            )
            self.__change_unchecked_functions_to_var(op.value, unchecked_functions)
        elif isinstance(op, ast.JoinedStr):
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__change_unchecked_functions_to_var(op.values, unchecked_functions)
        elif isinstance(op, ast.Return):
            if op.value:
                op.value = self.__check_if_func_call_is_applicable_and_replace(
                    op.value, unchecked_functions
                )
                self.__change_unchecked_functions_to_var(op.value, unchecked_functions)
        elif isinstance(op, ast.Call):
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__change_unchecked_functions_to_var(op.args, unchecked_functions)
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__change_unchecked_functions_to_var(op.keywords, unchecked_functions)
            # Here we cannot have a Call to replace because it would have been done in the parent
            self.__change_unchecked_functions_to_var(op.func, unchecked_functions)
        elif isinstance(op, ast.keyword):
            op.value = self.__check_if_func_call_is_applicable_and_replace(
                op.value, unchecked_functions
            )
            self.__change_unchecked_functions_to_var(op.value, unchecked_functions)
        elif isinstance(op, ast.FunctionDef):
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__change_unchecked_functions_to_var(op.body, unchecked_functions)

    def manipulate_mains(self, verification: bool) -> Tuple[List[str], List[str]]:
        """
        Manipulates the `main.py` of all the apps, modifying the names of the functions and instances (specified in `accepted_names`),
        and adding the state argument to the calls. Then, the `invariant` and `iteration` functions are extracted, together with their imports,
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
            # Some imports might be a single string containing multiple imports separated by '\n'
            imps = (i for imp in imps for i in imp.split("\n"))
            imports.extend(imps)
            functions.append(funcs)

        # Keep only non-empty imports
        return [imp.replace("\n", "") for imp in imports if imp], functions

    def __check_no_invalid_calls_in_function(
        self, functions: List[ast.FunctionDef], invalid_func_names: Set[str]
    ) -> Tuple[bool, str]:
        """
        Checks that there are no calls to the given functions in the body of the given function definitions.
        """

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
                for o in (check(v) for v in list(op)):
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

                if f_name in invalid_func_names:
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

    def __add_unchecked_function_arguments(
        self, f: ast.FunctionDef, arguments: List[UncheckedFunction]
    ):
        """
        In place, adds to the given function the given arguments.
        """
        ast_arguments = map(
            lambda unchecked_f: ast.arg(
                unchecked_f.name_with_app_name,
                ast.Name(unchecked_f.return_type, ast.Load),
            ),
            filter(lambda uf: uf.return_type != None, arguments),
        )

        f.args.args.extend(ast_arguments)

    def __rename_files(
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
        filenames: Set[str],
    ):
        """
        In place, renames all the occurrences of the given filenames by prepending the path to it.
        """
        if isinstance(op, list) or isinstance(op, tuple):
            for v in list(op):
                self.__rename_files(v, app_name, filenames)
        elif isinstance(op, ast.List) or isinstance(op, ast.Tuple):
            self.__rename_files(op.elts, app_name, filenames)
        elif isinstance(op, ast.BoolOp):
            self.__rename_files(op.values, app_name, filenames)
        elif isinstance(op, ast.UnaryOp):
            self.__rename_files(op.operand, app_name, filenames)
        elif isinstance(op, ast.NamedExpr) or isinstance(op, ast.Expr):
            self.__rename_files(op.value, app_name, filenames)
        elif isinstance(op, ast.Lambda):
            self.__rename_files(op.body, app_name, filenames)
        elif isinstance(op, ast.Assign):
            self.__rename_files(op.value, app_name, filenames)
        elif isinstance(op, ast.Return):
            if op.value:
                self.__rename_files(op.value, app_name, filenames)
        elif isinstance(op, ast.Compare):
            self.__rename_files(op.left, app_name, filenames)
            self.__rename_files(op.comparators, app_name, filenames)
        elif isinstance(op, ast.BinOp):
            self.__rename_files(op.left, app_name, filenames)
            self.__rename_files(op.right, app_name, filenames)
        elif isinstance(op, ast.IfExp) or isinstance(op, ast.If):
            self.__rename_files(op.test, app_name, filenames)
            self.__rename_files(op.body, app_name, filenames)
            self.__rename_files(op.orelse, app_name, filenames)
        elif isinstance(op, ast.Dict):
            self.__rename_files(op.values, app_name, filenames)
        elif isinstance(op, ast.Set):
            self.__rename_files(op.elts, app_name, filenames)
        elif isinstance(op, ast.comprehension):
            self.__rename_files(op.iter, app_name, filenames)
            self.__rename_files(op.ifs, app_name, filenames)
        elif (
            isinstance(op, ast.ListComp)
            or isinstance(op, ast.SetComp)
            or isinstance(op, ast.GeneratorExp)
        ):
            self.__rename_files(op.generators, app_name, filenames)
        elif isinstance(op, ast.DictComp):
            self.__rename_files(op.value, app_name, filenames)
            self.__rename_files(op.generators, app_name, filenames)
        elif isinstance(op, ast.Yield) or isinstance(op, ast.YieldFrom):
            if op.value:
                self.__rename_files(op.value, app_name, filenames)
        elif isinstance(op, ast.FormattedValue):
            self.__rename_files(op.value, app_name, filenames)
        elif isinstance(op, ast.JoinedStr):
            self.__rename_files(op.values, app_name, filenames)
        elif isinstance(op, ast.Return):
            if op.value:
                self.__rename_files(op.value, app_name, filenames)
        elif isinstance(op, ast.Call):
            self.__rename_files(op.func, app_name, filenames)
            self.__rename_files(op.args, app_name, filenames)
            self.__rename_files(op.keywords, app_name, filenames)
        elif isinstance(op, ast.keyword):
            self.__rename_files(op.value, app_name, filenames)
        elif isinstance(op, ast.FunctionDef):
            self.__rename_files(op.body, app_name, filenames)
            if op.returns:
                self.__rename_files(op.returns, app_name, filenames)
        elif isinstance(op, ast.Constant):
            v = op.value
            if v in filenames:
                # We rename the file
                op.value = f"{self.__files_folder_path}/{app_name}/{v}"

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
            # We only keep invariant and iteration functions, and we add to them the docstring with the contracts
            functions_ast = list(
                map(
                    lambda f: self.__add_return_states(
                        self.__add_doc_string(
                            f,
                            self.__construct_contracts(
                                self.__app_names,
                                list(unchecked_func_dict.values()),
                                verification,
                            ),
                        )
                    )
                    if f.name == f"{app_name}_iteration" and verification
                    else f,
                    cast(
                        List[ast.FunctionDef],
                        filter(
                            lambda n: isinstance(n, ast.FunctionDef)
                            and (
                                n.name == f"{app_name}_{self.__INVARIANT_FUNC_NAME}"
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
            # external libraries in iteration or invariant functions
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
                    and n.module != "instances",
                    module_body,
                )
            )
            return functions_ast, imports_ast, from_imports_ast

        with open(f"{directory}/{app_name}/main.py", "r") as file:
            module = ast.parse(file.read())
            module_body = module.body

            unchecked_func_dict = self.__get_unchecked_functions(module_body, app_name)
            unchecked_funcs = list(unchecked_func_dict.values())

            # We rename all the files, if any
            filenames = self.__filenames_per_app[app_name]
            if filenames:
                self.__rename_files(module_body, app_name, filenames)

            # We rename all the device instances and add the state argument to each of their calls
            self.__rename_instances_add_state(
                module_body, app_name, accepted_names, unchecked_funcs, verification
            )

            # Extract imports, invariant/iteration functions and add the contracts to them
            (
                functions_ast,
                imports_ast,
                from_imports_ast,
            ) = extract_functions_and_imports(module_body, unchecked_func_dict)

            # Contains all the invalid function names: these are not allowed in invariant or iteration functions
            invalid_func_names = {self.__PRINT_FUNC_NAME}

            # Check if an invariant function contains a call to an invalid function,
            # i.e. an unchecked function or print
            unchecked_func_names = {
                uf.name_with_app_name for _, uf in unchecked_func_dict.items()
            }
            invariant_invalid_func_names = unchecked_func_names | invalid_func_names
            valid, wrong_invariant_func = self.__check_no_invalid_calls_in_function(
                list(
                    filter(
                        lambda f: f.name.endswith(f"_{self.__INVARIANT_FUNC_NAME}"),
                        functions_ast,
                    )
                ),
                invariant_invalid_func_names,
            )
            if not valid:
                raise InvalidFunctionCallException(
                    f"The invariant function '{wrong_invariant_func}' contains a call to an unchecked function or to 'print'."
                )

            iteration_functions = list(
                filter(
                    lambda f: f.name.endswith(f"_{self.__ITERATION_FUNC_NAME}"),
                    functions_ast,
                )
            )

            # Check if an iteration function contains a call to print
            valid, wrong_iteration_func = self.__check_no_invalid_calls_in_function(
                iteration_functions,
                invalid_func_names,
            )
            if not valid:
                raise InvalidFunctionCallException(
                    f"The iteration function '{wrong_iteration_func}' contains a call to 'print'."
                )

            if verification:
                # Replace all calls to unchecked functions by variables in iteration function
                self.__change_unchecked_functions_to_var(
                    iteration_functions,
                    unchecked_funcs,
                )

                # Add unchecked_function_names as arguments of the iteration functions
                for f in iteration_functions:
                    self.__add_unchecked_function_arguments(f, unchecked_funcs)

            # Transform to source code
            functions = astor.to_source(ast.Module(functions_ast))
            imports = astor.to_source(ast.Module(imports_ast))
            from_imports = astor.to_source(ast.Module(from_imports_ast))
            return [imports, from_imports], functions
