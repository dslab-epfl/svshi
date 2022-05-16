from .runtime_file import first_app_invariant, second_app_invariant, third_app_invariant, AppState, PhysicalState, InternalState

def check_conditions(first_app_app_state: AppState, second_app_app_state: AppState, third_app_app_state: AppState, physical_state: PhysicalState, internal_state: InternalState) -> bool:
    return first_app_invariant(first_app_app_state, physical_state, internal_state) and second_app_invariant(second_app_app_state, physical_state, internal_state) and third_app_invariant(third_app_app_state, physical_state, internal_state)