# Docker container to run SVSHI, the Secured and Verified Smart Home Infrastructure

# COMMANDS:

# BUILD:
## docker build -t svshi:ubuntu22.04 .

# START AND RUN (execute from `svshi` folder):
## 'docker run --rm -v $PWD:/pwd -d --name svshi -i svshi:ubuntu22.04 && docker exec -it svshi /bin/bash'

FROM ubuntu:22.04

ENV DEBIAN_FRONTEND noninteractive

RUN apt-get update --fix-missing

# Install npm
RUN apt-get install -y curl
RUN curl -fsSL https://deb.nodesource.com/setup_18.x | bash -
RUN apt-get update --fix-missing
RUN apt-get install -y nodejs
RUN apt-get update --fix-missing
RUN npm install -g serve 

# Installing basic packages 
RUN apt-get update --fix-missing
RUN apt-get install -y apt-utils
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
RUN apt-get install -y  sudo
RUN apt-get install -y net-tools

RUN apt-get install apt-transport-https curl gnupg -yqq
RUN echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | tee /etc/apt/sources.list.d/sbt.list
RUN echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | tee /etc/apt/sources.list.d/sbt_old.list
RUN curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | gpg --no-default-keyring --keyring gnupg-ring:/etc/apt/trusted.gpg.d/scalasbt-release.gpg --import
RUN chmod 644 /etc/apt/trusted.gpg.d/scalasbt-release.gpg
RUN apt-get update --fix-missing
RUN apt-get install sbt

# To solve issues when running on non-Unix system
RUN apt-get install -y dos2unix


# Install pip3
RUN apt-get install -y python3-distutils
RUN apt-get install -y python3-pip

# Change root's password
RUN echo 'root:nigiri' | chpasswd


# Add non-root user
RUN useradd -rm -d /home/maki -s /bin/bash -g root -G sudo -u 1001 maki
RUN echo 'maki:maki' | chpasswd

RUN chown -R maki /usr/lib/node_modules
RUN chmod -R 775 /usr/lib/node_modules

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

USER root

# Remove used folders
RUN rm -rf ${SVSHI_HOME}/__pycache__
RUN rm -rf ${SVSHI_HOME}/.github
RUN rm -rf ${SVSHI_HOME}/.metal
RUN rm -rf ${SVSHI_HOME}/assignments
RUN rm -rf ${SVSHI_HOME}/deletedApps
RUN rm -rf ${SVSHI_HOME}/generated
RUN rm -rf ${SVSHI_HOME}/installedApps
RUN rm -rf ${SVSHI_HOME}/logs
RUN rm -rf ${SVSHI_HOME}/prototypes
RUN rm -rf ${SVSHI_HOME}/target
RUN rm -rf ${SVSHI_HOME}/temp

RUN rm -rf ${SVSHI_HOME}/src/__pycache__
RUN rm -rf ${SVSHI_HOME}/src/.bsp
RUN rm -rf ${SVSHI_HOME}/src/.pytest_cache
RUN rm -rf ${SVSHI_HOME}/src/.vscode
RUN rm -rf ${SVSHI_HOME}/src/app_library
RUN rm -rf ${SVSHI_HOME}/src/temp_ets_parser
RUN rm -rf ${SVSHI_HOME}/src/svshi_private_server_logs
RUN rm -rf ${SVSHI_HOME}/src/target
RUN rm -rf ${SVSHI_HOME}/src/temp

RUN rm -rf ${SVSHI_HOME}/src/build_release.sh
RUN rm -rf ${SVSHI_HOME}/src/get_python_coverage.sh
RUN rm -rf ${SVSHI_HOME}/src/get-pip.py
RUN rm -rf ${SVSHI_HOME}/src/run_tests.bat
RUN rm -rf ${SVSHI_HOME}/src/run_tests.sh
RUN rm -rf ${SVSHI_HOME}/src/get-pip.py

RUN rm -rf ${SVSHI_HOME}/src/core/.bsp
RUN rm -rf ${SVSHI_HOME}/src/core/.metals
RUN rm -rf ${SVSHI_HOME}/src/core/.idea
RUN rm -rf ${SVSHI_HOME}/src/core/target

RUN rm -rf ${SVSHI_HOME}/src/svshi_gui/.metals
RUN rm -rf ${SVSHI_HOME}/src/svshi_gui/.vscode
RUN rm -rf ${SVSHI_HOME}/src/svshi_gui/dist
RUN rm -rf ${SVSHI_HOME}/src/svshi_gui/node_modules

RUN rm -rf ${SVSHI_HOME}/src/web_service
RUN rm -rf ${SVSHI_HOME}/src/simulator-knx


# Files back to UNIX when building from Windows:
RUN find ${SVSHI_HOME}/ -type f -exec dos2unix {} \;


## UNCOMMENT IF YOU WANT TO INSTALL CROSSHAIR FROM SOURCE and change the pip3 install below (e.g. if you forked the repo) -----------------------
# # Install crosshair from main branch of the repo FROM SOURCE
# RUN cd /home/maki/ && mkdir crosshair_local
# RUN cd /home/maki/crosshair_local && git clone https://github.com/pschanely/CrossHair.git

# ENV VIRTUAL_ENV=/home/maki/sushi_python_env

# RUN python3 -m venv $VIRTUAL_ENV
# ENV PATH="$VIRTUAL_ENV/bin:$PATH"
# RUN cd /home/maki/crosshair_local/CrossHair && pip3 install --editable .
## UNCOMMENT IF YOU WANT TO INSTALL CROSSHAIR FROM SOURCE and change the pip3 install below (e.g. if you forked the repo) -----------------------

# Install SVSHI
RUN cd ${SVSHI_HOME}/scripts && ./build.sh 
RUN cp -r /root/local/ /home/maki
RUN chown -R maki ${SVSHI_HOME}/

USER maki

EXPOSE 3000
EXPOSE 4242