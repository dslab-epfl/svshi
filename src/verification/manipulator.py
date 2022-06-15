import ast
import copy
import re
import astor
from dataclasses import dataclass
from typing import Dict, List, Optional, Set, Tuple, Type, Union, Final, cast


def _print_and_raise(msg: str, exception: Type[Exception]):
    """
    Utility function to print `msg` (to pass it to the scala module), before raising
    the exception with that same `msg`.
    """
    print(msg)  # To pass it to the scala module
    raise exception(msg)


class InvalidFunctionCallException(Exception):
    """
    An invalid function call exception.
    """


class DirectCallToIsolatedFunctionException(Exception):
    """
    An on_trigger_ or periodic_ function should not be directly called.
    """


class UntypedIsolatedFunctionException(Exception):
    """
    An on_trigger_ or periodic_ function has no return type.
    """


class UnallowedArgsInIsolatedFunctionException(Exception):
    """
    Isolated functions are not allowed to have any *args or **kwargs or default args.
    """


class InvalidPeriodicFunctionException(Exception):
    """
    A periodic function should have no arguments as input.
    A periodic function should have a period.
    """


class InvalidTriggerIfNotRunningCallException(Exception):
    """
    Calls to `trigger_if_not_running` should have the correct args.
    It should only be called on an `on_trigger` function.
    """


class InvalidGetLatestValueCallException(Exception):
    """
    Calls to `get_latest_value` should have exactly one argument: an `on_trigger` or a
    `periodic` function.
    """


class InvalidFileOpenModeException(Exception):
    """
    A mode passed to a call of functions of svshi_api to open a file
    """


class InvalidFileOpenModeException(Exception):
    """
    A mode passed to a call of functions of svshi_api to open a file
    """


class ForbiddenModuleImported(Exception):
    """
    A forbidden module was imported by the user in one app
    """


