# Run the Stomp server
coilmq

# TODO: Run the runtime verifier

# Run all the apps
cd ../app-library
for d in */ ; do
    cd $d
    ./run.sh &
    cd ..
done