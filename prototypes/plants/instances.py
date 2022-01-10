###
### DO NOT TOUCH THIS FILE!!!
###

from models.humidity import HumiditySensor

from models.state import AppState

HUMIDITY_SENSOR = HumiditySensor('humidity_sensor')

app_state = AppState()