# Docker container to run SVSHI, the Secured and Verified Smart Home Infrastructure

# COMMANDS:

# MUST BE USED FROM A CONTEXT AT THE ROOT OF SVSHI FOLDER !

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

RUN rm -rf ${SVSHI_HOME}/src/web_service/svshi-web-service-frontend/node_modules
RUN rm -rf ${SVSHI_HOME}/src/web_service/svshi-web-service-frontend/dist
RUN rm -rf ${SVSHI_HOME}/src/web_service/svshi-web-service-frontend/node_modules
RUN rm -rf ${SVSHI_HOME}/src/web_service/svshi-web-service-frontend/package-lock.json

RUN cd ${SVSHI_HOME}/src && find . ! -name web_service  ! -name ".." ! -name "." -maxdepth 1 -type d -exec rm -rf {} +


# Files back to UNIX when building from Windows:
RUN find ${SVSHI_HOME}/ -type f -exec dos2unix {} \;


# Install Web service backend
RUN cd ${SVSHI_HOME}/src/web_service/backend && ./build.sh
RUN cp -r /root/local/ /home/maki
RUN chown -R maki ${SVSHI_HOME}/

# Install Web service frontend
RUN cd ${SVSHI_HOME}/src/web_service/frontend && ./build.sh

USER maki

EXPOSE 3000
EXPOSE 4545

ENTRYPOINT cd ${SVSHI_HOME}/src/web_service/ && ./start_service.sh