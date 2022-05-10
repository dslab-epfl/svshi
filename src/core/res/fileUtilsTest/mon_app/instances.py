###
### DO NOT TOUCH THIS FILE!!!
###

from models.binary import BinarySensor
from models.switch import Switch

from models.state import AppState

DEVICE_ONE = BinarySensor('device_one')
DEVICE_TWO = Switch('device_two')

app_state = AppState()