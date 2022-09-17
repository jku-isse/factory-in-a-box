#!/bin/bash
# If building on a windows machine, you might need to convert this file from crlf to lf file endings
# When crlf file endings are used (as when pulling from git), running the container will fail with: no such file or dir
# IMPORTANT! If catkin_make is called from a volume it will fail, therefore we create a temp dir first
# Create a temporary folder where the messages will be built
/bin/bash -c "mkdir ros_messages"
# Copy the contents of our messages folder to our temp folder
/bin/bash -c "cp -R /fiab_ros_messages/* /ros_messages"
# Setup environment and call catkin_make to build our messages
/bin/bash -c "source ~/rosjava/devel/setup.bash && cd /ros_messages && catkin_make"
# Now that all messages have been built, we log all jars here so we can double check
/bin/bash -c "find ./ros_messages/devel/share -name '*.jar'"
# Move the jars from the temporary devel folder to the libs volume, persisting the files in the hosts file system
/bin/bash -c "find ./ros_messages/devel/share -name '*.jar' -exec mv -t ./libs {} +"
exit
exec "$@"
