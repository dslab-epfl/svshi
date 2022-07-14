docker run --rm -d -v svshiVolumeDev:/home/maki/ -p 3000:3000 -p 4242:4242 --name svshi_dev -i svshi_dev:ubuntu22.04
docker exec -it svshi_dev /bin/bash