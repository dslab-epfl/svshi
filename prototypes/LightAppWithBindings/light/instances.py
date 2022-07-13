###
### DO NOT TOUCH THIS FILE!!!
###

from models.switch import Switch
from models.binarySensor import BinarySensor

from models.state import AppState
from models.SvshiApi import SvshiApi

LIGHT = Switch('light')
SENSOR = BinarySensor('sensor')

svshi_api = SvshiApi()
app_state = AppState()