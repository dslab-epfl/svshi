###
### DO NOT TOUCH THIS FILE!!!
###

from models.binarySensor import BinarySensor

from models.state import AppState
from models.SvshiApi import SvshiApi

DOOR_LOCK_SENSOR = BinarySensor('door_lock_sensor')
PRESENCE_DETECTOR = BinarySensor('presence_detector')

svshi_api = SvshiApi()
app_state = AppState()