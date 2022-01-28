from .runtime_file import test_app_one_invariant, AppState, PhysicalState

def check_conditions(test_app_one_app_state: AppState, physical_state: PhysicalState) -> bool:
    return test_app_one_invariant(test_app_one_app_state, physical_state)
