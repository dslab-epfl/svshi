# Docker container to run SVSHI, the Secured and Verified Smart Home Infrastructure

# COMMANDS:

# BUILD:
## docker build -t svshi:ubuntu22.04 .

# START AND RUN (execute from `svshi` folder):
## 'docker run --rm -v $PWD:/pwd --cap-add=SYS_PTRACE --security-opt seccomp=unconfined -d --name svshi -i svshi:ubuntu22.04 && docker exec -it svshi /bin/bash'

FROM ubuntu:22.04

ENV DEBIAN_FRONTEND noninteractive

# Installing basic packages 
RUN dpkg --add-architecture i386 
RUN apt-get update 
RUN apt-get install -y software-properties-common  
RUN add-apt-repository universe 
RUN apt-get install -y zip unzip build-essential jq curl wget rubygems gcc gdb python3 python3-pip python3-dev python3.9-venv wget git nano 
RUN apt-get install -y openjdk-11-jdk scala
RUN apt-get install sudo

RUN apt-get update && \
    apt-get install apt-transport-https curl gnupg -yqq && \
    echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | tee /etc/apt/sources.list.d/sbt.list && \
    echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | tee /etc/apt/sources.list.d/sbt_old.list && \
    curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | gpg --no-default-keyring --keyring gnupg-ring:/etc/apt/trusted.gpg.d/scalasbt-release.gpg --import && \
    chmod 644 /etc/apt/trusted.gpg.d/scalasbt-release.gpg && \
    apt-get update && \
    apt-get install sbt

# Change root's password
RUN echo 'root:nigiri' | chpasswd


# Add non-root user
RUN useradd -rm -d /home/maki -s /bin/bash -g root -G sudo -u 1001 maki
USER maki
WORKDIR /home/maki


# Install pip3 as maki
RUN pip3 install -U pip


# Update env variables
ENV PATH="/home/maki/.local/lib/python3.9/site-packages:${PATH}"
ENV PATH="/home/maki/.local/bin:${PATH}"
ENV PATH="/home/maki/local/bin:${PATH}"
ENV SVSHI_HOME="/home/maki/svshi"

RUN mkdir ${SVSHI_HOME}

# Copy the files
COPY . ${SVSHI_HOME}/

## UNCOMMENT IF YOU WANT TO INSTALL CROSSHAIR FROM SOURCE and change the pip3 install below (e.g. if you forked the repo) -----------------------
# # Install crosshair from main branch of the repo FROM SOURCE
# RUN cd /home/maki/ && mkdir crosshair_local
# RUN cd /home/maki/crosshair_local && git clone https://github.com/pschanely/CrossHair.git

# ENV VIRTUAL_ENV=/home/maki/sushi_python_env

# RUN python3 -m venv $VIRTUAL_ENV
# ENV PATH="$VIRTUAL_ENV/bin:$PATH"
# RUN cd /home/maki/crosshair_local/CrossHair && pip3 install --editable .
## UNCOMMENT IF YOU WANT TO INSTALL CROSSHAIR FROM SOURCE and change the pip3 install below (e.g. if you forked the repo) -----------------------


# Install python requirements
RUN cd ${SVSHI_HOME}/src && [ -f requirements.txt  ] && pip3 install -r requirements.txt

# Install SVSHI
RUN cd ${SVSHI_HOME}/ && ./build.sh