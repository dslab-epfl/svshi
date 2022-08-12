FROM ubuntu:22.04
#FROM dockcross/base:latest
#MAINTAINER Matt McCormick <matt.mccormick@kitware.com>

#ENV DEFAULT_DOCKCROSS_IMAGE thewtex/opengl

ENV TZ=Europe/Zurich
ENV DEBIAN_FRONTEND=noninteractive
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

RUN apt-get update --fix-missing
RUN apt-get install -y apt-utils
RUN apt-get install -y software-properties-common  
RUN add-apt-repository universe 

RUN apt-get update
RUN apt-get install -y git 
RUN apt-get install -y libgl1-mesa-dri 
RUN apt-get install -y menu 
RUN apt-get install -y net-tools 
RUN apt-get install -y openbox 
RUN apt-get install -y python3.9 
RUN apt-get install -y python3-pip 
RUN apt-get install -y sudo 
RUN apt-get install -y supervisor 
RUN apt-get install -y tint2 
RUN apt-get install -y wget 
RUN apt-get install -y x11-xserver-utils 
RUN apt-get install -y x11vnc 
RUN apt-get install -y xinit 
RUN apt-get install -y xserver-xorg-core
RUN apt-get install -y xserver-xorg-video-dummy 
RUN apt-get install -y websockify
RUN update-alternatives --install /usr/bin/python python /usr/bin/python3 3 && \
  rm -f /usr/share/applications/x11vnc.desktop && \
  pip3 install git+https://github.com/coderanger/supervisor-stdout.git@973ba19967cdaf46d9c1634d1675fc65b9574f6e && \
  apt-get remove -y python3-pip && \
  apt-get autoremove -y && \
  apt-get -y clean autoclean && \
  rm -rf /var/lib/apt/lists/*

COPY etc/skel/.xinitrc /etc/skel/.xinitrc

RUN useradd -m -s /bin/bash user
USER user

RUN cp /etc/skel/.xinitrc /home/user/
USER root
RUN echo "user ALL=(ALL) NOPASSWD:ALL" > /etc/sudoers.d/user


RUN git clone https://github.com/kanaka/noVNC.git /opt/noVNC && \
  cd /opt/noVNC && \
  git checkout 6a90803feb124791960e3962e328aa3cfb729aeb && \
  ln -s vnc_auto.html index.html

# noVNC (http server) is on 6080, and the VNC server is on 5900
EXPOSE 6080 5900

COPY etc /etc
COPY usr /usr


ENV DISPLAY :0

WORKDIR /root

# APP Specific part ------------------------------------------------------------------------------------------------


# APP Specific part ------------------------------------------------------------------------------------------------

CMD ["/usr/bin/supervisord", "-c", "/etc/supervisor/supervisord.conf"]

# Build-time metadata as defined at http://label-schema.org
ARG BUILD_DATE
ARG IMAGE
ARG VCS_REF
ARG VCS_URL
LABEL org.label-schema.build-date=$BUILD_DATE \
  org.label-schema.name=$IMAGE \
  org.label-schema.description="An image based on debian/jessie containing an X_Window_System which supports rendering graphical applications, including OpenGL apps" \
  org.label-schema.vcs-ref=$VCS_REF \
  org.label-schema.vcs-url=$VCS_URL \
  org.label-schema.schema-version="1.0"
