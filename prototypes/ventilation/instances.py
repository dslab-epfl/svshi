###
### DO NOT TOUCH THIS FILE!!!
###

from models.binary import BinarySensor
from models.co2 import CO2Sensor
from models.switch import Switch

from models.state import AppState

PRESENCE_DETECTOR = BinarySensor('presence_detector')
CO_TWO_SENSOR = CO2Sensor('co_two_sensor')
VENTILATION = Switch('ventilation')

app_state = AppState()