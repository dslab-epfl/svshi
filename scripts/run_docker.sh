docker run --rm -d -v svshiVolume:/home/maki/ -p 3000:3000 -p 4242:4242 --name svshi -i svshi:ubuntu22.04
docker exec -it svshi /bin/bash