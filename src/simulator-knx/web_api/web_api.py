from http.client import BAD_REQUEST
from xmlrpc.client import INTERNAL_ERROR
from aioreactive import catch
from flask import Flask, request
from flask import jsonify

# from requests import request

import sys, os
import threading
import queue
from threading import Lock
import subprocess

data_lock = Lock()

app = Flask(__name__)
host = "127.0.0.1"
port = 4646

SUCCESS_CODE = 200
INTERNAL_ERROR_CODE = 500
BAD_REQUEST_CODE = 400


class FlaskAppProp:
    HOST: str = "127.0.0.1"
    PORT: int = 4646


# callback_queue = queue.Queue() #
sim_start_event = (
    threading.Event()
)  # bool event to trigger sim launch: True to launch sim, False to stop it
sim_stop_event = threading.Event()
sim_stop_event.set()  # Initialize stop event to True as simulation is not running

sim_waiting_for_svshi = threading.Event()

# config_path_webapp = None
config_webapp_name = None

SVSHI_HOME = os.environ["SVSHI_HOME"]

sys.path.append("./simulator")
# simulator_running = False

# TODO prepare simulator stuff if needed


@app.route("/simulator/ipAddr", methods=["GET"])
def getIpAddress():
    return (
        jsonify(address=FlaskAppProp.HOST),
        SUCCESS_CODE,
    )  # {"isRunning": True}


@app.route("/simulator/running", methods=["GET"])
def isRunning():
    return (
        jsonify(isRunning=sim_start_event.is_set()),
        SUCCESS_CODE,
    )  # {"isRunning": True}


@app.route("/simulator/start", methods=["POST"])
def startSimulator():

    sim_stop_event.clear()
    sim_start_event.set()
    global sim_waiting_for_svshi
    sim_waiting_for_svshi.wait()  # wait until simulator is waiting for svshi to be run, and return success code 200
    return jsonify(success=True)


@app.route("/simulator/stop", methods=["POST"])
def stopSimulator():
    sim_start_event.clear()
    sim_stop_event.set()
    return jsonify(success=True)


@app.route("/simulator/config", methods=["POST"])
def setConfig():
    # app = request.form["app"] # app name str
    # path = os.path.dirname(os.path.dirname(".")) ### TODO finish this shit
    config_path = f"{SVSHI_HOME}/src/simulator-knx/config/"  # empty_config
    empty_config_path = config_path + "webapp_base_config.json"
    # apps_path = f"{SVSHI_HOME}/src/simulator-knx/svshi_apps/"
    # app_name = "app_one_two"
    # app_path = apps_path + app_name + "/"
    body = request.json
    print(type(body))

    from simulator.tools import config_from_request

    global config_webapp_name
    config_webapp_name = config_from_request(empty_config_path, body)
    print(f"web_api :: webapp name: {config_webapp_name}")
    return jsonify(success=True), SUCCESS_CODE


# @app.route("/simulator/simulation-state", methods=["GET"])
# def getSimulationState():
#     # TODO read the state and return it
#     simulationState = []
#     return jsonify(simulationState), SUCCESS_CODE


# TODO continue implementing other entrypoints


# if __name__ == "__main__":
#     app.run(host=host, port=port, debug=True)
