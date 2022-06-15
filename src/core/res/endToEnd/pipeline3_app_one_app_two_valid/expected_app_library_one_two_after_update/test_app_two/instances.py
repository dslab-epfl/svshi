###
### DO NOT TOUCH THIS FILE!!!
###

from models.temperature import TemperatureSensor

from models.state import AppState
from models.SvshiApi import SvshiApi

TEMPERATURE_SENSOR = TemperatureSensor('temperature_sensor')

svshi_api = SvshiApi()
app_state = AppState()