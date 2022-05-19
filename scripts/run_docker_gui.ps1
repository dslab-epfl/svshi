docker run --rm -d -v svshiVolume:/home/maki/ -p 3000:3000 -p 4242:4242 --name svshi -i svshi:ubuntu22.04

docker exec svshi /bin/bash -c "svshi gui -a $(docker inspect --format '{{ .NetworkSettings.IPAddress }}' 'svshi'):4242 &"
docker exec svshi /bin/bash -c "cd /home/maki/svshi/src/svshi_gui && serve -s dist"