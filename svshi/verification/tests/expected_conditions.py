from .verification_file import first_app_invariant, second_app_invariant, third_app_invariant, PhysicalState

def check_conditions(state: PhysicalState) -> bool:
    return first_app_invariant(state) and second_app_invariant(state) and third_app_invariant(state)