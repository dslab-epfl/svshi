#!/bin/bash

# Run the Stomp server
coilmq &
coilmq_pid=$!

sleep 1

# Run all the apps
apps_pid=()
cd ../app_library
for d in */ ; do
    python3 $d/main.py &
    apps_pid+=($d$!)
done

cd ..

sleep 1

#  Run the runtime verifier
cd runtime
python3 main.py -l "${apps_pid[@]}"

sleep 1

# Kill all remaining apps
for app in "${apps_pid[@]}"; do
    arr=(${app//// })
    pid=${arr[1]}
    kill $pid  
done

sleep 1

# Kill the Stomp server
kill $coilmq_pid