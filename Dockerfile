# Docker container to run SVSHI, the Secured and Verified Smart Home Infrastructure

# COMMANDS:

# BUILD:
## docker build -t svshi:ubuntu22.04 .

# START AND RUN (execute from `smartinfra` folder):
## 'docker run --rm -v $PWD:/pwd --cap-add=SYS_PTRACE --security-opt seccomp=unconfined -d --name svshi -i svshi:ubuntu22.04 && docker exec -it svshi /bin/bash'

FROM ubuntu:22.04

ENV DEBIAN_FRONTEND noninteractive

# Installing basic packages 
RUN dpkg --add-architecture i386 && \
    apt-get update && \
    apt-get install -y software-properties-common  && \
    add-apt-repository universe && \
	apt-get install -y zip unzip build-essential jq curl wget rubygems gcc gdb python3 python3-pip python3-dev wget git nano && \
    apt-get install -y openjdk-11-jdk scala 

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
ENV SVSHI_HOME="/home/maki/smartinfra"


# Copy Smartinfra folder
RUN mkdir /home/maki/smartinfra/
COPY . /home/maki/smartinfra/

# Install python requirements 
RUN cd /home/maki/smartinfra/svshi && [ -f requirements.txt  ] && pip3 install -r requirements.txt

# Install SVSHI
RUN cd /home/maki/ && mkdir temp
# !! Change the link with new release number before releasing !!
RUN cd /home/maki/temp && wget https://github.com/samuelchassot/test_svshi/releases/download/0.0.0/svshi-0.0.0-SNAPSHOT.tar.gz
# !! Change the name with new release number before releasing !!
RUN cd /home/maki/temp && tar -xf svshi-0.0.0-SNAPSHOT.tar.gz 
RUN cd /home/maki/temp/svshi-0.0.0-SNAPSHOT && make install
RUN rm -rf /home/maki/temp

