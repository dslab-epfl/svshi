###
### DO NOT TOUCH THIS FILE!!!
###

from models.binarySensor import BinarySensor
from models.switch import Switch

from models.state import AppState

BINARY_SENSOR_INSTANCE_NAME = BinarySensor('binary_sensor_instance_name')
SWITCH_INSTANCE_NAME = Switch('switch_instance_name')

app_state = AppState()