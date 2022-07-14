###
### DO NOT TOUCH THIS FILE!!!
###

from models.humiditySensor import HumiditySensor

from models.state import AppState
from models.SvshiApi import SvshiApi

HUMIDITY_SENSOR = HumiditySensor('humidity_sensor')

svshi_api = SvshiApi()
app_state = AppState()