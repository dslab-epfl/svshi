###
### DO NOT TOUCH THIS FILE!!!
###

from models.binarySensor import BinarySensor
from models.switch import Switch

from models.state import AppState

BINARY = BinarySensor('binary')
SWITCH = Switch('switch')

app_state = AppState()