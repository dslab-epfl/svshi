#!/bin/bash

# Run the Stomp server
coilmq &
coilmq_pid=$!

sleep 1

#  Run the runtime module
python3 -m runtime.main

sleep 1

# Kill the Stomp server
kill $coilmq_pid