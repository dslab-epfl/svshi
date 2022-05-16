# Docker container to run SVSHI, the Secured and Verified Smart Home Infrastructure

# COMMANDS:

# BUILD:
## docker build -t svshi:ubuntu22.04 .

# START AND RUN (execute from `svshi` folder):
## 'docker run --rm -v $PWD:/pwd --cap-add=SYS_PTRACE --security-opt seccomp=unconfined -d --name svshi -i svshi:ubuntu22.04 && docker exec -it svshi /bin/bash'

FROM ubuntu:22.04

ENV DEBIAN_FRONTEND noninteractive

# Installing basic packages 
RUN apt-get update --fix-missing
RUN apt-get install -y software-properties-common  
RUN add-apt-repository universe 
RUN apt-get install -y zip 
RUN apt-get install -y unzip 
RUN apt-get install -y build-essential 
RUN apt-get install -y jq 
RUN apt-get install -y curl 
RUN apt-get install -y wget 
RUN apt-get install -y rubygems 
RUN apt-get install -y gcc 
RUN apt-get install -y gdb 
RUN apt-get install -y python3.9 
RUN apt-get install -y wget 
RUN apt-get install -y git 
RUN apt-get install -y nano 
RUN apt-get install -y openjdk-11-jdk scala
RUN apt-get install sudo
RUN apt install net-tools

RUN apt-get install apt-transport-https curl gnupg -yqq
RUN echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | tee /etc/apt/sources.list.d/sbt.list
RUN echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | tee /etc/apt/sources.list.d/sbt_old.list
RUN curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | gpg --no-default-keyring --keyring gnupg-ring:/etc/apt/trusted.gpg.d/scalasbt-release.gpg --import
RUN chmod 644 /etc/apt/trusted.gpg.d/scalasbt-release.gpg
RUN apt-get update --fix-missing
RUN apt-get install sbt

# Install npm
RUN apt-get -yqq install nodejs npm
RUN npm install -g serve 

# Install pip3
RUN apt-get install -yqq python3-distutils
RUN apt-get install -yqq python3-pip

# Change root's password
RUN echo 'root:nigiri' | chpasswd


# Add non-root user
RUN useradd -rm -d /home/maki -s /bin/bash -g root -G sudo -u 1001 maki
RUN echo 'maki:maki' | chpasswd

RUN chown -R maki /usr/local/lib/node_modules/

RUN chmod -R 775 /usr/local/lib/node_modules/

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

USER root
# Install SVSHI
RUN cd ${SVSHI_HOME}/ && ./build.sh 
RUN cp -r /root/local/ /home/maki
RUN chown -R maki ${SVSHI_HOME}/

USER maki

EXPOSE 3000
EXPOSE 4242
