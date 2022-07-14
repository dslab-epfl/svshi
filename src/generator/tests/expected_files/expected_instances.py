###
### DO NOT TOUCH THIS FILE!!!
###

from models.binarySensor import BinarySensor
from models.switch import Switch
from models.temperatureSensor import TemperatureSensor
from models.humiditySensor import HumiditySensor
from models.co2Sensor import CO2Sensor

from models.state import AppState
from models.SvshiApi import SvshiApi

BINARY_SENSOR_INSTANCE_NAME = BinarySensor('binary_sensor_instance_name')
SWITCH_INSTANCE_NAME = Switch('switch_instance_name')
TEMPERATURE_SENSOR_INSTANCE_NAME = TemperatureSensor('temperature_sensor_instance_name')
HUMIDITY_SENSOR_INSTANCE_NAME = HumiditySensor('humidity_sensor_instance_name')
CO_TWO_SENSOR_INSTANCE_NAME = CO2Sensor('co_two_sensor_instance_name')

svshi_api = SvshiApi()
app_state = AppState()