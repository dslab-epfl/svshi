cd app-library
for d in */ ; do
    cd $d
    ./run.sh &
    cd ..
done