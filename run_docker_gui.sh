docker run --rm --security-opt seccomp=unconfined --user root -p 3000:3000 -p 4242:4242 -d --name svshi -i svshi:ubuntu22.04
docker exec svshi /bin/bash -c "svshi gui -a $(docker inspect --format '{{ .NetworkSettings.IPAddress }}' 'svshi'):4242 &"
docker exec svshi /bin/bash -c "/home/maki/svshi/start_ui.sh"