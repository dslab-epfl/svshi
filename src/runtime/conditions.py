# Default file, will be overwritten while running
        
from .runtime_file import AppState, PhysicalState, InternalState

def check_conditions(physical_state: PhysicalState, internal_state: InternalState, **app_state: AppState) -> bool:
    return True