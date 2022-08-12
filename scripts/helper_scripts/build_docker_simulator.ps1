cd ../../src/simulator-knx/docker_opengl_image 
./build_image.sh
cd ../../..
docker build --tag simulator_knx:ubuntu22.04 --label simulator_knx -f "src/simulator-knx/Dockerfile" .