from .verification_file import first_app_precond, second_app_precond, third_app_precond, PhysicalState

def check_conditions(state: PhysicalState) -> bool:
    return first_app_precond(state) and second_app_precond(state) and third_app_precond(state)