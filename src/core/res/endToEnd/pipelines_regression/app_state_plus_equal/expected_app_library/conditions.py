from .runtime_file import door_lock_invariant, AppState, PhysicalState

def check_conditions(door_lock_app_state: AppState, physical_state: PhysicalState) -> bool:
    return door_lock_invariant(door_lock_app_state, physical_state)