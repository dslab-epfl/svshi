import os
import shutil

import pytest

from . import list_of_test_functions
from ..verification_functions import *
from z3 import *


@dataclasses.dataclass
class PhysicalState:
    GA_1_1_1: float
    GA_1_1_2: float
    GA_1_1_3: bool
    GA_1_1_4: bool


@dataclasses.dataclass
class AppState:
    INT_0: int = 0
    INT_1: int = 0
    INT_2: int = 0
    INT_3: int = 0
    FLOAT_0: float = 0.0
    FLOAT_1: float = 0.0
    FLOAT_2: float = 0.0
    FLOAT_3: float = 0.0
    BOOL_0: bool = False
    BOOL_1: bool = False
    BOOL_2: bool = False
    BOOL_3: bool = False


@dataclasses.dataclass
class fakePathSummary:
    result: AstVector


physical_state = PhysicalState(0, 1, True, False)
s = Solver()
SVSHI_HOME: Final = os.environ["SVSHI_HOME"]
TESTS_DIRECTORY = SVSHI_HOME+"/src/extended_verification/tests"
FUNCTIONS_TEST_FILENAME = "list_of_test_functions.py"

verif_functions = VerificationFunctions()


@pytest.fixture(autouse=True)
def run_before_and_after_tests():
    """Fixture to execute setup and cleanup"""
    # Setup
    shutil.copy(f"{TESTS_DIRECTORY}/" + FUNCTIONS_TEST_FILENAME, f"{TESTS_DIRECTORY}/../" + FUNCTION_VERIFICATION_FILE)

    yield  # this is where the testing happens

    os.remove(f"{TESTS_DIRECTORY}/../" + FUNCTION_VERIFICATION_FILE)
    if os.path.exists(f"{TESTS_DIRECTORY}/../temp_check.py"):
        os.remove(f"{TESTS_DIRECTORY}/../temp_check.py")


def test_run_crosshair_var_dict():
    var_dict = {}
    verif_functions.run_crosshair_on_iteration_fct(list_of_test_functions.app_test_run_crosshair, var_dict)
    print(var_dict)
    assert ('GA_0_0_3' in var_dict and 'INT_0_' in var_dict[
        'GA_0_0_3'].__str__()), f"GA GA_0_0_3 is supposed to be INT_0_* but was {var_dict['GA_0_0_3']}"
    assert 'GA_0_0_4' not in var_dict, f"key is {var_dict['GA_0_0_4']} but shouldn't exist as GA_0_0_4 is only a value in this fonction"


def test_check_expr_is_sat_for_time_constraint():
    time_hour_14 = Int('time_hour_14')
    f1 = If(Not(10 <= time_hour_14), 0, If(10 <= time_hour_14, 1, -1)) == 1
    s.reset()
    s.add(verif_functions.check(every=Day(1), duration=Hour(10), cdt=f1))
    out = s.check()
    assert out.__repr__() == "sat"


