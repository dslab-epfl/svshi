cd ../..
docker build --tag svshi_webservice:ubuntu22.04 --label svshi_docker_webservice -f "src/web_service/Dockerfile" .