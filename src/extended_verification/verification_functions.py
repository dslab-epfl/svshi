import dataclasses
import os
import textwrap
from typing import List, Dict, Tuple, Union, Final
import collections
import functools
import importlib
import inspect
import re
from typing import Callable
import z3
from crosshair.path_cover import path_cover, CoverageType
from z3 import ArgumentError, is_not, Not, Int, And, ForAll, Exists, Implies, is_quantifier, Solver, simplify, \
    Or, If, z3util, ExprRef, is_bool, is_const, unsat
from crosshair.options import (DEFAULT_OPTIONS, AnalysisOptionSet)

from .check_objs import *
from crosshair import FunctionInfo

APP_STATE_VARS_REGEX = r"INT_[0-3]\b|FLOAT_[0-3]\b|BOOL_[0-3]\b"

APP_STATE_VARS_REGEX = r"INT_[0-3]\b|FLOAT_[0-3]\b|BOOL_[0-3]\b"

SVSHI_API_OBJ_NAME = "svshi_api"
API_CHECK_NAME = f"{SVSHI_API_OBJ_NAME}.check_time_property"
DUMMY_CHECK = "dummy_check"
SVSHI_HOME: Final = os.environ["SVSHI_HOME"]
DEBUG = False
FUNCTION_VERIFICATION_FILE_NO_EXT = "functions_to_verify"
FUNCTION_VERIFICATION_FILE = FUNCTION_VERIFICATION_FILE_NO_EXT + ".py"

@dataclasses.dataclass
class CheckContainer:
    function: str
    list_of_replaced_checks: List[str]