def test_check_expr_is_sat_for_time_constraint_every_two_weeks():
    expected_out_readable = """
ForAll([time_weekday_14,
        time_hour_14,
        time_min,
        time_day,
        time_month,
        time_year],
       Implies(And(time_day >= 1,
                   time_day <= 7,
                   time_hour_14 >= 0,
                   time_hour_14 <= 23,
                   time_weekday_14 >= 1,
                   time_weekday_14 <= 4,
                   time_month >= 1,
                   time_month <= 12,
                   time_min >= 0,
                   time_min <= 59),
               And(Exists([t, time_weekday_14],
                         And(And(time_day >= 1,
                                 time_day <= 7,
                                 time_hour_14 >= 0,
                                 time_hour_14 <= 23,
                                 time_weekday_14 >= 1,
                                 time_weekday_14 <= 4,
                                 time_month >= 1,
                                 time_month <= 12,
                                 time_min >= 0,
                                 time_min <= 59),
                             time_weekday_14 >= 1,
                             time_weekday_14 <= 2,
                             And(And(t >= 0, t <= 13),
                                 ForAll(time_hour_14,
                                        Implies(And(t <=
                                        time_hour_14,
                                        time_hour_14 <=
                                        t + 10),
                                        If(And(time_weekday_14 ==
                                        1,
                                        Not(time_hour_14 >  
                                        10)),
                                        1,
                                        If(And(time_weekday_14 ==
                                        3,
                                        time_hour_14 >= 10),
                                        1,
                                        -1)) ==
                                        1))))),
                  Exists([t, time_weekday_14],
                         And(And(time_day >= 1,
                                 time_day <= 7,
                                 time_hour_14 >= 0,
                                 time_hour_14 <= 23,
                                 time_weekday_14 >= 1,
                                 time_weekday_14 <= 4,
                                 time_month >= 1,
                                 time_month <= 12,
                                 time_min >= 0,
                                 time_min <= 59),
                             time_weekday_14 >= 3,
                             time_weekday_14 <= 4,
                             And(And(t >= 0, t <= 13),
                                 ForAll(time_hour_14,
                                        Implies(And(t <=
                                        time_hour_14,
                                        time_hour_14 <=
                                        t + 10),
                                        If(And(time_weekday_14 ==
                                        1,
                                        Not(time_hour_14 >
                                        10)),
                                        1,
                                        If(And(time_weekday_14 ==
                                        3,
                                        time_hour_14 >= 10),
                                        1,
                                        -1)) ==
                                        1))))))))
    """
    time_hour_14, time_weekday_14 = Ints('time_hour_14 time_weekday_14')
    f1 = If(And(time_weekday_14 == 1, Not(10 < time_hour_14)), 1,
            If(And(time_weekday_14 == 3, 10 <= time_hour_14), 1, -1)) == 1
    s.reset()
    e = verif_functions.check(every=Week(2), duration=Hour(10), cdt=f1)
    s.add(e)
    out = s.check()
    assert out.__repr__() == "sat" and remove_line_ret_and_spaces(e.__str__()) == remove_line_ret_and_spaces(
        expected_out_readable)


def test_check_expr_is_unsat_for_time_constraint():
    time_hour_14 = Int('time_hour_14')
    f1 = If(Not(10 <= time_hour_14), 0, If(10 <= time_hour_14, 1, -1)) == 1
    s.reset()
    s.add(verif_functions.check(every=Day(1), duration=Hour(20), cdt=f1))
    out = s.check()
    assert out.__repr__() == "unsat"


def test_check_expr_is_unsat_for_external_value():
    time_hour_14 = Int('time_hour_14')
    GA_0_0_1_10 = Bool('GA_0_0_1_10')
    f1 = If(Not(10 <= time_hour_14), 0,
            If(And(10 <= time_hour_14, Not(GA_0_0_1_10)), 0, If(And(10 <= time_hour_14, GA_0_0_1_10), 1, -1))) == 1
    s.reset()
    e = verif_functions.check(every=Day(1), duration=Hour(10), cdt=f1)
    s.add(e)
    out = s.check()
    assert out.__repr__() == "unsat"


def test_check_expr_is_unsat_for_negative_time():
    time_hour_14 = Int('time_hour_14')
    f1 = If(time_hour_14 == -1, 1, -1) == 1
    s.reset()
    e = verif_functions.check(every=Day(1), duration=Hour(1), cdt=f1)
    s.add(e)
    out = s.check()
    assert out.__repr__() == "unsat"


def test_end_to_end_verification_function_empty_iteration_function():
    def no_fun():
        pass

    assert verif_functions.check_iteration_satisfies_invariant(list_of_test_functions.app_one_iteration, no_fun)[0]


def test_end_to_end_verification_function_sat():
    sat,out= verif_functions.check_iteration_satisfies_invariant(list_of_test_functions.app_one_iteration, list_of_test_functions.app_one_invariant)
    print(out)
    assert sat


def test_end_to_end_verification_function_sat_inv():
    assert verif_functions.check_iteration_satisfies_invariant(list_of_test_functions.app_one_iteration, list_of_test_functions.invariant_test_one)[0]


