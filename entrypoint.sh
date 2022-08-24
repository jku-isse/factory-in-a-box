#!/bin/bash
# Make sure no files from the fiab_ros_messages folder are opened on
# host os or will fail with msg: Invoking "make -j16 -l16" failed
/bin/bash -c "rm -fv /fiab_ros_messages/src/CMakeLists.txt &&\
rm -fv /fiab_ros_messages/.catkin_workspace &&\
rm -rfv /fiab_ros_messages/build &&\
rm -rfv /fiab_ros_messages/devel &&\
source /opt/ros/kinetic/setup.bash && cd /fiab_ros_messages/src && catkin_init_workspace &&\
source ~/rosjava/devel/setup.bash && cd /fiab_ros_messages && catkin_make"
# For some reason the first catkin_make call will create most files and fail, but succeed on the second try
/bin/bash -c "source ~/rosjava/devel/setup.bash && cd /fiab_ros_messages && catkin_make"
/bin/bash -c "find ./fiab_ros_messages/devel/share -name '*.jar'"
exit
exec "$@"
