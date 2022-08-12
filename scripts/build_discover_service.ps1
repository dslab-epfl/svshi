cd ../src/simulator-knx/docker_opengl_image
./build_image.sh
cd ../../..
docker compose -f web_service_compose.yaml build