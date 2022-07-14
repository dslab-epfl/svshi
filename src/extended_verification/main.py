import importlib
import inspect
import os
import sys
from types import ModuleType
from typing import Final

from . import verification_functions



SVSHI_HOME: Final = os.environ["SVSHI_HOME"]


class UnsatError(Exception):
    ...


def run_extended_module_with_verification_file(module: ModuleType, verif_functions: verification_functions.VerificationFunctions):
    """
    run the extended verification on the given module
    :param module: the module to check, must contain a "system_behaviour" and "*invariant" function
    :return: return nothing, prints "CONFIRMED" if no counterexamples were found
    """
    fcts = dir(module)
    functions = inspect.getsource(module)
    with open(SVSHI_HOME + "/src/extended_verification/" + verification_functions.FUNCTION_VERIFICATION_FILE, "w") as funcfile:
        funcfile.write(functions)
    app_invariant_list = list(filter(lambda f: "invariant" in f, fcts))
    if len(app_invariant_list) == 0:
        raise ValueError("No invariants on the files")
    for inv in app_invariant_list:
        is_sat, out = verif_functions.check_iteration_satisfies_invariant(getattr(module, "system_behaviour"), getattr(module, inv))
        if not is_sat:
            raise UnsatError(f"ERROR: unsat for invariant {inv} " + out)
        else:
            print(f"CONFIRMED for invariant: {inv}")




if __name__ == '__main__':
    import argparse
    parser = argparse.ArgumentParser()

    #-mn MODULE_NAME -cto CONDITIONS_TIMEOUT -pto PATH_TIMEOUT
    parser.add_argument("module_name", help="Module to verify, must contain system_behavior and invaraiant functions")
    parser.add_argument("-cto", "--per_condition_timeout", help="Crosshair's condition timeout in seconds, default: 30",type=float,default=30.0)
    parser.add_argument("-pto", "--per_path_timeout", help="Crosshair's path timeout in seconds, default: 30",type=float,default=30.0)

    args = parser.parse_args()
    module_name = args.module_name
    verif_functions = verification_functions.VerificationFunctions(per_path_timeout = args.per_path_timeout, per_condition_timeout=args.per_condition_timeout)

    module = importlib.import_module(module_name)
    try:
        run_extended_module_with_verification_file(module, verif_functions=verif_functions)
    except UnsatError as e:
        exception_str = e.__str__()
        #remove counterexample's condition to ease reading
        counterexample = exception_str.split(" for condition:")[0]
        print(counterexample)
        exit(-1)
