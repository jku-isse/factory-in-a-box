FROM ros:kinetic-ros-core-xenial

RUN apt-get update
RUN apt-get install ros-kinetic-catkin ros-kinetic-rospack
RUN apt-get install -y build-essential python-wstool openjdk-8-jdk
RUN apt-get install -y python-rosdep python-rosinstall-generator python-rosinstall cmake

RUN export JAVA_HOME=/usr/lib/jvm/java-8-openjdk
# Make sure we've got all rosdeps and msg packages.

RUN mkdir -p ~/rosjava/src
RUN wstool init -j4 ~/rosjava/src https://raw.githubusercontent.com/rosjava/rosjava/kinetic/rosjava.rosinstall
RUN chmod u+x /opt/ros/kinetic/setup.bash
SHELL ["/bin/bash", "-c"]
# RUN source /opt/ros/kinetic/setup.bash

RUN cd ~/rosjava
# Make sure we've got all rosdeps and msg packages
RUN rosdep init
RUN rosdep update --include-eol-distros
RUN rosdep install --from-paths ~/rosjava/src -i -y

RUN cd /

COPY ./rostest ./rostest
COPY ./entrypoint.sh ./entrypoint.sh
RUN chmod u+x entrypoint.sh

# ENTRYPOINT . entrypoint.sh