def test_end_to_end_verification_check_function_unsat_due_to_sensor():
    sat, out = verif_functions.check_iteration_satisfies_invariant(list_of_test_functions.app_two_iteration, list_of_test_functions.app_two_invariant)
    assert not sat


def test_end_to_end_verification_inv_unsat_due_to_sensor():
    sat, err = verif_functions.check_iteration_satisfies_invariant(list_of_test_functions.app_two_iteration, list_of_test_functions.app_two_invariant_no_check)
    print(err)
    expected_err = """
counterexample [time_hour_16 = 10, GA_0_0_1_10 = False] for condition: 
Or(Not(10 <= time_hour_16),
   And(10 <= time_hour_16,
       If(Not(10 <= time_hour_16),0,
          If(And(10 <= time_hour_16, Not(GA_0_0_1_10)),0,
             If(And(10 <= time_hour_16, GA_0_0_1_10), 1, -1))) == 1))
    """
    assert not sat and remove_line_ret_and_spaces(err) == remove_line_ret_and_spaces(expected_err)


def test_end_to_end_verification_function_sat_corner_case_aliasing():
    with pytest.raises(ValueError) as e:
        verif_functions.check_iteration_satisfies_invariant(list_of_test_functions.app_three_iteration, list_of_test_functions.app_three_invariant)
    assert str(e.value) == "Error on check condition function, make sure your conditions doesn't have any aliases"


def test_end_to_end_verification_function_sat_corner_case_multiple_lines_on_check():
    expected_out_readable = """[[ForAll([time_hour_16,
        GA_0_0_1_10,
        time_min,
        time_day,
        time_weekday,
        time_month,
        time_year],
       Implies(And(time_day >= 1,
                   time_day <= 7,
                   time_hour_16 >= 0,
                   time_hour_16 <= 23,
                   time_weekday >= 1,
                   time_weekday <= 4,
                   time_month >= 1,
                   time_month <= 12,
                   time_min >= 0,
                   time_min <= 59),
               Exists(t,
                      And(And(And(t >= 0, t <= 13),
                              ForAll(time_hour_16,
                                     Implies(And(t <=time_hour_16,time_hour_16 <=t + 10),
                                        Or(And(If(Not(10 <=time_hour_16),0,If(10 <=time_hour_16,1,-1)) ==0,
                                        If(Not(10 <=time_hour_16),If(GA_0_0_1_10,1,0),If(10 <=time_hour_16,If(GA_0_0_1_10,1,0),-1)) ==1),
                                        If(Not(10 <=time_hour_16),0,If(10 <=time_hour_16,1,-1)) ==1)))),
                          And(time_day >= 1,
                              time_day <= 7,
                              time_hour_16 >= 0,
                              time_hour_16 <= 23,
                              time_weekday >= 1,
                              time_weekday <= 4,
                              time_month >= 1,
                              time_month <= 12,
                              time_min >= 0,
                              time_min <= 59)))))
                              ]]"""
    expected_out = remove_line_ret_and_spaces(expected_out_readable)
    replace_list, valid_paths_inv,app_state_name = verif_functions.invariant_function_to_paths_with_check_to_replace(
        list_of_test_functions.invariant_test_line_return,{})
    assert app_state_name == "app_one_app_state"
    cdt_dict = verif_functions.run_crosshair_on_iteration_fct(list_of_test_functions.app_one_iteration)
    verif_functions.replace_checks_from_path_list(replace_list, valid_paths_inv, cdt_dict, list_of_test_functions.invariant_test_line_return)
    print(valid_paths_inv)
    assert remove_line_ret_and_spaces(valid_paths_inv.__str__()) == expected_out


def remove_line_ret_and_spaces(s: str) -> str:
    return s.replace('\n', "").replace(' ', '')


#
# def test_end_to_end_verification_function_sat_corner_case_empty_dict():
#     out = invariant_function_to_paths({},sample_functions.invariant_test_line_return)
#     assert len(out) == 2