class VerificationFunctions:

    def __init__(self, per_path_timeout=30.0, per_condition_timeout=25):
        self.PER_PATH_TIMEOUT = per_path_timeout
        self.PER_CONDITION_TIMEOUT = per_condition_timeout
        
    def debug(self, *s: str):
        if DEBUG:
            print(s)


    def expression_list_to_conjunction(self, l: List[ExprRef]) -> ExprRef:
        """
        Transforms a list of z3 expressions into one single And(). If there's only one element, it returns it.
        :param l: a list of z3 expressions
        :return: A conjunction of the list or the element if the list is a singleton
        """
        if len(l) > 1:
            return And(l)
        elif len(l) == 0:
            raise ValueError("empty list, impossible to create a conjunction")
        else:
            return l[0]


    def print_warning_if_not_exhausted(self, exhausted: bool):
        if not exhausted:
            print("WARNING: paths not exhausted, increase the condition and path timeout")


    def get_paths(self, f: FunctionInfo) -> Tuple[List[Tuple[ExprRef, object]], bool]:
        """
        Convert a function f into a list of paths (z3 expressions) using CrossHair and their symbolic values at return.
        :param f: a callable function
        :return: A tuple. The first element is the list of paths (paths being a list of z3 expressions), their symbolic values at return and whether crosshair exhausted all the paths.
        """
        if f is None:
            raise ValueError("no function to cover!")
        # code below from CrossHair
        defaults = DEFAULT_OPTIONS.overlay(
            AnalysisOptionSet(
                per_condition_timeout=self.PER_CONDITION_TIMEOUT,
                per_path_timeout=self.PER_PATH_TIMEOUT,  # mostly, we don't want to time out paths
            )
        )
        defaults.stats = collections.Counter()
        pc, exhausted = path_cover(f, defaults, CoverageType.OPCODE)
        try:
            print(pc)  # force the paths to realize?
        except AssertionError:
            raise ValueError(f"Failed to run crosshair path, check your function {f.name}")
        return pc, exhausted


    def print_paths_in_PathSummary_list(self, pc):
        for p in pc:
            for i in p.result:
                print(i)


    def import_from(self, module_str: str, name):
        module = importlib.import_module(module_str)
        os.remove(getattr(module, '__cached__', module_str + '.pyc'))  # remove the cached compiled file
        importlib.reload(module)
        return getattr(module, name)


    def string_to_callable(self, function: str, name: str) -> Callable:
        """
        Writes a function from a string into a file. Then it is imported and returns the function as a Callable() object
        :param function: The function as a string
        :param name: The function name
        :return: The function as a callable object
        """
        with open(SVSHI_HOME + "/src/extended_verification/temp_check.py", "w") as funcfile:
            funcfile.write(function)
        func_as_call = self.import_from("extended_verification.temp_check", name)
        return func_as_call


    def replace_removed_checks(self, valid_paths, replace_list, cdt_dict, app_name) -> List[Tuple]:
        """
        From paths with the dummy variable c0 == v, it replaces this condition by the actual check call. Does the modification in place.
        Modifies the valid_path with the check call being replaced by a "ForAll [...]"
        :param valid_paths: The list of paths used by CrossHair
        :param replace_list: The list of check call to be replaced
        :param cdt_dict: the dictionary of functions per attribute
        :return: translated_checks a list of pairs of (z3_check_str,api_function_call)
        """
        check_len = len(f"{API_CHECK_NAME}(")
        translated_checks: List[Tuple] = []
        for i, func_string in enumerate(replace_list):
            func_string_args = func_string[check_len:-1].split(",")
            freq_str = func_string_args[0].strip().removeprefix("frequency=").removeprefix("svshi_api.")
            duration_str = func_string_args[1].strip().removeprefix("duration=").removeprefix("svshi_api.")
            d = {s.__name__:s for s in DateObj.__subclasses__()}
            frequency = eval(freq_str, d)
            duration = eval(duration_str, d)
            condition_str = ",".join(func_string_args[2:]).strip().removeprefix("condition=")
            condition_func = textwrap.dedent(
            f"""
        from .{FUNCTION_VERIFICATION_FILE_NO_EXT} import *
        def conds{i}(physical_state: PhysicalState, internal_state: InternalState):
            return ({condition_str})
            """)

            condition = self.string_to_callable(condition_func, f"conds{i}")
            try:
                z3_condition, exhausted = self.get_paths(FunctionInfo.from_fn(condition))
            except ValueError:
                raise ValueError("Error on check condition function, make sure your conditions doesn't have any aliases")
            self.print_warning_if_not_exhausted(exhausted)
            constraints_list = self.extract_valid_paths_from_invariant(z3_condition)
            search_str = str(i) + " == c0"  # c0 is the flag used internally to model the check function to CrossHair
            for p in valid_paths:
                for c in range(len(p)):
                    if search_str in str(p[c]):
                        if is_not(p[c]):
                            p[c] = z3.BoolVal(True)  # replace by True to ignore when check is false
                        else:
                            condition_of_check_list = [
                                self.split_z3_expr_list_to_constraint_and_inv(constraint, cdt_dict, app_name) for
                                constraint in constraints_list]
                            condition_of_check = functools.reduce(lambda x, y: Or(x, y), condition_of_check_list)
                            z3_check = self.check(frequency, duration, condition_of_check)
                            translated_checks.append((z3_check.__str__(),func_string))
                            p[c] = z3_check
        return translated_checks

    def extract_valid_paths_from_invariant(self, paths) -> List[z3.AstRef]:
        """
        Extracts the paths that returns True or the symbolic value of the function (i.e. return a )
        :param paths: A List of PathSummary
        :return: A list of path conditions (z3 ExprRef) which are represented as an AstRef (a list of z3 ExprRef)
        """
        valid_paths_inv = []
        # get valid paths (paths that only returns True) from the invariant fct
        for p in paths:
            return_val = p.result[1]['ret']
            path_cdt = p.result[0]
            if return_val is None:
                self.debug(f"WARNING: path leads to None, ignoring path {path_cdt}")
            elif isinstance(return_val, bool):
                if return_val:
                    if len(path_cdt) > 0:
                        valid_paths_inv.append(path_cdt)
                    else:
                        valid_paths_inv.append([z3.BoolVal(return_val)])
                else:
                    self.debug("ignoring path that returns false", return_val)
            elif z3.is_expr(return_val):
                path_cdt.push(return_val)  # path_cdt is an AstRef
                if z3.BoolVal(False) == simplify(And(path_cdt)):
                    continue
                valid_paths_inv.append(path_cdt)
            else:
                valid_paths_inv.append(path_cdt)

        return valid_paths_inv


    def extract_actual_check_call(self, func: str) -> str:
        """
        Function extracts full svshi_api_check call string, including all the arguments given
        """
        start_index = func.find(API_CHECK_NAME)
        end_index = start_index + len(API_CHECK_NAME) + 1
        open_parenheses_counter = 1
        while open_parenheses_counter > 0:
            if func[end_index] == "(":
                open_parenheses_counter += 1
            elif func[end_index] == ")":
                open_parenheses_counter -= 1
            end_index += 1
        return func[start_index:end_index]


    def check_extractor(self, call_num: int, check_container: CheckContainer) -> CheckContainer:
        """
        Recursively replace all instances of svshi_api_check calls to dummy_check calls. Store the original calls
        and the order the calls occurred at in a list.
        :param call_num: The index of the check, used to make a dummy constraint in crosshair to replace the check call
        :param check_container: Object that contains the function and the list of check to replace
        :return: The "sanitized" function and the list of original occurrences
        """
        if API_CHECK_NAME not in check_container.function:
            return check_container
        else:
            check_string = self.extract_actual_check_call(check_container.function)
            dummy_check_string = f"{SVSHI_API_OBJ_NAME}.{DUMMY_CHECK}(internal_state,{call_num})"
            check_container.list_of_replaced_checks.append(check_string)
            new_func = check_container.function.replace(check_string, dummy_check_string, 1)
            check_container.function = new_func
            return self.check_extractor(call_num + 1, check_container)


    def check_function_replace(self, function: str) -> CheckContainer:
        """
        Replace svshi_api_check calls with dummy check calls and generate a list of the original calls
        :param function: The function to "sanitize"
        :return: The "sanitized" function and a list of original svshi_api_check calls in their correct order
        """
        return self.check_extractor(0, CheckContainer(function, list()))


    def crosshair_variable_name_dict(self, crosshair_v: List[ExprRef]) -> Dict[str, ExprRef]:
        """
        Creates a dictionary from original variables names to crosshair variables names (i.e. varName_NUMBER to varName:varName_NUMBER)
        :param crosshair_v: A list of z3 ExpRef
        :return: a dict {original_name,crosshair/z3 name}
        """
        return {self.crosshair_z3_var_to_var_str(x): x for x in crosshair_v}


    def check(self, every: DateObj, duration: DateObj, cdt: ExprRef) -> List[ExprRef]:
        """
        Generate from two DateObj and a condition a z3 expression: For every time t1 there exits a duration d s.t. condition
        :param every: A DateObj that gives the frequency of the property to check
        :param duration: A DateObj that gives the duration of the property to check
        :param cdt: The z3 condition to be checked
        :return: The z3 expression of the given property
        """

        crosshair_variables = z3util.get_vars(cdt)
        values_dict = self.crosshair_variable_name_dict(
            crosshair_variables)  # get the CrossHair time variables that should be replaced
        t = Int('t')
        for v in DateObj.internal_state_variable_names:
            values_dict.setdefault(v, Int(v)) # adds the z3 values that were not used on the iteration/invariant
        duration_z3 = values_dict.get(duration.descr, Int(duration.descr))
        duration_cdt = And(And(t >= duration.min_v, t <= duration.max_v - duration.value), ForAll(duration_z3,
                                                                                                Implies(
                                                                                                    And(t <= duration_z3,
                                                                                                        duration_z3 <= t + duration.value),
                                                                                                    cdt)))
        every_descr = values_dict.get(every.descr, Int(every.descr))
        date_obj_constraints_list = []
        for date_obj in DateObj.__subclasses__():
            # generating min_time<=i<=maxtime for every date, converted to z3
            date_obj_constraints_list.append(date_obj.min_v <= values_dict.get(date_obj.descr, Int(date_obj.descr)))
            date_obj_constraints_list.append(values_dict.get(date_obj.descr, Int(date_obj.descr)) <= date_obj.max_v)
        dateobj_conjunction = self.expression_list_to_conjunction(date_obj_constraints_list)
        i = every.min_v
        every_conditions_lists = []
        if every.value > 1:
            while i <= every.max_v:  # generates the interval of the duration of "every"
                e = Exists([t, every_descr], And(dateobj_conjunction,
                                                i <= every_descr,
                                                every_descr <= min(i + every.value - 1, every.max_v),
                                                duration_cdt))

                every_conditions_lists.append(e)
                i += every.get_value()
        else:
            every_conditions_lists.append(Exists(t, And(duration_cdt, dateobj_conjunction)))
        t = functools.reduce(lambda x, y: And(x, y), every_conditions_lists)
        final_check = ForAll(list(values_dict.values()), Implies(dateobj_conjunction,
                                                                t))
        return final_check


    def run_crosshair_on_iteration_fct(self, fct: Callable, var_dict: Dict[str, z3.ExprRef] = None):
        """
        Run crosshair cover on the function fct, updates the dictionary of all symbolic variables var_dict
        :param fct: the iteration function
        :param var_dict: dictionary of all symbolic variables
        :return: cdt_dict, a dictionary of variable names as key and their function in z3 as a value
        """
        f = FunctionInfo.from_fn(fct)
        paths, exhausted = self.get_paths(f)

        self.print_warning_if_not_exhausted(exhausted)
        for p in paths:  # convert the paths constraints as a list to a conjunction,  modifying the paths list inplace
            path_constraint_list = p.result[0]
            if len(path_constraint_list) == 0:
                constraints = z3.BoolVal(True)  # any condition
                self.debug("WARNING: path with no condition, supposed it is always true")
            else:
                constraints = self.expression_list_to_conjunction(path_constraint_list)
            p.result = (constraints, p.result[1])

        tmp = {}
        for i, p in enumerate(paths):
            condition = p.result[0]
            for svshi_obj, content in p.result[1].items():  # get app_state, physical_state etc...
                for var_name, z3_expr in content.items():
                    if "app_state" in svshi_obj:
                        var_name = svshi_obj + var_name # adds the name to cover all the app_states
                    cdt_list = tmp.setdefault(var_name, [])
                    if len(cdt_list) > i:
                        raise ValueError(f"Duplicate key detected for key {var_name}")
                    cdt_list.append((condition, z3_expr))
                    self.add_z3_var_to_var_dict(var_dict, var_name, z3_expr)
        cdt_dict = {k: self.path_list_to_nested_ifs(v) for k, v in tmp.items()}
        return cdt_dict


    def add_z3_var_to_var_dict(self, var_dict: Dict[str, ExprRef], var_name: str, z3_expr: ExprRef):
        if re.match(APP_STATE_VARS_REGEX, var_name):
            raise ValueError(f"adding {var_name} as a key should never happen, could cause app_state conflicts")
        if var_dict is not None:
            if var_name not in var_dict and is_const(z3_expr):
                var_dict[var_name] = z3_expr
            elif var_name not in z3_expr.__str__():
                self.debug(f"ignoring formula {z3_expr} for var name {var_name}")
            elif self.crosshair_z3_var_to_var_str(var_dict[var_name]) != z3_expr.__str__():
                ValueError(
                    f"multiples z3GA for the same name! name:{var_name} value in iteration : {z3_expr} value in dict {var_dict[var_name]}")


    def path_list_to_nested_ifs(self, l: List[Tuple[ExprRef, Union[ExprRef, bool]]]) -> ExprRef:
        """
        Converts a list of paths and their return values into into a function made of z3 If : If(condition1,return1,(else if) condition2 ... )
        (i.e. [(And(t>0,c>0),a),(c>0,Not(a))]) into z3 If => If((And(t>0,c>0),a,If(c>0,Not(a),-1))
        :param l: A list containing a Tuple of paths and their return
        :return: a z3 expression of If
        """
        if len(l) == 0:
            return -1  # make sure this "unreachable condition" at the very end of the Ifs can never lead to a valid result because we use 1 for true and 0 for false, so -1 is invalid in the context -> leads to unsat in any case
        else:
            current_element = l[0]
            path_condition = current_element[0]
            result = current_element[1]
            if isinstance(result, bool):
                result = 1 if result else 0  # converts boolean result into an int, to be compatible with the -1 (the unreachable condition)
            if is_bool(result):
                result = z3.IntSort().cast(result)  # same here
            return If(path_condition, result, self.path_list_to_nested_ifs(l[1:]))


    def split_z3_expr_list_to_constraint_and_inv(self, path: List[ExprRef], cdt_dict, app_name: str) -> ExprRef:
        quantifier_list, quantifier_free_list = set(), set()
        for expr in path:  # remove quantifiers as they should already have their conditions
            if is_quantifier(expr):
                quantifier_list.add(expr)
            else:
                quantifier_free_list.add(expr)
        if len(quantifier_free_list) == 0:
            return self.expression_list_to_conjunction(list(quantifier_list))
        s = Solver()
        s.reset()
        s.add(quantifier_free_list)
        if not (
                "unsat" in s.check().__str__()):  # path of the function must be valid,
            # this condition must always be valid. It is indeed a path condition and if it is unsatisfiable, it means the path cannot be reached which is impossible because Crosshair explored it. 
            # This means that something wrong was done during code manipulation.
            # Moreover, some of the variables of the path condition could be replaced by function calls, so we better catch unsatisfiable constraints here.
            GA_condtion_lists = []
            other_conditions = []
            for z3_expr in quantifier_free_list:
                tup = z3util.get_vars(z3_expr)
                GA_z3 = None
                if len(tup) == 1:  # Given the quantifier_free_list always have ASTs, it should be a list of single constants
                    GA_z3 = tup[0]
                elif len(tup) > 1:
                    raise ValueError(f"unknown case for {tup}")
                else:  # Likely a "True" Or "False", which we'll just ignore
                    GA_condtion_lists.append(z3_expr)
                    continue
                    # returns a list, but here it's always one element inside this list
                GA_str = self.crosshair_z3_var_to_var_str(GA_z3)  # convert CH GA to actual GA name
                if "INT_" in GA_str or "FLOAT_" in GA_str or "BOOL_" in GA_str:
                    GA_str = app_name + GA_str
                if GA_str in cdt_dict and "time_" not in GA_str:  # check if f(GA) satisfies the given constraint from the current invariant path
                    f = cdt_dict[GA_str]
                    GA_cdt = z3_expr
                    if is_not(z3_expr):
                        if z3_expr.arg(
                                0).num_args() == 0:  # extract what's inside the not(expr) and if num_args of expr ==0, we know it's a Not(var) i.e. Not(a)
                            cdt = f == 0
                        else:  # its a Not(expression) i.e Not(a>10)
                            # subsitute the value GA_z3 (i.e. GA_0_0_1) by the function f_GA_0_0_1() in expression z3_expr
                            cdt = z3.substitute(z3_expr, (GA_z3, f))
                    elif is_const(z3_expr):
                        cdt = f == 1
                    elif z3.is_expr(z3_expr):
                        if GA_z3.sort() != f.sort():
                            raise ValueError(f"Can't swap condtion {GA_z3} in the function {f}" +
                                            f"because {GA_z3} is {GA_z3.sort()} but in the iteration function it has been assigned to {f.sort()}")
                        cdt = z3.substitute(z3_expr, (
                            GA_z3, f))  # changing path condition to replace the GA condition to the function
                    elif z3.is_int(z3_expr):
                        cdt = int(GA_cdt.as_long())
                    else:
                        raise ValueError(f"unknown type for GA {GA_str} type: {type(GA_cdt)}")
                    self.debug(f"we want {GA_str} == {cdt}, expr: {cdt}", type(f), type(cdt))
                    GA_condtion_lists.append(cdt)
                else:
                    # non GA constraints, such as time
                    if "GA" in GA_str or "INT_0" in GA_str:
                        raise ValueError(f"variable {GA_str} is not on the cdt_dict! {cdt_dict}")
                    self.debug(f"adding non GA constraint {GA_str} for condition {z3_expr}")
                    other_conditions.append(z3_expr)
            GA_condtion_lists.extend(quantifier_list)
            if len(GA_condtion_lists) == 0:
                return self.expression_list_to_conjunction(other_conditions)
            else:
                GA_cdts = self.expression_list_to_conjunction(GA_condtion_lists)
            if len(other_conditions) > 0:
                t = self.expression_list_to_conjunction(other_conditions)
                return And(t, GA_cdts)
            else:
                return GA_cdts


        else:
            raise ValueError(f"unsat {simplify(And(path))}")


    def check_iteration_satisfies_invariant(self, iteration_function: Callable, invariant_function: Callable):
        variable_dict = {}
        replace_list, valid_paths_inv, app_name = self.invariant_function_to_paths_with_check_to_replace(invariant_function,
                                                                                                    variable_dict)

        cdt_dict = self.run_crosshair_on_iteration_fct(iteration_function, variable_dict)

        translated_checks = self.replace_checks_from_path_list(replace_list, valid_paths_inv, cdt_dict, app_name)

        all_cond = [self.split_z3_expr_list_to_constraint_and_inv(vp, cdt_dict, app_name) for vp in valid_paths_inv]
        all_cond_z3 = Or(all_cond) #create a disjunction of all possible paths
        for v in variable_dict.values():
            if not is_const(v):
                raise ArgumentError(f"invalid bounded var {v}")
        s = Solver()
        s.reset()
        s.add(ForAll(list(variable_dict.values()), all_cond_z3))
        is_sat = s.check() != unsat
        print("solver is ", s.check())
        if is_sat:
            return is_sat,""
        else:
            s.reset()
            s.add(Not(all_cond_z3))
            s.check()
            is_sat = s.check() != unsat
            if is_sat:
                m = s.model()
                simp = simplify(all_cond_z3)
                if len(m) == 0 and "False" == str(simp):
                    e = "ERROR: the conditions are always false, check your functions"
                    return False, e
                if all(map(lambda x: not is_quantifier(x), all_cond)):
                    return not is_sat, self.get_counterexample(all_cond_z3, s, translated_checks)
                else:
                    print("checking each condition:")
                    all_conditions_solved = ""
                    self.debug(all_cond)
                    for c in all_cond:
                        all_conditions_solved += self.get_counterexample(c, s, translated_checks)
                return not is_sat, all_conditions_solved
        return is_sat, ""


    def get_counterexample(self, c: ExprRef, s: Solver, translated_checks:List[Tuple]):
        """
        Generates a counterexample as a string from a condition c and a solver s
        :param c: The z3 condition
        :param s: The solver
        :return: The message giving the counterexample (if it exists) of the given condition c
        """
        s.reset()
        s.add(Not(c))
        r = s.check()
        if r == z3.unknown:
            return f"failed to prove {c}"
        else:
            c_str = c.__str__()
            for translated_check_pair in translated_checks:
                c_str_no_spaces = c_str.replace("\n", "").replace(" ", "")
                c_str = c_str_no_spaces.replace(translated_check_pair[0].replace("\n", "").replace(" ", ""),
                                                translated_check_pair[1].replace("\n", ""))
        m = s.model()
        if len(m) > 0:
            return f"counterexample {s.model()} for condition: {c_str}\n"
        else:
            # condition contains only quantifiers
            return f"This condition is always false: {c_str}\n"


    def crosshair_z3_var_to_var_str(self, variable: ExprRef) -> str:
        """
        :param variable: Converts a z3 Const/variable used by crosshair (that has an _NUMBER) at the end of the name to its original name.
        :return: The actual variable name as a string
        """
        return "_".join(variable.__str__().split('_')[:-1])


    def replace_checks_from_path_list(self, replace_list, valid_paths_inv, cdt_dict, app_name) -> List[Tuple]:
        translated_checks: List[Tuple] = []
        if len(replace_list) > 0:
            translated_checks = self.replace_removed_checks(valid_paths_inv, replace_list, cdt_dict, app_name)
        if len(valid_paths_inv) == 0:
            print("WARNING: all paths lead to None :( \n check your invariant function")
            valid_paths_inv.append([z3.BoolVal(True)])
        return translated_checks


    def invariant_function_to_paths_with_check_to_replace(self, invariant_function: Callable,
                                                        variable_dict: Dict[str, z3.ExprRef]):
        f = inspect.getsource(invariant_function)
        check_container = self.check_function_replace(f)
        replace_list = check_container.list_of_replaced_checks
        if len(replace_list) > 0:
            imports = textwrap.dedent(f"""
            from .{FUNCTION_VERIFICATION_FILE_NO_EXT} import *
            """)
            c = self.string_to_callable(imports + check_container.function, invariant_function.__name__)
        else:
            c = invariant_function
        paths, exhausted = self.get_paths(FunctionInfo.from_fn(c))
        self.print_warning_if_not_exhausted(exhausted)
        valid_paths_inv = self.extract_valid_paths_from_invariant(paths)
        app_state_name = self.extract_app_name(invariant_function, paths)
        for vp in valid_paths_inv:
            for e in vp:
                tup = z3util.get_vars(e)
                variable = None if len(tup) == 0 else tup[0]
                v = self.crosshair_z3_var_to_var_str(variable)
                if "INT_" in v or "FLOAT_" in v or "BOOL_" in v:
                    v = app_state_name + v
                if variable is not None:
                    self.add_z3_var_to_var_dict(variable_dict, v, variable)
        return replace_list, valid_paths_inv, app_state_name


    def extract_app_name(self, invariant_function, paths):
        app_state_list = list(filter(lambda x: "app_state" in x, paths[0].args.arguments.keys()))
        app_state_name = ""
        if len(app_state_list) == 1:
            app_state_name = app_state_list[0]
        elif len(app_state_list) > 1:
            self.debug("multiple app state detected, trying to infer from app name")
            app_name = invariant_function.__name__.removesuffix("_invariant")
            possible_app_name = list(filter(lambda x: app_name in x, app_state_list))
            if len(possible_app_name) > 1:
                raise ValueError(f"multiple app names : {possible_app_name} cannot decide")
            elif len(possible_app_name) == 0:
                raise ValueError(f"no match for the possible app name {app_name} given {app_state_list}")
            else:
                app_state_name = possible_app_name[0]
        return app_state_name
