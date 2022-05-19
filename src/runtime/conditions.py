# Default file, will be overwritten while running
        
from .runtime_file import AppState, PhysicalState, InternalState

def check_conditions(app_state: AppState, state: PhysicalState, internal_state: InternalState) -> bool:
    return True