@dataclass
class IsolatedFunction:
    """
    An isolated function.
    """

    name: str
    name_with_app_name: str
    period: Optional[int]
    return_type: str
    args: ast.arguments


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
    __ISOLATED_VALUES_TYPE: Final = "IsolatedFunctionsValues"
    __ISOLATED_VALUES_ARGUMENT: Final = "isolated_fn_values"
    __ON_TRIGGER_FUNC_PREFIX: Final = "on_trigger"
    __PERIODIC_FUNC_PREFIX: Final = "periodic"
    __ISOLATED_FUNC_PREFIXES: Final = (__ON_TRIGGER_FUNC_PREFIX, __PERIODIC_FUNC_PREFIX)
    __ISOLATED_FUNC_ALLOWED_RETURN_TYPES: Final = {None, "bool", "int", "float", "str"}
    __INVARIANT_FUNC_NAME: Final = "invariant"
    __ITERATION_FUNC_NAME: Final = "iteration"
    __PRINT_FUNC_NAME: Final = "print"
    __TRIGGER_IF_NOT_RUNNING_NAME: Final = "trigger_if_not_running"
    __GET_LATEST_VALUE_NAME: Final = "get_latest_value"
    __OPEN_FUNC_NAME: Final = "open"
    __SYSTEM_BEHAVIOUR_FUNC_NAME: Final = "system_behaviour"
    __SVSHI_API_INSTANCE_NAME: Final = "svshi_api"
    __SVSHI_API_GET_FILE_TEXT_API_FUNC_NAME: Final = "get_file_text_mode"
    __SVSHI_API_GET_FILE_BINARY_API_FUNC_NAME: Final = "get_file_binary_mode"
    __SVSHI_API_GET_FILEPATH_API_FUNC_NAME: Final = "get_file_path"
    __SVSHI_API_FILESYSTEM_FUNCTION_NAMES: Final = [
        __SVSHI_API_GET_FILE_TEXT_API_FUNC_NAME,
        __SVSHI_API_GET_FILE_BINARY_API_FUNC_NAME,
        __SVSHI_API_GET_FILEPATH_API_FUNC_NAME,
    ]
    __SVSHI_API_FILESYSTEM_OPEN_FILE_MODES = ["w", "r", "a", "wr", "ar", "ra", "rw"]

    __SVSHI_API_FUNCTIONS_WITH_INTERNAL_STATE: Final = [
        "get_hour_of_the_day",
        "get_minute_in_hour",
        "get_day_of_week",
        "get_day_of_month",
        "get_month_in_year",
        "get_year",
        *__SVSHI_API_FILESYSTEM_FUNCTION_NAMES,
    ]

    __PERIOD_REGX: Final = re.compile(r"^\s*period:\s*(\d+)")
    __FORBIDDEN_MODULE_IN_APPS = ["time"]

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

    def __period_from_docstring(self, docstring: str) -> int:
        """Return the `period:` value of a docstring."""
        lines = docstring.splitlines()
        matches = list(filter(None, map(self.__PERIOD_REGX.match, lines)))
        if not matches:
            _print_and_raise(
                "Periodic function has no period specified in the docstring. "
                "If you wish to execute as often as possible, use `period: 0`.",
                InvalidPeriodicFunctionException,
            )
        if len(matches) > 1:
            _print_and_raise(
                "Periodic function has multiple periods defined in the docstring. "
                "Only one is allowed.",
                InvalidPeriodicFunctionException,
            )
        return int(matches[0].group(1))

    def __get_isolated_functions(
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
    ) -> Dict[str, IsolatedFunction]:
        """
        Goes through the AST and returns a Dict containing entries of the form
        "isolated_func_name" -> IsolatedFunction.
        """
        if isinstance(op, (list, tuple)):
            dicts_list = [self.__get_isolated_functions(v, app_name) for v in list(op)]
            res = {}
            for d in dicts_list:
                for k in d.keys():
                    res[k] = d[k]
            return res
        elif isinstance(op, (ast.List, ast.Tuple)):
            return self.__get_isolated_functions(op.elts, app_name)
        elif isinstance(op, ast.FunctionDef) and (
            op.name.startswith(self.__ISOLATED_FUNC_PREFIXES)
        ):
            doc_string = ast.get_docstring(op)
            doc_string = "" if doc_string == None else doc_string
            return_type = op.returns
            if not return_type:
                _print_and_raise(
                    f"The function '{op.name}' has no return type.",
                    UntypedIsolatedFunctionException,
                )
            if not isinstance(return_type, (ast.Name, ast.Constant)):
                _print_and_raise(
                    f"The function '{op.name}' has a return type which is not allowed."
                    f" Allowed return types: {self.__ISOLATED_FUNC_ALLOWED_RETURN_TYPES}",
                    UntypedIsolatedFunctionException,
                )
            # When the return type is 'None', return_type is ast.Constant
            return_type_str = (
                cast(ast.Name, return_type).id
                if isinstance(return_type, ast.Name)
                else cast(ast.Constant, return_type).value
            )
            if return_type_str != None and len(return_type_str) == 0:
                _print_and_raise(
                    f"The function '{op.name}' has no return type.",
                    UntypedIsolatedFunctionException,
                )
            # We only allow str, int, float, bool or None as return types.
            # Otherwise, we might need additional imports and verification might timeout.
            if return_type_str not in self.__ISOLATED_FUNC_ALLOWED_RETURN_TYPES:
                _print_and_raise(
                    f"The function '{op.name}' has return type {return_type_str}, "
                    "which is not allowed. Allowed return types: "
                    f"{self.__ISOLATED_FUNC_ALLOWED_RETURN_TYPES}",
                    UntypedIsolatedFunctionException,
                )
            func_name = op.name
            args = op.args
            # We don't allow *args, **kwargs and default values for isolated functions.
            if args.vararg or args.kwarg or args.defaults:
                _print_and_raise(
                    f"Function {func_name} is invalid: *args, **kwargs and default "
                    "values are not allowed for `periodic` and `on_trigger` functions.",
                    UnallowedArgsInIsolatedFunctionException,
                )
            if func_name.startswith(self.__PERIODIC_FUNC_PREFIX) and (
                args.args or args.kwonlyargs
            ):
                _print_and_raise(
                    f"Function {func_name} is periodic and is not allowed to have any "
                    "argument.",
                    InvalidPeriodicFunctionException,
                )
            new_func_name = f"{app_name}_{func_name}"
            period = None
            if func_name.startswith(self.__PERIODIC_FUNC_PREFIX):
                period = self.__period_from_docstring(doc_string)
            return {
                func_name: IsolatedFunction(
                    func_name, new_func_name, period, return_type_str, args
                )
            }
        else:
            return {}

    def _check_coherent_fn_call_to_fn_def(
        self,
        call_args: List[ast.expr],
        call_keywords: List[ast.keyword],
        fn_def_args: ast.arguments,
        fn_name: str,
    ):
        """
        Check that the args `call_args`and `call_kwarg_names` of a call are accepted
        by the function whose args are `fn_def_args`.
        Note: we do not allow default values, *args and **kwargs (see __get_isolated_functions).
        """
        call_kwarg_names = {x.arg for x in call_keywords}

        # We don't allow calling the function with *args or **kwargs.
        if (None in call_kwarg_names) or any(
            map(lambda x: isinstance(x, ast.Starred), call_args)
        ):
            _print_and_raise(
                f"You are giving *args or **kwargs when triggering function {fn_name}, "
                "which is not allowed. Provide all arguments separately instead.",
                InvalidTriggerIfNotRunningCallException,
            )

        ###########################
        # Check keyword arguments #
        ###########################

        # Check all keyword-only args are satisfied.
        kwarg_names_def = {x.arg for x in fn_def_args.kwonlyargs}
        if not kwarg_names_def <= call_kwarg_names:
            _print_and_raise(
                f"Trigger to {fn_name} is missing some keyword-only arguments: "
                f"{kwarg_names_def - call_kwarg_names}",
                InvalidTriggerIfNotRunningCallException,
            )
        # Remove supplied kwargs which were just used.
        call_kwarg_names = call_kwarg_names - kwarg_names_def

        # Check all given kwargs correspond to some argument in the function def.
        args_names_def = [x.arg for x in fn_def_args.args]
        if not call_kwarg_names <= set(args_names_def):
            _print_and_raise(
                f"Trigger to {fn_name} is supplied unknown keyword arguments: "
                f"{call_kwarg_names - set(args_names_def)}",
                InvalidTriggerIfNotRunningCallException,
            )
        # Note: We don't need to check that no positional arg follows a keyword arg,
        # since the ast parser will already throw a SyntaxError in that case.

        ##############################
        # Check positional arguments #
        ##############################

        # Check that it was not called with too few or too many arguments.
        num_expected_args = (
            len(args_names_def) + len(fn_def_args.posonlyargs) - len(call_kwarg_names)
        )
        if len(call_args) < num_expected_args:
            _print_and_raise(
                f"Trigger to {fn_name} is missing some positional arguments.",
                InvalidTriggerIfNotRunningCallException,
            )
        if len(call_args) > num_expected_args:
            _print_and_raise(
                f"Trigger to {fn_name} has too many positional arguments.",
                InvalidTriggerIfNotRunningCallException,
            )

    def __check_coherent_call_to_trigger_if_not_running(
        self,
        op: ast.Call,
        on_trigger_fn_name: str,
        isolated_functions: List[IsolatedFunction],
    ) -> None:
        """
        Check that the given call to svshi_api.trigger_if_not_running is correct.
        This means, it should be called on an existing on_trigger function with the
        correct arguments.
        """
        args = op.args[1:]

        # Find the corresponding IsolatedFunction
        iso_fn = list(
            filter(lambda x: x.name == on_trigger_fn_name, isolated_functions)
        )
        if not len(iso_fn):
            _print_and_raise(
                "Incorrect call to svshi_api.trigger_if_not_running: "
                f"on_trigger function {on_trigger_fn_name} does not exist!",
                InvalidTriggerIfNotRunningCallException,
            )
        iso_fn_args = iso_fn[0].args

        self._check_coherent_fn_call_to_fn_def(
            args, op.keywords, iso_fn_args, on_trigger_fn_name
        )

    def __check_and_rename_isolated_fn_call(
        self,
        op: ast.Call,
        app_name: str,
        method_name: str,
        isolated_functions: List[IsolatedFunction],
    ) -> None:
        """
        Check that calls to svshi_api.get_value and svshi_api.trigger_if_not_running
        are correct.
        """
        is_trigger_fn = method_name == self.__TRIGGER_IF_NOT_RUNNING_NAME
        if is_trigger_fn:
            if not len(op.args):
                _print_and_raise(
                    "Incorrect call to svshi_api.trigger_if_not_running: "
                    "no arguments provided.",
                    InvalidTriggerIfNotRunningCallException,
                )
        else:
            if len(op.args) != 1:
                _print_and_raise(
                    "Incorrect call to svshi_api.get_latest_value: "
                    f"Expecting exactly one argument. Found: {len(op.args)}",
                    InvalidGetLatestValueCallException,
                )
        isolated_fn_name = cast(ast.Name, op.args[0])
        if is_trigger_fn:
            if not isolated_fn_name.id.startswith(self.__ON_TRIGGER_FUNC_PREFIX):
                _print_and_raise(
                    "Incorrect call to svshi_api.trigger_if_not_running: "
                    "should only be called on `on_trigger` functions. "
                    f"Found: {isolated_fn_name.id}",
                    InvalidTriggerIfNotRunningCallException,
                )
            self.__check_coherent_call_to_trigger_if_not_running(
                op, isolated_fn_name.id, isolated_functions
            )
        else:
            if not isolated_fn_name.id.startswith(self.__ISOLATED_FUNC_PREFIXES):
                _print_and_raise(
                    "Incorrect call to svshi_api.get_latest_value: "
                    "shoudl only be called on `on_trigger` and "
                    f"`periodic` functions. Found {isolated_fn_name.id}",
                    InvalidGetLatestValueCallException,
                )
        # Rename the isolated function: prepend the application name.
        isolated_fn_name.id = f"{app_name}_{isolated_fn_name.id}"

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
        isolated_functions: List[IsolatedFunction],
        verification: bool,
    ):
        """
        Renames the instances calling functions with name in accepted_names
        and on_trigger and periodic functions by adding the app_name in front of them.
        Additionally, it also adds the "state" argument to the calls with name in accepted_names.
        Furthermore, "invariant", "iteration", "on_trigger" and "periodic" functions are
        modified with the app_name added to them and with the state parameters added for
        the first two.
        The internal_state argument is added to all SvshiApi functions calls of functions in the list __SVSHI_API_FUNCTIONS_WITH_INTERNAL_STATE
        The internal_state arguments is added to all isolated functions as the last argument
        """
        if isinstance(op, (list, tuple)):
            for v in list(op):
                self.__rename_instances_add_state(
                    v, app_name, accepted_names, isolated_functions, verification
                )
        elif isinstance(op, (ast.List, ast.Tuple)):
            self.__rename_instances_add_state(
                op.elts, app_name, accepted_names, isolated_functions, verification
            )
        elif isinstance(op, ast.BoolOp):
            self.__rename_instances_add_state(
                op.values, app_name, accepted_names, isolated_functions, verification
            )
        elif isinstance(op, ast.UnaryOp):
            self.__rename_instances_add_state(
                op.operand, app_name, accepted_names, isolated_functions, verification
            )
        elif isinstance(op, ast.NamedExpr):
            self.__rename_instances_add_state(
                op.target, app_name, accepted_names, isolated_functions, verification
            )
            self.__rename_instances_add_state(
                op.value, app_name, accepted_names, isolated_functions, verification
            )
        elif isinstance(op, ast.Expr):
            self.__rename_instances_add_state(
                op.value, app_name, accepted_names, isolated_functions, verification
            )
        elif isinstance(op, ast.Lambda):
            self.__rename_instances_add_state(
                op.body, app_name, accepted_names, isolated_functions, verification
            )
        elif isinstance(op, ast.Assign):
            self.__rename_instances_add_state(
                op.targets, app_name, accepted_names, isolated_functions, verification
            )
            self.__rename_instances_add_state(
                op.value, app_name, accepted_names, isolated_functions, verification
            )
        elif isinstance(op, ast.AugAssign):
            self.__rename_instances_add_state(
                op.target, app_name, accepted_names, isolated_functions, verification
            )
            self.__rename_instances_add_state(
                op.value, app_name, accepted_names, isolated_functions, verification
            )
        elif isinstance(op, ast.AnnAssign):
            self.__rename_instances_add_state(
                op.target, app_name, accepted_names, isolated_functions, verification
            )
            self.__rename_instances_add_state(
                op.annotation,
                app_name,
                accepted_names,
                isolated_functions,
                verification,
            )
            if op.value:
                self.__rename_instances_add_state(
                    op.value,
                    app_name,
                    accepted_names,
                    isolated_functions,
                    verification,
                )
        elif isinstance(op, ast.Return):
            if op.value:
                self.__rename_instances_add_state(
                    op.value,
                    app_name,
                    accepted_names,
                    isolated_functions,
                    verification,
                )
        elif isinstance(op, ast.Compare):
            self.__rename_instances_add_state(
                op.left, app_name, accepted_names, isolated_functions, verification
            )
            self.__rename_instances_add_state(
                op.comparators,
                app_name,
                accepted_names,
                isolated_functions,
                verification,
            )
        elif isinstance(op, ast.BinOp):
            self.__rename_instances_add_state(
                op.left, app_name, accepted_names, isolated_functions, verification
            )
            self.__rename_instances_add_state(
                op.right, app_name, accepted_names, isolated_functions, verification
            )
        elif isinstance(op, (ast.IfExp, ast.If)):
            self.__rename_instances_add_state(
                op.test, app_name, accepted_names, isolated_functions, verification
            )
            self.__rename_instances_add_state(
                op.body, app_name, accepted_names, isolated_functions, verification
            )
            self.__rename_instances_add_state(
                op.orelse, app_name, accepted_names, isolated_functions, verification
            )
        elif isinstance(op, ast.Dict):
            self.__rename_instances_add_state(
                op.values, app_name, accepted_names, isolated_functions, verification
            )
        elif isinstance(op, ast.Set):
            self.__rename_instances_add_state(
                op.elts, app_name, accepted_names, isolated_functions, verification
            )
        elif isinstance(op, ast.comprehension):
            self.__rename_instances_add_state(
                op.iter, app_name, accepted_names, isolated_functions, verification
            )
            self.__rename_instances_add_state(
                op.ifs, app_name, accepted_names, isolated_functions, verification
            )
        elif isinstance(op, (ast.ListComp, ast.SetComp, ast.GeneratorExp)):
            self.__rename_instances_add_state(
                op.generators,
                app_name,
                accepted_names,
                isolated_functions,
                verification,
            )
        elif isinstance(op, ast.DictComp):
            self.__rename_instances_add_state(
                op.value, app_name, accepted_names, isolated_functions, verification
            )
            self.__rename_instances_add_state(
                op.generators,
                app_name,
                accepted_names,
                isolated_functions,
                verification,
            )
        elif isinstance(op, (ast.Yield, ast.YieldFrom)):
            if op.value:
                self.__rename_instances_add_state(
                    op.value,
                    app_name,
                    accepted_names,
                    isolated_functions,
                    verification,
                )
        elif isinstance(op, ast.FormattedValue):
            self.__rename_instances_add_state(
                op.value, app_name, accepted_names, isolated_functions, verification
            )
        elif isinstance(op, ast.JoinedStr):
            self.__rename_instances_add_state(
                op.values, app_name, accepted_names, isolated_functions, verification
            )
        elif isinstance(op, ast.Return):
            if op.value:
                self.__rename_instances_add_state(
                    op.value,
                    app_name,
                    accepted_names,
                    isolated_functions,
                    verification,
                )
        elif isinstance(op, ast.Call):
            self.__rename_instances_add_state(
                op.args, app_name, accepted_names, isolated_functions, verification
            )
            self.__rename_instances_add_state(
                op.keywords, app_name, accepted_names, isolated_functions, verification
            )
            # Placeholder values that can never occur to detect the two cases without flags
            f_name = "123"
            module_name = "456"
            if isinstance(op.func, ast.Attribute):
                # call of the form obj.func(...) let's
                # op.func.value is a ast.Name in this case
                module_name = cast(ast.Name, op.func.value).id
                f = op.func
                f_name = f.attr
                # functions of the svshi_api object is called
                if (
                    isinstance(f.value, ast.Name)
                    and module_name == self.__SVSHI_API_INSTANCE_NAME
                ):
                    method_name = f.attr
                    # functions of the svshi_api object involving internal_state is called
                    if method_name in self.__SVSHI_API_FUNCTIONS_WITH_INTERNAL_STATE:
                        op.args.append(
                            ast.Name(self.__INTERNAL_STATE_ARGUMENT, ast.Load)
                        )
                    # If we have svshi_api.get_latest_value or
                    # svshi_api.trigger_if_not_running,
                    # rename the function called.
                    if method_name in {
                        self.__GET_LATEST_VALUE_NAME,
                        self.__TRIGGER_IF_NOT_RUNNING_NAME,
                    }:
                        self.__check_and_rename_isolated_fn_call(
                            op, app_name, method_name, isolated_functions
                        )

            elif isinstance(op.func, ast.Name):
                # call of the form func(...)
                f_name = op.func.id
            else:
                self.__rename_instances_add_state(
                    op.func, app_name, accepted_names, isolated_functions, verification
                )
            if module_name != "456":
                # Composite call so op.func is an ast.Attribute
                if module_name in accepted_names:
                    # If the function name is in the list of accepted names, add the state argument to the call
                    op.args.append(ast.Name(self.__PHYSICAL_STATE_ARGUMENT, ast.Load))
                    if verification:
                        op.args.append(
                            ast.Name(self.__INTERNAL_STATE_ARGUMENT, ast.Load)
                        )

                    # Rename the instance calling the function, adding the app name to it
                    new_name = f"{app_name.upper()}_{module_name}"
                    cast(ast.Name, cast(ast.Attribute, op.func).value).id = new_name
            else:
                # Simple call so op.func is an ast.Name
                if f_name in accepted_names:
                    # If the function name is in the list of accepted names, add the state argument to the call
                    op.args.append(ast.Name(self.__PHYSICAL_STATE_ARGUMENT, ast.Load))
                    if verification:
                        op.args.append(
                            ast.Name(self.__INTERNAL_STATE_ARGUMENT, ast.Load)
                        )

                    # Rename the instance calling the function, adding the app name to it
                    new_name = f"{app_name.upper()}_{f_name}"
                    cast(ast.Name, op.func).id = new_name

                elif f_name in {uf.name for uf in isolated_functions}:
                    _print_and_raise(
                        f"{f_name} should not be called directly. "
                        f"Use svshi_api.get_latest_value({f_name}) to get its value. "
                        f"Use svshi_api.trigger_if_not_alredy_running to execute `on_trigger` functions.",
                        DirectCallToIsolatedFunctionException,
                    )

        elif isinstance(op, ast.keyword):
            self.__rename_instances_add_state(
                op.value, app_name, accepted_names, isolated_functions, verification
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
            or op.name.startswith(self.__ISOLATED_FUNC_PREFIXES)
        ):
            if (
                op.name == self.__INVARIANT_FUNC_NAME
                or op.name == self.__ITERATION_FUNC_NAME
            ):
                # Add the state arguments to the function
                # For the verification file we pass all the app states, for the runtime one just one is enough
                only_app_state_app_name = "" if verification else app_name
                self.__add_states_arguments_to_f(op, only_app_state_app_name)

            # Rename the function, adding the app name to it
            op.name = f"{app_name}_{op.name}"
            self.__rename_instances_add_state(
                op.body, app_name, accepted_names, isolated_functions, verification
            )

    def __add_states_arguments_to_f(
        self, f: ast.FunctionDef, only_app_state_app_name: str
    ) -> ast.FunctionDef:
        """
        Adds in place the states (PhysicalState, AppState, InternalState, IsolatedFunctionsValues) to the function arguments.
        only_app_state_app_name is used if only one app state should be added, in that case this arg is the name of said app; it must equal "" to add all app_states
        IsolatedFunctionsValues is not added to invariant functions.
        returns the FunctionDef for convenience
        """
        f_with_args = self.__add_app_states_to_fun_def(f, only_app_state_app_name)
        f_with_args = self.__add_physical_state_to_fun_def(f_with_args)
        f_with_args = self.__add_internal_state_to_fun_def(f_with_args)
        # IsolatedFunctionsValues is not added to invariant functions.
        if f_with_args.name != self.__INVARIANT_FUNC_NAME:
            f_with_args = self.__add_isolated_values_to_fun_def(f_with_args)
        return f_with_args

    def __add_app_states_to_fun_def(
        self, f: ast.FunctionDef, only_app_state_app_name: str
    ) -> ast.FunctionDef:
        state_args = (
            list(
                map(
                    lambda n: ast.arg(
                        n,
                        ast.Name(self.__APP_STATE_TYPE, ast.Load),
                    ),
                    filter(
                        lambda n: n.startswith(only_app_state_app_name),
                        self.__app_states_names,
                    ),
                )
            )
            if only_app_state_app_name != ""
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
        f.args.args.extend(state_args)
        return f

    def __add_physical_state_to_fun_def(self, f: ast.FunctionDef) -> ast.FunctionDef:
        args = [
            (
                ast.arg(
                    self.__PHYSICAL_STATE_ARGUMENT,
                    ast.Name(self.__PHYSICAL_STATE_TYPE, ast.Load),
                )
            )
        ]
        f.args.args.extend(args)
        return f

    def __add_internal_state_to_fun_def(self, f: ast.FunctionDef) -> ast.FunctionDef:
        args = [
            (
                ast.arg(
                    self.__INTERNAL_STATE_ARGUMENT,
                    ast.Name(self.__INTERNAL_STATE_TYPE, ast.Load),
                )
            )
        ]
        f.args.args.extend(args)
        return f

    def __add_isolated_values_to_fun_def(self, f: ast.FunctionDef) -> ast.FunctionDef:
        args = [
            (
                ast.arg(
                    self.__ISOLATED_VALUES_ARGUMENT,
                    ast.Name(self.__ISOLATED_VALUES_TYPE, ast.Load),
                )
            )
        ]
        f.args.args.extend(args)
        return f

    def __construct_contracts(
        self,
        app_names: List[str],
        verification: bool = True,
    ) -> str:
        """
        Returns the contract as a docstring using the given app names.
        """
        pre_str = "pre: "
        post_str = "post: "
        return_value_name_str = "**__return__"

        def construct_func_call(func_name: str, arg_names: List[str]) -> str:
            res = func_name + "("
            for i in range(len(arg_names)):
                res += arg_names[i] + ", "
            if len(res) > 2:
                res = res[:-2]
            res += ")"
            return res

        conditions = []
        sorted_app_names = sorted(app_names)
        for app_name in sorted_app_names:
            arg_names = (
                list(filter(lambda n: n.startswith(app_name), self.__app_states_names))
                if not verification
                else list(self.__app_states_names)
            )
            arg_names.append(self.__PHYSICAL_STATE_ARGUMENT)
            if verification:
                arg_names.append(self.__INTERNAL_STATE_ARGUMENT)
            invariant = pre_str
            invariant += construct_func_call(
                app_name + "_" + self.__INVARIANT_FUNC_NAME,
                arg_names,
            )
            conditions.append(invariant)

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

    def __remove_doc_string(self, f: ast.FunctionDef) -> ast.FunctionDef:
        """
        Remove the docstring of f is there is one, in place
        returns the FunctionDef for convenience
        """
        doc_string = ast.get_docstring(f)
        if doc_string:
            f.body = f.body[1:]
        return f

    def __add_return_states(self, f: ast.FunctionDef) -> ast.FunctionDef:
        """
        Modify the function in place to add a statement that returns all app states, the internal state and the physical state as a dict to the end of the body of the given function. It returns the modified function for convenience.
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
        verification: bool,
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
        Checks if the given op is a Function call and if that function is
        `svshi_api.get_latest_value` or `svshi_api.trigger_if_not_running`.
        If it is `get_latest_value(fn), it returns a new ast.Attribute representing
        `IsolatedFunctionsValues.app_name_fn_name`.
        `trigger_if_not_running` is replaced by None at verification time and
        is not changed at all for runtime.
        otherwise it returns a ast.Constant with None. In all either cases, it returns the op.
        """
        if isinstance(op, ast.Call) and isinstance(op.func, ast.Attribute):
            # op.func.value is an ast.Name in this case
            if cast(ast.Name, op.func.value).id == self.__SVSHI_API_INSTANCE_NAME:
                f_name = op.func.attr
                if f_name == self.__GET_LATEST_VALUE_NAME:
                    isolated_fn_name = cast(ast.Name, op.args[0]).id
                    return ast.Attribute(
                        value=ast.Name(
                            id=self.__ISOLATED_VALUES_ARGUMENT, ctx=ast.Load
                        ),
                        attr=isolated_fn_name,
                        ctx=ast.Load,
                    )
                elif f_name == self.__TRIGGER_IF_NOT_RUNNING_NAME:
                    if verification:
                        return ast.Constant(None, "None")
        return op

    def __replace_calls_to_isolated_functions(
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
        verification: bool,
    ):
        """
        Manipulates the op to replace all calls to `svshi_api.get_latest_value(fn)` by
        `IsolatedFunctionsValues.app_name_fn_name`. Calls to
        `svshi_api.trigger_if_not_running` are replaced by None for
        verification, but are not changed for the runtime.
        The replacement is done in place, it modifies the object.
        """
        if isinstance(op, list):
            for index, v in enumerate(op):
                temp_new_ast = self.__check_if_func_call_is_applicable_and_replace(
                    v, verification
                )
                op[index] = temp_new_ast
                self.__replace_calls_to_isolated_functions(v, verification)
        elif isinstance(op, (ast.List, ast.Tuple)):
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__replace_calls_to_isolated_functions(op.elts, verification)
        elif isinstance(op, ast.BoolOp):
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__replace_calls_to_isolated_functions(op.values, verification)
        elif isinstance(op, ast.UnaryOp):
            op.operand = self.__check_if_func_call_is_applicable_and_replace(
                op.operand, verification
            )
            self.__replace_calls_to_isolated_functions(op.operand, verification)
        elif isinstance(op, ast.NamedExpr):
            op.target = self.__check_if_func_call_is_applicable_and_replace(
                op.target, verification
            )
            self.__replace_calls_to_isolated_functions(op.target, verification)
            op.value = self.__check_if_func_call_is_applicable_and_replace(
                op.value, verification
            )
            self.__replace_calls_to_isolated_functions(op.value, verification)
        elif isinstance(op, ast.Expr):
            op.value = self.__check_if_func_call_is_applicable_and_replace(
                op.value, verification
            )
            self.__replace_calls_to_isolated_functions(op.value, verification)
        elif isinstance(op, ast.Lambda):
            op.body = self.__check_if_func_call_is_applicable_and_replace(
                op.body, verification
            )
            self.__replace_calls_to_isolated_functions(op.body, verification)
        elif isinstance(op, ast.Assign):
            op.targets = self.__check_if_func_call_is_applicable_and_replace(
                op.targets, verification
            )
            self.__replace_calls_to_isolated_functions(op.targets, verification)
            op.value = self.__check_if_func_call_is_applicable_and_replace(
                op.value, verification
            )
            self.__replace_calls_to_isolated_functions(op.value, verification)
        elif isinstance(op, ast.AugAssign):
            op.target = self.__check_if_func_call_is_applicable_and_replace(
                op.target, verification
            )
            self.__replace_calls_to_isolated_functions(op.target, verification)
            op.value = self.__check_if_func_call_is_applicable_and_replace(
                op.value, verification
            )
            self.__replace_calls_to_isolated_functions(op.value, verification)
        elif isinstance(op, ast.AnnAssign):
            op.target = self.__check_if_func_call_is_applicable_and_replace(
                op.target, verification
            )
            self.__replace_calls_to_isolated_functions(op.target, verification)
            op.annotation = self.__check_if_func_call_is_applicable_and_replace(
                op.annotation, verification
            )
            self.__replace_calls_to_isolated_functions(op.annotation, verification)
            op.value = self.__check_if_func_call_is_applicable_and_replace(
                op.value, verification
            )
            self.__replace_calls_to_isolated_functions(op.value, verification)
        elif isinstance(op, ast.Return):
            if op.value:
                op.value = (
                    temp_new_ast
                ) = self.__check_if_func_call_is_applicable_and_replace(
                    op.value, verification
                )
                self.__replace_calls_to_isolated_functions(op.value, verification)
        elif isinstance(op, ast.Compare):
            op.left = self.__check_if_func_call_is_applicable_and_replace(
                op.left, verification
            )
            self.__replace_calls_to_isolated_functions(op.left, verification)
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__replace_calls_to_isolated_functions(op.comparators, verification)
        elif isinstance(op, ast.BinOp):
            op.left = self.__check_if_func_call_is_applicable_and_replace(
                op.left, verification
            )
            self.__replace_calls_to_isolated_functions(op.left, verification)
            op.right = self.__check_if_func_call_is_applicable_and_replace(
                op.right, verification
            )
            self.__replace_calls_to_isolated_functions(op.right, verification)
        elif isinstance(op, ast.IfExp):
            op.test = self.__check_if_func_call_is_applicable_and_replace(
                op.test, verification
            )
            self.__replace_calls_to_isolated_functions(op.test, verification)
            op.body = self.__check_if_func_call_is_applicable_and_replace(
                op.body, verification
            )
            self.__replace_calls_to_isolated_functions(op.body, verification)
            op.orelse = self.__check_if_func_call_is_applicable_and_replace(
                op.orelse, verification
            )
            self.__replace_calls_to_isolated_functions(op.orelse, verification)
        elif isinstance(op, ast.If):
            op.test = self.__check_if_func_call_is_applicable_and_replace(
                op.test, verification
            )
            self.__replace_calls_to_isolated_functions(op.test, verification)
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__replace_calls_to_isolated_functions(op.body, verification)
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__replace_calls_to_isolated_functions(op.orelse, verification)
        elif isinstance(op, ast.Dict):
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__replace_calls_to_isolated_functions(op.values, verification)
        elif isinstance(op, ast.Set):
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__replace_calls_to_isolated_functions(op.elts, verification)
        elif isinstance(op, ast.comprehension):
            op.iter = self.__check_if_func_call_is_applicable_and_replace(
                op.iter, verification
            )
            self.__replace_calls_to_isolated_functions(op.iter, verification)
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__replace_calls_to_isolated_functions(op.ifs, verification)
        elif isinstance(op, (ast.ListComp, ast.SetComp, ast.GeneratorExp)):
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__replace_calls_to_isolated_functions(op.generators, verification)
        elif isinstance(op, ast.DictComp):
            op.value = self.__check_if_func_call_is_applicable_and_replace(
                op.value, verification
            )
            self.__replace_calls_to_isolated_functions(op.value, verification)
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__replace_calls_to_isolated_functions(op.generators, verification)
        elif isinstance(op, (ast.Yield, ast.YieldFrom)):
            if op.value:
                op.value = self.__check_if_func_call_is_applicable_and_replace(
                    op.value, verification
                )
                self.__replace_calls_to_isolated_functions(op.value, verification)
        elif isinstance(op, ast.FormattedValue):
            op.value = self.__check_if_func_call_is_applicable_and_replace(
                op.value, verification
            )
            self.__replace_calls_to_isolated_functions(op.value, verification)
        elif isinstance(op, ast.JoinedStr):
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__replace_calls_to_isolated_functions(op.values, verification)
        elif isinstance(op, ast.Return):
            if op.value:
                op.value = self.__check_if_func_call_is_applicable_and_replace(
                    op.value, verification
                )
                self.__replace_calls_to_isolated_functions(op.value, verification)
        elif isinstance(op, ast.Call):
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__replace_calls_to_isolated_functions(op.args, verification)
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__replace_calls_to_isolated_functions(op.keywords, verification)
            # Here we cannot have a Call to replace because it would have been done in the parent
            self.__replace_calls_to_isolated_functions(op.func, verification)
        elif isinstance(op, ast.keyword):
            op.value = self.__check_if_func_call_is_applicable_and_replace(
                op.value, verification
            )
            self.__replace_calls_to_isolated_functions(op.value, verification)
        elif isinstance(op, ast.FunctionDef):
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__replace_calls_to_isolated_functions(op.body, verification)

    def manipulate_mains(
        self, verification: bool, app_priorities: Dict[str, int]
    ) -> Tuple[List[str], List[str], List[IsolatedFunction]]:
        """
        Manipulates the `main.py` of all the apps, modifying the names of the functions and instances (specified in `accepted_names`),
        and adding the state argument to the calls. Then, the `invariant` and `iteration` functions are extracted, together with their imports,
        and dumped in the verification file.
        It also produces a function called `system_behaviour` that represents the whole system and is thus a combination of all the iteration functions.
        To do so, it needs the app_priorities to construct the function correctly to respect them
        """
        imports: List[str] = []
        functions: List[str] = []
        app_names_to_iteration_function_without_ret: Dict[str, ast.FunctionDef] = {}
        app_names_to_isolated_funcs: Dict[str, List[IsolatedFunction]] = {}
        for (
            directory,
            app_name,
        ), accepted_names in sorted(self.__instances_names_per_app.items()):
            (
                imps,
                funcs,
                iteration_func_without_ret,
                isolated_functions,
            ) = self.__manipulate_app_main(
                directory, app_name, accepted_names, verification
            )
            # Some imports might be a single string containing multiple imports separated by '\n'
            imps = (i for imp in imps for i in imp.split("\n"))
            imports.extend(imps)
            functions.append(funcs)
            app_names_to_iteration_function_without_ret[
                app_name
            ] = iteration_func_without_ret
            app_names_to_isolated_funcs[app_name] = isolated_functions

        # Generate the system behaviour function
        system_behaviour_func = self.__generate_system_behaviour_function(
            app_names_to_iteration_funcs_without_ret=app_names_to_iteration_function_without_ret,
            app_priorities=app_priorities,
            verification=verification,
        )

        system_behaviour_func_str = astor.to_source(system_behaviour_func)
        functions.append(system_behaviour_func_str)

        isolated_functions = [
            fn for fn_list in app_names_to_isolated_funcs.values() for fn in fn_list
        ]

        # Keep only non-empty imports
        new_imports = [imp.replace("\n", "") for imp in imports if imp]
        return new_imports, functions, isolated_functions

    def __generate_system_behaviour_function(
        self,
        app_names_to_iteration_funcs_without_ret: Dict[str, ast.FunctionDef],
        app_priorities: Dict[str, int],
        verification: bool,
    ) -> ast.FunctionDef:
        """
        Create the system behaviour function that contains all iterations functions back to back and that represents the behaviour of the entire system.
        It takes as arguments:
            - a Dict that maps all application names to their iteration functions, manipulated but without any return statement added
            - a Dict that maps all application names to their priority: the higher the number, the higher the priority
            - a bool indicating whether it must generate a verification version of the functions

        Applications with higher priority level can override behaviour of lower priority apps. Apps with same level are ordered by alphabetical order (arbitrary so no gurantee)

        If the verification bool flag is set, a return statement is added to return all states.
        """
        app_names = app_priorities.keys()
        priority_low_to_high_sorted_app_names: list[str] = []
        sorted_low_to_high_distinct_priorities = sorted(
            list(set(app_priorities.values()))
        )
        for i in sorted_low_to_high_distinct_priorities:
            sorted_low_to_high_app_names = sorted(
                filter(lambda n: app_priorities[n] == i, app_names)
            )
            priority_low_to_high_sorted_app_names.extend(sorted_low_to_high_app_names)

        body = []
        for app_name in priority_low_to_high_sorted_app_names:
            iteration_func = app_names_to_iteration_funcs_without_ret[app_name]
            body.extend(iteration_func.body)

        args = ast.arguments(args=[], defaults=[])
        behaviour_func = ast.FunctionDef(
            name=self.__SYSTEM_BEHAVIOUR_FUNC_NAME,
            body=body,
            decorator_list=[],
            args=args,
        )
        behaviour_func = self.__add_states_arguments_to_f(behaviour_func, "")
        if verification:
            behaviour_func = self.__add_return_states(behaviour_func)
        return behaviour_func

    def __check_no_invalid_calls_in_function(
        self, functions: List[ast.FunctionDef], invalid_func_names: Set[str]
    ) -> Tuple[bool, str]:
        """
        Checks that there are no calls to the given functions in the body of the given function definitions.
        returns a tuple with a boolean indicating whether the list of function definitions is valid (i.e., does not contain any
        invalid calls) and a str which is the first function defintion that contains an invalid call if the boolean is false

        an invalid function in the set can be a function name like `func` that gives call of the form `func()` or `m.func()` for any m
        or an instance/module name and a function name like `mod.func2` for calls of the form `mod.func(2)`
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
            if isinstance(op, (list, tuple)):
                for o in (check(v) for v in list(op)):
                    if not o:
                        return False
                return True
            elif isinstance(op, (ast.List, ast.Tuple)):
                return check(op.elts)
            elif isinstance(op, ast.BoolOp):
                return check(op.values)
            elif isinstance(op, ast.UnaryOp):
                return check(op.operand)
            elif isinstance(op, (ast.NamedExpr, ast.Expr)):
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
            elif isinstance(op, (ast.IfExp, ast.If)):
                return check(op.test) and check(op.body) and check(op.orelse)
            elif isinstance(op, ast.Dict):
                return check(op.values)
            elif isinstance(op, ast.Set):
                return check(op.elts)
            elif isinstance(op, ast.comprehension):
                return check(op.iter) and check(op.ifs)
            elif isinstance(op, (ast.ListComp, ast.SetComp, ast.GeneratorExp)):
                return check(op.generators)
            elif isinstance(op, ast.DictComp):
                return check(op.value) and check(op.generators)
            elif isinstance(op, (ast.Yield, ast.YieldFrom)):
                return check(op.value) if op.value else True
            elif isinstance(op, ast.FormattedValue):
                return check(op.value)
            elif isinstance(op, ast.JoinedStr):
                return check(op.values)
            elif isinstance(op, ast.Return):
                return check(op.value) if op.value else True
            elif isinstance(op, ast.Call):
                module_name = "123"  # impossible name as placeholder
                f_name = ""
                if isinstance(op.func, ast.Attribute):
                    # op.func.value is a ast.Name in this case
                    module_name = cast(ast.Name, op.func.value).id
                    f_name = op.func.attr
                elif isinstance(op.func, ast.Name):
                    f_name = op.func.id

                if (
                    f_name in invalid_func_names
                    or f"{module_name}.{f_name}" in invalid_func_names
                ):
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

    def __add_app_name_to_function_call_arguments(
        self, f: ast.Call, app_name: str
    ) -> ast.Call:
        """
        In place, adds to the given function the give app_name as the first argument after `self`.
        return the Call for convenience
        """
        new_arg = ast.Constant(value=app_name)
        current_args = f.args
        if len(current_args) > 0:
            current_args.insert(0, new_arg)
        else:
            current_args = [new_arg]
        f.args = current_args
        return f

    def __check_if_function_filesystem_and_add_app_name(
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
        app_name: str,
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
        Checks if the given op is a Function call and if that function is a filesystem SvshiApi call.
        If that's the case, it adds, in place, the app_name as argument.
        """
        if isinstance(op, ast.Call):
            if isinstance(op.func, ast.Attribute):
                # op.func.value is a ast.Name in this case
                module_name = cast(ast.Name, op.func.value).id
                f_name = cast(ast.Name, op.func.attr)
                if (
                    module_name == self.__SVSHI_API_INSTANCE_NAME
                    and f_name in self.__SVSHI_API_FILESYSTEM_FUNCTION_NAMES
                ):
                    self.__add_app_name_to_function_call_arguments(
                        cast(ast.Call, op), app_name=app_name
                    )
        return op

    def __check_mode_arg_if_function_call(
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
    ) -> None:
        """
        Checks if the given op is a Function call and if that function is a filesystem SvshiApi get file call.
        WARNING: it must be called BEFORE adding the app_name argument!! It assumes the mode arg is the 2nd one.
        If that's the case, it checks that the mode arg is valid i.e., in 'w', 'r', 'a', 'wr', 'ar', 'ra', 'rw'.

        Raises InvalidFileOpenModeException() if the mode is invalid or not there

        """

        if isinstance(op, ast.Call):
            if isinstance(op.func, ast.Attribute):
                # op.func.value is a ast.Name in this case
                module_name = cast(ast.Name, op.func.value).id
                f_name = cast(ast.Name, op.func.attr)
                if module_name == self.__SVSHI_API_INSTANCE_NAME and f_name in [
                    self.__SVSHI_API_GET_FILE_BINARY_API_FUNC_NAME,
                    self.__SVSHI_API_GET_FILE_TEXT_API_FUNC_NAME,
                ]:
                    current_args = op.args
                    if len(current_args) > 1:
                        mode_arg = current_args[1]  # mode is the second argument
                        if not isinstance(mode_arg, ast.Constant):
                            _print_and_raise(
                                "The mode must be a constant str!",
                                InvalidFileOpenModeException,
                            )
                        if (
                            mode_arg.value
                            not in self.__SVSHI_API_FILESYSTEM_OPEN_FILE_MODES
                        ):
                            _print_and_raise(
                                f"The mode passed to a get file function must be in "
                                f"{self.__SVSHI_API_FILESYSTEM_OPEN_FILE_MODES}. "
                                "Current mode is '{mode_arg.value}'",
                                InvalidFileOpenModeException,
                            )
                    else:
                        # This is a problem, return False
                        _print_and_raise(
                            "Too few argument passed to svshi_api get file function!",
                            InvalidFileOpenModeException,
                        )

    def __add_appname_to_filesystem_functions_args_and_check_mode_arg(
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
        app_name: str,
    ):
        """
        Manipulates the op to add the app_name to all call to filesystem related functions as arguments. The addition is
        done in place, it modifies the object.
        I also checks that the argument passed for mode (e.g., "wr", "r", "a") is valid i.e. a combination of 'w', 'r', 'a' without 'a' and 'w' together
        so available mode are 'w', 'r', 'a', 'wr', 'ar', 'ra', 'rw'
        An empty mode corresponds to 'r'
        """
        if isinstance(op, list):
            for index, v in enumerate(op):
                self.__check_mode_arg_if_function_call(v)
                temp_new_ast = self.__check_if_function_filesystem_and_add_app_name(
                    v, app_name
                )
                op[index] = temp_new_ast
                self.__add_appname_to_filesystem_functions_args_and_check_mode_arg(
                    v, app_name
                )
        elif isinstance(op, (ast.List, ast.Tuple)):
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__add_appname_to_filesystem_functions_args_and_check_mode_arg(
                op.elts, app_name
            )
        elif isinstance(op, ast.BoolOp):
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__add_appname_to_filesystem_functions_args_and_check_mode_arg(
                op.values, app_name
            )
        elif isinstance(op, ast.UnaryOp):
            self.__check_mode_arg_if_function_call(op.operand)
            op.operand = self.__check_if_function_filesystem_and_add_app_name(
                op.operand, app_name
            )
            self.__add_appname_to_filesystem_functions_args_and_check_mode_arg(
                op.operand, app_name
            )
        elif isinstance(op, ast.NamedExpr):
            self.__check_mode_arg_if_function_call(op.target)
            op.target = self.__check_if_function_filesystem_and_add_app_name(
                op.target, app_name
            )
            self.__add_appname_to_filesystem_functions_args_and_check_mode_arg(
                op.target, app_name
            )
            self.__check_mode_arg_if_function_call(op.value)
            op.value = self.__check_if_function_filesystem_and_add_app_name(
                op.value, app_name
            )
            self.__add_appname_to_filesystem_functions_args_and_check_mode_arg(
                op.value, app_name
            )
        elif isinstance(op, ast.Expr):
            self.__check_mode_arg_if_function_call(op.value)
            op.value = self.__check_if_function_filesystem_and_add_app_name(
                op.value, app_name
            )
            self.__add_appname_to_filesystem_functions_args_and_check_mode_arg(
                op.value, app_name
            )
        elif isinstance(op, ast.Lambda):
            self.__check_mode_arg_if_function_call(op.body)
            op.body = self.__check_if_function_filesystem_and_add_app_name(
                op.body, app_name
            )
            self.__add_appname_to_filesystem_functions_args_and_check_mode_arg(
                op.body, app_name
            )
        elif isinstance(op, ast.Assign):
            self.__check_mode_arg_if_function_call(op.targets)
            op.targets = self.__check_if_function_filesystem_and_add_app_name(
                op.targets, app_name
            )
            self.__add_appname_to_filesystem_functions_args_and_check_mode_arg(
                op.targets, app_name
            )
            self.__check_mode_arg_if_function_call(op.value)
            op.value = self.__check_if_function_filesystem_and_add_app_name(
                op.value, app_name
            )
            self.__add_appname_to_filesystem_functions_args_and_check_mode_arg(
                op.value, app_name
            )
        elif isinstance(op, ast.AugAssign):
            self.__check_mode_arg_if_function_call(op.target)
            op.target = self.__check_if_function_filesystem_and_add_app_name(
                op.target, app_name
            )
            self.__add_appname_to_filesystem_functions_args_and_check_mode_arg(
                op.target, app_name
            )
            self.__check_mode_arg_if_function_call(op.value)
            op.value = self.__check_if_function_filesystem_and_add_app_name(
                op.value, app_name
            )
            self.__add_appname_to_filesystem_functions_args_and_check_mode_arg(
                op.value, app_name
            )
        elif isinstance(op, ast.AnnAssign):
            self.__check_mode_arg_if_function_call(op.target)
            op.target = self.__check_if_function_filesystem_and_add_app_name(
                op.target, app_name
            )
            self.__add_appname_to_filesystem_functions_args_and_check_mode_arg(
                op.target, app_name
            )
            self.__check_mode_arg_if_function_call(op.annotation)
            op.annotation = self.__check_if_function_filesystem_and_add_app_name(
                op.annotation, app_name
            )
            self.__add_appname_to_filesystem_functions_args_and_check_mode_arg(
                op.annotation, app_name
            )
            self.__check_mode_arg_if_function_call(op.value)
            op.value = self.__check_if_function_filesystem_and_add_app_name(
                op.value, app_name
            )
            self.__add_appname_to_filesystem_functions_args_and_check_mode_arg(
                op.value, app_name
            )
        elif isinstance(op, ast.Return):
            if op.value:
                self.__check_mode_arg_if_function_call(op.value)
                op.value = (
                    temp_new_ast
                ) = self.__check_if_function_filesystem_and_add_app_name(
                    op.value, app_name
                )
                self.__add_appname_to_filesystem_functions_args_and_check_mode_arg(
                    op.value, app_name
                )
        elif isinstance(op, ast.Compare):
            self.__check_mode_arg_if_function_call(op.left)
            op.left = self.__check_if_function_filesystem_and_add_app_name(
                op.left, app_name
            )
            self.__add_appname_to_filesystem_functions_args_and_check_mode_arg(
                op.left, app_name
            )
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__add_appname_to_filesystem_functions_args_and_check_mode_arg(
                op.comparators, app_name
            )
        elif isinstance(op, ast.BinOp):
            self.__check_mode_arg_if_function_call(op.left)
            op.left = self.__check_if_function_filesystem_and_add_app_name(
                op.left, app_name
            )
            self.__add_appname_to_filesystem_functions_args_and_check_mode_arg(
                op.left, app_name
            )
            self.__check_mode_arg_if_function_call(op.right)
            op.right = self.__check_if_function_filesystem_and_add_app_name(
                op.right, app_name
            )
            self.__add_appname_to_filesystem_functions_args_and_check_mode_arg(
                op.right, app_name
            )
        elif isinstance(op, ast.IfExp):
            self.__check_mode_arg_if_function_call(op.test)
            op.test = self.__check_if_function_filesystem_and_add_app_name(
                op.test, app_name
            )
            self.__add_appname_to_filesystem_functions_args_and_check_mode_arg(
                op.test, app_name
            )
            self.__check_mode_arg_if_function_call(op.body)
            op.body = self.__check_if_function_filesystem_and_add_app_name(
                op.body, app_name
            )
            self.__add_appname_to_filesystem_functions_args_and_check_mode_arg(
                op.body, app_name
            )
            self.__check_mode_arg_if_function_call(op.orelse)
            op.orelse = self.__check_if_function_filesystem_and_add_app_name(
                op.orelse, app_name
            )
            self.__add_appname_to_filesystem_functions_args_and_check_mode_arg(
                op.orelse, app_name
            )
        elif isinstance(op, ast.If):
            self.__check_mode_arg_if_function_call(op.test)
            op.test = self.__check_if_function_filesystem_and_add_app_name(
                op.test, app_name
            )
            self.__add_appname_to_filesystem_functions_args_and_check_mode_arg(
                op.test, app_name
            )
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__add_appname_to_filesystem_functions_args_and_check_mode_arg(
                op.body, app_name
            )
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__add_appname_to_filesystem_functions_args_and_check_mode_arg(
                op.orelse, app_name
            )
        elif isinstance(op, ast.Dict):
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__add_appname_to_filesystem_functions_args_and_check_mode_arg(
                op.values, app_name
            )
        elif isinstance(op, ast.Set):
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__add_appname_to_filesystem_functions_args_and_check_mode_arg(
                op.elts, app_name
            )
        elif isinstance(op, ast.comprehension):
            self.__check_mode_arg_if_function_call(op.iter)
            op.iter = self.__check_if_function_filesystem_and_add_app_name(
                op.iter, app_name
            )
            self.__add_appname_to_filesystem_functions_args_and_check_mode_arg(
                op.iter, app_name
            )
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__add_appname_to_filesystem_functions_args_and_check_mode_arg(
                op.ifs, app_name
            )
        elif isinstance(op, (ast.ListComp, ast.SetComp, ast.GeneratorExp)):
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__add_appname_to_filesystem_functions_args_and_check_mode_arg(
                op.generators, app_name
            )
        elif isinstance(op, ast.DictComp):
            self.__check_mode_arg_if_function_call(op.value)
            op.value = self.__check_if_function_filesystem_and_add_app_name(
                op.value, app_name
            )
            self.__add_appname_to_filesystem_functions_args_and_check_mode_arg(
                op.value, app_name
            )
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__add_appname_to_filesystem_functions_args_and_check_mode_arg(
                op.generators, app_name
            )
        elif isinstance(op, (ast.Yield, ast.YieldFrom)):
            if op.value:
                self.__check_mode_arg_if_function_call(op.value)
                op.value = self.__check_if_function_filesystem_and_add_app_name(
                    op.value, app_name
                )
                self.__add_appname_to_filesystem_functions_args_and_check_mode_arg(
                    op.value, app_name
                )
        elif isinstance(op, ast.FormattedValue):
            self.__check_mode_arg_if_function_call(op.value)
            op.value = self.__check_if_function_filesystem_and_add_app_name(
                op.value, app_name
            )
            self.__add_appname_to_filesystem_functions_args_and_check_mode_arg(
                op.value, app_name
            )
        elif isinstance(op, ast.JoinedStr):
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__add_appname_to_filesystem_functions_args_and_check_mode_arg(
                op.values, app_name
            )
        elif isinstance(op, ast.Return):
            if op.value:
                self.__check_mode_arg_if_function_call(op.value)
                op.value = self.__check_if_function_filesystem_and_add_app_name(
                    op.value, app_name
                )
                self.__add_appname_to_filesystem_functions_args_and_check_mode_arg(
                    op.value, app_name
                )
        elif isinstance(op, ast.Call):
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__add_appname_to_filesystem_functions_args_and_check_mode_arg(
                op.args, app_name
            )
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__add_appname_to_filesystem_functions_args_and_check_mode_arg(
                op.keywords, app_name
            )
            # Here we cannot have a Call to replace because it would have been done in the parent
            self.__add_appname_to_filesystem_functions_args_and_check_mode_arg(
                op.func, app_name
            )
        elif isinstance(op, ast.keyword):
            self.__check_mode_arg_if_function_call(op.value)
            op.value = self.__check_if_function_filesystem_and_add_app_name(
                op.value, app_name
            )
            self.__add_appname_to_filesystem_functions_args_and_check_mode_arg(
                op.value, app_name
            )
        elif isinstance(op, ast.FunctionDef):
            # Here we do not do the check as it cannot be a function call (due to the type)
            self.__add_appname_to_filesystem_functions_args_and_check_mode_arg(
                op.body, app_name
            )

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
        if isinstance(op, (list, tuple)):
            for v in list(op):
                self.__rename_files(v, app_name, filenames)
        elif isinstance(op, (ast.List, ast.Tuple)):
            self.__rename_files(op.elts, app_name, filenames)
        elif isinstance(op, ast.BoolOp):
            self.__rename_files(op.values, app_name, filenames)
        elif isinstance(op, ast.UnaryOp):
            self.__rename_files(op.operand, app_name, filenames)
        elif isinstance(op, (ast.NamedExpr, ast.Expr)):
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
        elif isinstance(op, (ast.IfExp, ast.If)):
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
        elif isinstance(op, (ast.ListComp, ast.SetComp, ast.GeneratorExp)):
            self.__rename_files(op.generators, app_name, filenames)
        elif isinstance(op, ast.DictComp):
            self.__rename_files(op.value, app_name, filenames)
            self.__rename_files(op.generators, app_name, filenames)
        elif isinstance(op, (ast.Yield, ast.YieldFrom)):
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

    def __check_presence_forbidden_module(
        self, imports_ast: List[ast.stmt], forbidden_modules: List[str]
    ) -> Tuple[bool, str]:
        """
        Checks in the given list of imports if one of the given forbidden module is imported.
        Return a tuple whose first element is True if a forbidden module is found, False otherwise and second is the forbidden module found or "" if 1st is False
        """
        imported_modules = []
        for imp in imports_ast:
            if isinstance(imp, ast.Import):
                for n in imp.names:
                    imported_modules.append(n.name)
            elif isinstance(imp, ast.ImportFrom):
                imported_modules.append(imp.module)

        forbidden_modules_imported = cast(
            List[str], list(filter(lambda m: m in forbidden_modules, imported_modules))
        )
        if len(forbidden_modules_imported) > 0:
            return (True, forbidden_modules_imported[0])
        else:
            return (False, "")

    def __manipulate_app_main(
        self,
        directory: str,
        app_name: str,
        accepted_names: Set[str],
        verification: bool,
    ) -> Tuple[List[str], str, ast.FunctionDef, List[IsolatedFunction]]:
        """
        Returns the list of imports as strings and the manipulated invariant, function as pretty printed code (as string), the iteration function ast without the return
        statement but with the other manipulation, and the list of IsolatedFunction of the app.
        We keep imports added by the user.
        The manipulation consists in modifying the functions to add the app name, renaming all used files, renaming calls to devices and adding arguments to
        calls to their methods.
        If it is for the verification file (i.e. verification is True), the imports are dumped.
        """

        def extract_functions_and_imports(
            module_body: List[ast.stmt],
        ):
            # We only keep invariant and iteration functions, and we add to them the docstring with the contracts
            functions_ast = list(
                map(
                    lambda f: self.__add_doc_string(
                        f,
                        self.__construct_contracts(
                            self.__app_names,
                            verification,
                        ),
                    )
                    if f.name == f"{app_name}_{self.__ITERATION_FUNC_NAME}"
                    and verification
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
                                        tuple(
                                            f"{app_name}_{prefix}"
                                            for prefix in self.__ISOLATED_FUNC_PREFIXES
                                        )
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

            isolated_func_dict = self.__get_isolated_functions(module_body, app_name)
            isolated_funcs = list(isolated_func_dict.values())

            # # We rename all the files, if any
            # filenames = self.__filenames_per_app[app_name]
            # if filenames:
            #     self.__rename_files(module_body, app_name, filenames)

            # We rename all the device instances and add the state argument to each of their calls
            self.__rename_instances_add_state(
                module_body, app_name, accepted_names, isolated_funcs, verification
            )

            # Extract imports, invariant/iteration functions and add the contracts to them if verification is false
            (
                functions_ast,
                imports_ast,
                from_imports_ast,
            ) = extract_functions_and_imports(module_body)

            all_imports_ast = imports_ast + from_imports_ast

            # Contains all the invalid function names: these are not allowed in invariant or iteration functions
            invalid_func_names = {
                self.__PRINT_FUNC_NAME,
                self.__OPEN_FUNC_NAME,
                f"{self.__SVSHI_API_INSTANCE_NAME}.{self.__SVSHI_API_GET_FILEPATH_API_FUNC_NAME}",
                f"{self.__SVSHI_API_INSTANCE_NAME}.{self.__SVSHI_API_GET_FILE_TEXT_API_FUNC_NAME}",
                f"{self.__SVSHI_API_INSTANCE_NAME}.{self.__SVSHI_API_GET_FILE_BINARY_API_FUNC_NAME}",
            }

            # Check if an invariant function contains a call to an invalid function,
            # i.e. an isolated function or completely invalid ones
            isolated_func_names = {
                uf.name_with_app_name for _, uf in isolated_func_dict.items()
            }
            invariant_invalid_func_names = isolated_func_names | invalid_func_names
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
                _print_and_raise(
                    f"The invariant function '{wrong_invariant_func}' contains a call "
                    f"to a forbidden function in that list: {invariant_invalid_func_names}.",
                    InvalidFunctionCallException,
                )

            iteration_functions = list(
                filter(
                    lambda f: f.name.endswith(f"_{self.__ITERATION_FUNC_NAME}"),
                    functions_ast,
                )
            )

            # Check if an iteration function contains a call to an invalid function
            valid, wrong_iteration_func = self.__check_no_invalid_calls_in_function(
                iteration_functions,
                invalid_func_names,
            )
            if not valid:
                _print_and_raise(
                    f"The iteration function '{wrong_iteration_func}' contains a call "
                    f"to a forbidden function in that list: {invalid_func_names}.",
                    InvalidFunctionCallException,
                )

            # Replace all calls to isolated functions via svshi_api in place.
            self.__replace_calls_to_isolated_functions(
                iteration_functions,
                verification,
            )

            if not verification:
                # Check if an isolated function contains a call to an invalid function
                isolated_func_definitions = list(
                    filter(
                        lambda f: f.name.startswith(
                            tuple(
                                f"{app_name}_{prefix}"
                                for prefix in self.__ISOLATED_FUNC_PREFIXES
                            )
                        ),
                        functions_ast,
                    )
                )
                isolated_invalid_func_names = set([self.__OPEN_FUNC_NAME])
                valid, wrong_isolated_func = self.__check_no_invalid_calls_in_function(
                    isolated_func_definitions,
                    isolated_invalid_func_names,
                )
                if not valid:
                    _print_and_raise(
                        f"The function '{wrong_isolated_func}' contains a call to a "
                        f"forbidden function in that list: {isolated_invalid_func_names}.",
                        InvalidFunctionCallException,
                    )

                # Add the internal_state as argument to all isolated functions
                for isolated_def in isolated_func_definitions:
                    self.__add_internal_state_to_fun_def(isolated_def)

            # Filesystem related operations

            # We add the app_name as arguments to file system calls. They appear only in isolated functions as checked above, which
            # are in the list only when verification = false
            self.__add_appname_to_filesystem_functions_args_and_check_mode_arg(
                functions_ast, app_name
            )

            # Get the iteration function as an AST without return statement to generate the system behaviour function later
            # There is only one per app
            iteration_ast = copy.deepcopy(
                list(
                    filter(
                        lambda f: f.name == f"{app_name}_{self.__ITERATION_FUNC_NAME}",
                        functions_ast,
                    )
                )[0]
            )

            # We need to remove the docstring from the iteration_ast
            iteration_ast = self.__remove_doc_string(iteration_ast)

            if verification:
                # Add a return statement that returns the states for verification
                functions_ast = list(
                    map(
                        lambda f: self.__add_return_states(f)
                        if f.name == f"{app_name}_{self.__ITERATION_FUNC_NAME}"
                        else f,
                        functions_ast,
                    )
                )

            # Check for forbidden imported modules
            (
                forbidden_import_flag,
                forbidden_module_imported,
            ) = self.__check_presence_forbidden_module(
                imports_ast=all_imports_ast,
                forbidden_modules=self.__FORBIDDEN_MODULE_IN_APPS,
            )
            if forbidden_import_flag:
                _print_and_raise(
                    f"The app '{app_name}' imports the following module which is "
                    f"forbidden in applications: '{forbidden_module_imported}'",
                    ForbiddenModuleImported,
                )

            # Transform to source code
            functions = astor.to_source(ast.Module(functions_ast))
            imports = astor.to_source(ast.Module(imports_ast))
            from_imports = astor.to_source(ast.Module(from_imports_ast))
            return [imports, from_imports], functions, iteration_ast, isolated_funcs