def test_extract_valid_paths_from_invariant():
    GA_0_0_2_11 = Bool('GA_0_0_2_11')
    c0_19, c1_1a, time_hour_14 = Ints('c0_19 c1_1a time_hour_14')
    conditions_list = [[[Not(GA_0_0_2_11), 1 == c0_19, c1_1a == If(GA_0_0_2_11, 1, 0)], {'ret': True}],
                       [[Not(GA_0_0_2_11), 1 == c0_19, Not(c1_1a == If(GA_0_0_2_11, 1, 0))], {'ret': None}],
                       [[Not(GA_0_0_2_11), Not(1 == c0_19), Not(1 == c0_19)], {'ret': None}],
                       [[GA_0_0_2_11, Not(1 == c0_19), Not(1 == c0_19)], {'ret': False}]]

    t = [fakePathSummary(condition) for condition in conditions_list]
    u = verif_functions.extract_valid_paths_from_invariant(t)
    assert len(u) == 1 and u[0] == conditions_list[0][0]


def test_check_every_day():
    expected_str_readable = """
    ForAll([time_hour_14,
        time_min,
        time_day,
        time_weekday,
        time_month,
        time_year],
       Implies(And(time_day >= 1,
                   time_day <= 7,
                   time_hour_14 >= 0,
                   time_hour_14 <= 23,
                   time_weekday >= 1,
                   time_weekday <= 4,
                   time_month >= 1,
                   time_month <= 12,
                   time_min >= 0,
                   time_min <= 59),
               Exists(t,
                      And(And(And(t >= 0, t <= 58),
                              ForAll(time_min,
                                     Implies(And(t <=
                                        time_min,
                                        time_min <= t + 1),
                                        time_hour_14 == -1))),
                          And(time_day >= 1,
                              time_day <= 7,
                              time_hour_14 >= 0,
                              time_hour_14 <= 23,
                              time_weekday >= 1,
                              time_weekday <= 4,
                              time_month >= 1,
                              time_month <= 12,
                              time_min >= 0,
                              time_min <= 59)))))
    """
    time_hour_14 = Int('time_hour_14')
    expected_str = remove_line_ret_and_spaces(expected_str_readable)
    u = verif_functions.check(every=Day(1), duration=Minute(1), cdt=time_hour_14 == -1)
    s.reset()
    s.add(u)
    print(simplify(u))
    assert remove_line_ret_and_spaces(u.__str__()) == expected_str and s.check().__repr__() == "unsat"


def test_recursion_function_constructor():
    GA_0_0_2_11 = Bool('GA_0_0_2_11')
    c0_19, c1_1a, time_hour_14 = Ints('c0_19 c1_1a time_hour_14')
    l = [(GA_0_0_2_11, True), (And(Not(GA_0_0_2_11), c0_19 == 0), False)]
    out = verif_functions.path_list_to_nested_ifs(l)
    print(out)
    assert If(GA_0_0_2_11, 1, If(And(Not(GA_0_0_2_11), c0_19 == 0), 0, -1)) == out


def test_path_list_to_nested_ifs_two_elements():
    c0_19, c1_1a, time_hour_14 = Ints('c0_19 c1_1a time_hour_14')
    l = [(time_hour_14 >= 0, True)]
    out = verif_functions.path_list_to_nested_ifs(l)
    assert If(time_hour_14 >= 0, 1, -1) == out


def test_path_list_to_nested_ifs_no_elems():
    c0_19, c1_1a, time_hour_14 = Ints('c0_19 c1_1a time_hour_14')
    out = verif_functions.path_list_to_nested_ifs([])
    assert out == -1


def test_path_list_with_var_as_output_of_function():
    GA_0_0_2_11 = Bool('GA_0_0_2_11')
    c0_19, c1_1a, time_hour_14 = Ints('c0_19 c1_1a time_hour_14')
    l = [(GA_0_0_2_11, c1_1a), (And(Not(GA_0_0_2_11), c0_19 == 0), False)]
    out = verif_functions.path_list_to_nested_ifs(l)
    print(out)
    assert If(GA_0_0_2_11, c1_1a, If(And(Not(GA_0_0_2_11), c0_19 == 0), 0, -1)) == out


def test_iteration_no_conditions_unsat():
    assert not verif_functions.check_iteration_satisfies_invariant(list_of_test_functions.switch_off, list_of_test_functions.app_one_invariant)[0]


