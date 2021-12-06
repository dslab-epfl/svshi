from verification_file import app_precond, another_app_precond, PhysicalState

def check_conditions(state: PhysicalState) -> bool:
    return app_precond(state) and another_app_precond(state)