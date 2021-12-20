from verification_file import third_app_precond, PhysicalState

def check_conditions(state: PhysicalState) -> bool:
    return third_app_precond(state)