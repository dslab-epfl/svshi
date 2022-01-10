###
### DO NOT TOUCH THIS FILE!!!
###

from models.binary import BinarySensor
from models.switch import Switch

from models.state import AppState

PRESENCE_DETECTOR = BinarySensor('presence_detector')
VENTILATION = Switch('ventilation')

app_state = AppState()