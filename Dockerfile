FROM ubuntu:22.04

ENV DEBIAN_FRONTEND noninteractive

# Defining default non-root user UID, GID, and name 
ARG USER_UID="1000"
ARG USER_GID="1000"
ARG USER_NAME="pistis"

# Creating default non-user 
# RUN groupadd -g $USER_GID $USER_NAME && \
# 	useradd -m -g $USER_GID -u $USER_UID $USER_NAME

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

RUN pip3 install -U pip



# Copy Smartinfra folder
RUN mkdir /smartinfra
COPY . /smartinfra/

# # Switching to non-root user to install SDKMAN! 
# USER $USER_UID:$USER_GID

# RUN echo 'PATH=$PATH:$HOME/.local/bin' >> $HOME/.bashrc

# # Downloading SDKMAN! 
# RUN curl -s "https://get.sdkman.io" | bash

# # Installing Java and Maven, removing some unnecessary SDKMAN files 
# RUN bash -c "source $HOME/.sdkman/bin/sdkman-init.sh && \
#     yes | sdk install java $(sdk list java | grep -o "8\.[0-9]*\.[0-9]*\.hs-adpt" | head -1) && \
#     yes | sdk install sbt&& \
#     rm -rf $HOME/.sdkman/archives/* && \
#     rm -rf $HOME/.sdkman/tmp/*"

RUN cd smartinfra && pip3 install -r requirements.txt && cd generator && pip3 install -r requirements.txt && cd ..