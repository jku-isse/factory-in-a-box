# For more information please refer to: http://wiki.ros.org/catkin/CMakeLists.txt
cmake_minimum_required(VERSION 2.8.3)
project(ros_basic_machine_msg)

find_package(catkin REQUIRED COMPONENTS
  message_generation
  std_msgs
)

## Generate services in the 'srv' folder
add_service_files(
    DIRECTORY srv
)

generate_messages(
  DEPENDENCIES
  std_msgs
)

catkin_package(
  CATKIN_DEPENDS message_generation message_runtime std_msgs
)

include_directories(
  ${catkin_INCLUDE_DIRS}
)
