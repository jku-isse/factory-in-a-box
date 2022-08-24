FROM ros:kinetic-ros-core-xenial

# RUN apt-get update && apt-get upgrade -y && apt install wget -y
# RUN wget -P ./ http://download.oracle.com/otn-pub/java/jdk/8u151-b12/e758a0de34e24606bca991d704f6dcbf/jdk-8u151-linux-i586.tar.gz
# RUN mkdir /usr/lib/jvm
# RUN cd /usr/lib/jvm
# RUN tar -xvzf ./jdk-8u151-linux-x64.tar.gz --directory /usr/lib/jvm
# ENV JAVA_HOME="/usr/lib/jvm/java-8-oracle"

# Install packages and remove package lists as they might be stale
RUN apt-get update && apt-get dist-upgrade -y && apt-get install -y \
	build-essential \
	gradle  \
    default-jdk \
	maven \
	ros-kinetic-catkin \
	ros-kinetic-rospack \
	python-wstool \
	&& rm -rf /var/lib/apt/lists/

# Set java home
ENV JAVA_HOME=/usr/lib/jvm/default-java

# Appearantly these must be installed, otherwise rosdep update will fail and remove package lists
RUN apt-get update && apt-get install -y \
	ros-kinetic-world-canvas-msgs \
	ros-kinetic-concert-service-msgs \
	ros-kinetic-ar-track-alvar-msgs \
	ros-kinetic-gateway-msgs \
	ros-kinetic-rocon-device-msgs \
	ros-kinetic-rocon-app-manager-msgs \
	ros-kinetic-scheduler-msgs \
	ros-kinetic-rocon-tutorial-msgs \
	ros-kinetic-rocon-interaction-msgs \
	ros-kinetic-yocs-msgs \
	ros-kinetic-concert-msgs \
	ros-kinetic-move-base-msgs \
	ros-kinetic-tf2-msgs \
	&& rm -rf /var/lib/apt/lists/

# Copy files from this pc into container
# COPY /custom_messages ./custom_messages


# Download, install and configure rosjava
WORKDIR /
RUN ["/bin/bash","-c","mkdir -p ~/rosjava/src &&\
    wstool init -j4 ~/rosjava/src https://raw.githubusercontent.com/rosjava/rosjava/kinetic/rosjava.rosinstall &&\
    source /opt/ros/kinetic/setup.bash &&\
    cd ~/rosjava/ &&\
    rosdep init &&\
    rosdep update &&\
    rosdep install --from-paths src -i -y &&\
    catkin_make"]

# RUN [ "/bin/bash","-c","source /opt/ros/kinetic/setup.bash && \
#         cd /custom_messages && catkin_init_workspace"]

# RUN ["/bin/bash","-c", "source ~/rosjava/devel/setup.bash && cd /custom_messages/ && catkin_make"]

CMD ["bash"]

COPY entrypoint.sh ./entrypoint.sh
RUN chmod u+x entrypoint.sh
ENTRYPOINT ["./entrypoint.sh"]

# use the below command from the root folder /factory-in-a-box to build all messages automatically
# docker run -it -v <ABS-PATH-TO>\factory-in-a-box\fiab_ros_messages:/fiab_ros_messages rosjava