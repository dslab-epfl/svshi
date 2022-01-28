###
### DO NOT TOUCH THIS FILE!!!
###

from models.binary import BinarySensor

from models.state import AppState

DOOR_LOCK_SENSOR = BinarySensor('door_lock_sensor')
PRESENCE_DETECTOR = BinarySensor('presence_detector')

app_state = AppState()