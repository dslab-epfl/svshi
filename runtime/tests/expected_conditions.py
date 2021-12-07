from verification_file import another_app_precond, app_precond, PhysicalState

def check_conditions(state: PhysicalState) -> bool:
    return another_app_precond(state) and app_precond(state)