def test_iteration_no_conditions_sat():
    assert verif_functions.check_iteration_satisfies_invariant(list_of_test_functions.switch_on, list_of_test_functions.app_one_invariant)[0]


def test_empty_iteration_unsat_when_checking_only_false():
    assert not verif_functions.check_iteration_satisfies_invariant(list_of_test_functions.empty_fun, list_of_test_functions.check_empty_fun_off)[0]


def test_empty_iteration_unsat_when_checking_only_true():
    assert not verif_functions.check_iteration_satisfies_invariant(list_of_test_functions.empty_fun, list_of_test_functions.check_empty_fun_on)[0]


def test_float_check_sat():
    assert verif_functions.check_iteration_satisfies_invariant(list_of_test_functions.app_test_float, list_of_test_functions.check_on_float_sat)[0]


def test_float_check_unsat():
    assert not verif_functions.check_iteration_satisfies_invariant(list_of_test_functions.app_test_float, list_of_test_functions.check_on_float_unsat)[0]


def test_float_sat():
    assert verif_functions.check_iteration_satisfies_invariant(list_of_test_functions.app_test_float, list_of_test_functions.inv_on_float_sat)[0]


def test_app_test_float_depends_on_app_state_var_unsat():
    sat, counterexample = verif_functions.check_iteration_satisfies_invariant(list_of_test_functions.app_test_float_depends_on_app_state_var,
                                                              list_of_test_functions.inv_on_float_sat)
    assert not sat and "FLOAT_0" in counterexample


def test_float_unsat():
    assert not verif_functions.check_iteration_satisfies_invariant(list_of_test_functions.app_test_float, list_of_test_functions.inv_on_float_unsat)[0]


def test_contradictory_function():
    sat, err = verif_functions.check_iteration_satisfies_invariant(list_of_test_functions.switch_on, list_of_test_functions.inv_switch_off)
    assert not sat and err == "ERROR: the conditions are always false, check your functions"


def test_multiple_check_sat():
    assert verif_functions.check_iteration_satisfies_invariant(list_of_test_functions.app_test_float, list_of_test_functions.multiple_check_sat)[0]


def test_multiple_check_unsat():
    is_sat,counterexample = verif_functions.check_iteration_satisfies_invariant(list_of_test_functions.app_test_float, list_of_test_functions.multiple_check_unsat)
    assert not is_sat
    print(counterexample)
    assert counterexample == "This condition is always false: Or(And(svshi_api.check_time_property(Day(10),Hour(2),float_dev.read(physical_state,internal_state)>=14),svshi_api.check_time_property(Day(10),Hour(2),float_dev.read(physical_state, internal_state)>=11)))\n"


def test_extract_valid_paths_from_invariant_for_multiple_check():
    c0_1b = Int('c0_1b')
    v = [[[0 == c0_1b], {'ret': 1 == c0_1b}], [[Not(0 == c0_1b)], {'ret': 0 == c0_1b}]]

    for p in v:
        p[0] = z3_expr_list_to_ast_vector(p[0])

    t = [fakePathSummary(condition) for condition in v]
    assert str(verif_functions.extract_valid_paths_from_invariant(t)) == str([[0 == c0_1b, 1 == c0_1b]])


def test_boiler_case_not_compliant():
    assert not verif_functions.check_iteration_satisfies_invariant(list_of_test_functions.solar_boiler_app, list_of_test_functions.boiler_inv)[0]


def test_boiler_case_compliant():
    assert verif_functions.check_iteration_satisfies_invariant(list_of_test_functions.solar_boiler_app_compliant, list_of_test_functions.boiler_inv)[0]


def test_boiler_case_evil():
    sat, err = verif_functions.check_iteration_satisfies_invariant(list_of_test_functions.solar_boiler_app_evile, list_of_test_functions.boiler_inv)
    print(err)
    assert not sat




def z3_expr_list_to_ast_vector(p):
    vect = z3.AstVector()
    for cdt in p:
        vect.push(cdt)
    return vect
