###
### DO NOT TOUCH THIS FILE!!!
###

from models.binary import BinarySensor
from models.switch import Switch

from models.state import AppState
from models.SvshiApi import SvshiApi

BINARY_SENSOR_INSTANCE_NAME = BinarySensor('binary_sensor_instance_name')
SWITCH_INSTANCE_NAME = Switch('switch_instance_name')

svshi_api = SvshiApi()
app_state = AppState()