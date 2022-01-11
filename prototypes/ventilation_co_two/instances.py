###
### DO NOT TOUCH THIS FILE!!!
###

from models.co2 import CO2Sensor
from models.switch import Switch

from models.state import AppState

CO_TWO_DETECTOR = CO2Sensor('co_two_detector')
VENTILATION = Switch('ventilation')

app_state = AppState()