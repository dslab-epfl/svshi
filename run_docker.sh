docker run --rm -v $PWD:/pwd --cap-add=SYS_PTRACE --security-opt seccomp=unconfined -d --name svshi -i svshi:ubuntu22.04
docker exec -it svshi /bin/bash