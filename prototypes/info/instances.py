###
### DO NOT TOUCH THIS FILE!!!
###

from models.co2 import CO2Sensor
from models.temperature import TemperatureSensor

from models.state import AppState

CO_TWO = CO2Sensor('CO_TWO')
TEMPERATURE = TemperatureSensor('temperature')

app_state = AppState()