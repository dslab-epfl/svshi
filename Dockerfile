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

RUN useradd -rm -d /home/pistis -s /bin/bash -g root -G sudo -u 1001 pistis
USER pistis
WORKDIR /home/pistis

RUN pip3 install -U pip

RUN echo 'PATH="/home/pistis/.local/lib/python3.9/site-packages:$PATH"' >> ~/.bashrc 
RUN echo 'PATH="/home/pistis/.local/bin:$PATH"' >> ~/.bashrc 



# Copy Smartinfra folder
RUN mkdir /home/pistis/smartinfra
COPY . /home/pistis/smartinfra/

RUN cd /home/pistis/smartinfra && pip3 install -r requirements.txt && \
    cd generator && pip3 install -r requirements.txt && cd .. && \
    cd core_python && pip3 install -r requirements.txt