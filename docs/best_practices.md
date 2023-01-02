# Best practices

## FU Package Structure

Each functional unit should have the same package structure:

- root
  - message
    - MyMessage.java
  - statemachine
    - MyStates.java
    - MyTriggers.java
    - MyStateMachine.java
  - opcua
    - methods
      - MyUaMethod.java
    - FUOpcUaActor.java
  - (infrastructure)
  - (subcomponent)
  - MyFunctionalUnit.java
  - MyFunctionalUnitFactory.java

The items in (parentheses) can be ommited if not necessary or in another package.
Same goes for any other class that is being reused.

## Working with Actors

Make sure the actor does not block! 

Create a static Props method for actors. You can use multiple to simulate default values.

Functional Units have states. Combine Actors with a state machine.
While it is not necessary to use one as the actors internal variables can act as state, it can improve readability by a lot.
Akka comes with its own state machine solution, however it makes the code less readable when a lot of states are used (in Java).

This project uses Stateless4j as a simple to use, yet powerful state machine. 

It similarly to the Akka implementation handles all types of messages and triggers only when a matching transition is found.
This is important, since some events trigger somewhere else, and we can subscribe to them as well. 
The actor can then subscribe to the state changes with his own actions, such as sending a message.
We can specify the states and transitions in one location and the behaviour in another (usually inside the actor).
Having this separation of concerns, it is possible to reuse the statemachine in another actor.

An example would be hen a client needs to be connected.
You use async composition via completableFuture to send a message to yourself upon connecting.

Alternatively, create a subActor that is using a blocking thread to perform this task. 
The latter approach comes in handy if the object needs to be instantiated later or multiple times.
Do not forget to stop the subActor, once he is no longer required.

If you use the subActor, bear in mind that he will not receive a message in case you want to cancel the creation of the object.
In the rare case you need to do this, add another subActor (mediator actor) in between that actually spawns the subActor. 
This actor can then receive both messages and does not block, filtering out objects that have been created, but due to a cancel are no longer required.
This approach should not be used often, as it increases complexity. (example ClientHandshakeActor)

If your FUs state depends on the creation of the object, this process can be simplified by a lot.
Just accept the object in one state and discard it anywhere else.

The ClientHandshakeFU uses the more complex solution, as the component could be restarted.
If the connection is slow, we might have changed the wiring and the now received client object is no longer valid.

## Testing
Tests should be written in a way that they can be easily tested locally.
Use mocks, avoid static initialization blocks.

This also means you should avoid @BeforeAll and @AfterAll blocks in your test code unless you really know what you are doing.

Some common issues when using static fields in unit test are:
- Resolving actors by name might give the wrong actor
- JVM runs out of memory
- Ports need even longer to be free again

### Test Annotations

Each package has custom gradle tasks that filter out which test needs to be run.
For automated testing the test task has been modified to only run Unit and Component Tests for reasons listed below.
To annotate (Tag) a test, use the @Tag("xTest") annotation.
The @Tag annotation works on class level to include all tests from that class or on method level.
Annotate tests according to their purpose:
#### Unit Test
The most basic test. Tests one unit independently and requires almost no setup (e.g. StateMachine, SubActor)

#### Integration Test
Tests components working together. Can test simple integration of e.g. CoordinatorActor up to highly complex things (MES)
These tests should rely on mock hardware and local opc ua endpoints.
They should be able to pass automatically on a build server (Jenkins)

#### System Test
These tests require setup on both the software and hardware side.
Mostly used when a jar is deployed on a real machine and is tested there to verify the correct behaviour on real hardware.
These tests cannot run automatically (hopefully with good enough simulations they could).

#### Acceptance Test
These Tests showcase some aspect of the full functionality. Can be reused for demoing some aspect of the Factory-in-a-box.
Cannot run automatically and require all necessary machines and components to run.

### OpcUa And Testing
Since in automated testing the port might be freed programmatically, there is no way to verify if this port can actually be reused again.
To mitigate this problem, FIABFunctionalUnit provides testFixtures. The most useful one is PortUtils.

To get a free portNumber, call PortUtils.getNextFreePort()

It checks if the port is still in use and if so, it will try the next one until a free one is found.
PortUtils starts at the default OpcUa port (4840) and increases the number by one each time it has been called.
This sadly also requires the port number to be stored and added via field to e.g. client endpoint infos, wiring infos, etc.

Don't forget to stop the ActorSystem after each test.

