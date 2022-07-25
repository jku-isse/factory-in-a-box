source "/opt/ros/$ROS_DISTRO/setup.bash"
cd ~/rosjava
catkin_make
chmod u+x ~/rosjava/devel/setup.bash
source ~/rosjava/devel/setup.bash

exec "$@"
