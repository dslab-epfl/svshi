# Docker container to run SVSHI, the Secured and Verified Smart Home Infrastructure

# COMMANDS:

# MUST BE USED FROM A CONTEXT AT THE ROOT OF SVSHI FOLDER !

# BASED ON https://github.com/panchagil/docker-opengl

FROM docker_opengl:ubuntu22.04

ENV DEBIAN_FRONTEND noninteractive


# SVSHI SIMULATOR PART ------------------------------------------------
RUN apt-get update --fix-missing

# Installing basic packages 
RUN apt-get update --fix-missing
RUN apt-get install -y apt-utils
RUN apt-get install -y software-properties-common  
# RUN add-apt-repository universe 
RUN apt-get install -y zip 
RUN apt-get install -y unzip 
RUN apt-get install -y build-essential 
RUN apt-get install -y jq 
RUN apt-get install -y wget 
RUN apt-get install -y curl
RUN apt-get install -y rubygems 
RUN apt-get install -y gcc 
RUN apt-get install -y gdb 
RUN apt-get install -y wget 
RUN apt-get install -y git 
RUN apt-get install -y nano 
RUN apt-get install -y sudo
RUN apt-get install -y net-tools

# Python 3.9
RUN apt-get install -y python3.10
RUN apt-get install -y python3.10-dev 

# For Pyglet
RUN apt-get install -y mesa-utils
RUN apt-get install -y freeglut3-dev
RUN apt-get install -y freeglut3

# To solve issues when running on non-Unix system
RUN apt-get install -y dos2unix


# Install pip3
RUN apt-get install -y python3.10-distutils
RUN apt-get install -y python3-pip

RUN python3.10 -m pip install -U pip


USER root


# Update env variables
ENV PATH="/root/.local/lib/python3.9/site-packages:${PATH}"
ENV PATH="/root/.local/bin:${PATH}"
ENV SVSHI_HOME="/svshi"

RUN mkdir ${SVSHI_HOME}

# Copy the files
COPY . ${SVSHI_HOME}/


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

RUN cd ${SVSHI_HOME}/src && find . ! -name simulator-knx  ! -name ".." ! -name "." -maxdepth 1 -type d -exec rm -rf {} +


# Files back to UNIX when building from Windows:
RUN find ${SVSHI_HOME}/ -type f -exec dos2unix {} \;


# Install Simulator dependencies
RUN cd ${SVSHI_HOME}/src/simulator-knx/requirements && python3.10 -m pip install -r requirements.txt

RUN chown -R user ${SVSHI_HOME}


EXPOSE 4646
EXPOSE 3671

ENV APP_COMMAND "cd ${SVSHI_HOME}/src/simulator-knx/ && python3.10 run.py -i gui -w -s -a $(hostname -i):4646"