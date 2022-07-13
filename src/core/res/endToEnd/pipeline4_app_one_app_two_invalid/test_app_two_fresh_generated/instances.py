###
### DO NOT TOUCH THIS FILE!!!
###

from models.temperatureSensor import TemperatureSensor
from models.switch import Switch

from models.state import AppState

TEMPERATURE_SENSOR = TemperatureSensor('temperature_sensor')
SWITCH = Switch('switch')

app_state = AppState()