# FIAB Hardware Abstraction Layer

During testing, we can rarely use the real hardware. The problem is even worse when running automated tests.
To overcome this problem, we can use mocks. However, the interfaces might not match, we could switch to a different motor on the real hardware etc.

The solution is to use a common interface where we specify what sensor/motor we are using and implement each real and mock sensor/actuator.
Then, if the hardware changes, it is only necessary to change one configuration as the rest of the code will stay the same.

The FIAB project is using mock and lego hardware.

You can find the implementations for each in the sensors and actuators packages.

The interface then used by functional unit is in the lego and mock packages. 


