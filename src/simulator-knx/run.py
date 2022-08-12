""" Program starting hook. """

import sys
import threading
import time

# from threading import Lock

sys.path.append("./simulator")
sys.path.append("./web_api")

from simulator.simulator import launch_simulation, stop_simulation, pyglet_running
from simulator.tools import arguments_parser
from web_api import app, host, port, sim_start_event, sim_stop_event, FlaskAppProp
from simulator.svshi_interface import Interface


def flask_thread(host, port):
    FlaskAppProp.HOST = host
    app.run(host=host, port=port, debug=False, use_reloader=False)
    # , "debug=True" enables reloader = relaod flask server when code changes, but must run on mainthread, so we remove it to run pyglet on main thread


def sim_stop_thread():
    while True:
        sim_stop_event.wait()
        sim_stop_state = (
            stop_simulation()
        )  # returns 1 if pyglet was running and window closed, 0 if pyglet was not running
        if sim_stop_state is not None and sim_stop_state == 1:
            print("\n>>>>>>> Simulation STOP <<<<<<<<\n")

        sim_start_event.wait()
        # else: # if simulation was not running, nothing to stop


def main(argv):
    # print(f"current thread name: {threading.currentThread().getName()}")

    # Parsed CLI arguments given by the user when launching the program
    options = arguments_parser(argv)
    WEB_APP = options[
        7
    ]  # web app option, to determine if we need to launch flask or not
    print("options:\n")
    for o in options:
        print(o)
    if WEB_APP:
        address_port = options[8]
        address = address_port.split(":")[0]
        port = address_port.split(":")[1]
        Interface.HOST_ADDR = address
        ft = threading.Thread(target=flask_thread, args=(address, port))
        # print(f"flask thread name: {ft.getName()}")
        ft.start()

        sst = threading.Thread(target=sim_stop_thread, args=())
        # print(f"sim stop thread name: {sst.getName()}")
        sst.start()

        # print(f"sim event: {sim_start_event}, bool: {(sim_start_event.is_set())}\n")
        while True:
            sim_start_event.wait()
            print("\n>>>>>>> Simulation Starting <<<<<<<<\n")
            from web_api import config_webapp_name

            # global config_webapp_name
            if config_webapp_name is not None:
                print(f"run :: webapp name: {config_webapp_name}")
                options[4] = "./config/" + config_webapp_name + ".json"
            launch_simulation(options, fresh_knx_interface=True)
            sim_stop_event.wait()
            time.sleep(1)  # leave time to close simulation

    else:  # launch simulation in local without flask
        launch_simulation(options)


if __name__ == "__main__":
    main(sys.argv)
