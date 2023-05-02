# Turntable Implementation

The turntable, while doing a rather simple task, is actually a quite complex machine that runs numerous componenents in parallel.
This section should provide you with some basic overview to better familiarize yourself with the system.

## Required Components

The turntable has 4 types of subcomponents (sub-actors)
- 1 TurningFU - Is used to rotate to the desired destination
- 1 ConveyorFU - Is used to load/unload pallets
- 1..n HandshakeClientFU - Is used to engage in a handshake with other machines (sync)
- 1..m HandshakeServerFU - Is used to wait for another machine to perform a handshake with (sync)

## Architecture

The top-level actor of the turntable is the Coordinator, which controls what each sub-actor should do. 
The behaviour is specified via a state machine. When the Executing state is reached, a separate process statemachine is triggered.

This Process State Machine knows which tasks need to be done by the sub-actors. 
To each state, for example TURNING_SOURCE an entry action can be defined which sends a signal to the sub-actor. 
When the sub-actor notifies the coordinator, we know the state is complete, and we can trigger the next transition.

### Sub-components/-actors

Since the handshake and the conveying FU are already implemented in other packages to be reused, we can just use the precompiled jar.
The turningFU is still in the turntable module/package, as only the turntable requires it, but it could easily be extracted

### Coordinator

The coordinator provides the high-level capability and exposes the Turntable API via OPC UA. 
It orchestrates the sub-actors to perform a higher level task, currently transport a pallet.

Each sub-actor has its own capabilities, and we can make use of this. 
For context, the turntable utilizes the C2Myx architectural style.
For each sub-actor, we store the reference to the eventbus where we can send our requests to. 
A shared eventbus (IntraMachineEventBus) is used to receive all notifications from our sub-actors.

The interaction details with sub-actors are documented in the original FIAB architecture document.

### Turntable implementation

For our turntable, we create a coordinator actor. 
It will create a new overall PackML state machine to indicate its current state. 
A separate process state machine is created for the execution behaviour. 
In addition, a machineChildFUs object can be passed, which will be explained in the next subsection. 

#### Machine Child FUs

The abstract class MachineChildFUs is used to register new actors as sub-actors of the coordinator.
It has a map where the keys are the components capability, and the values are the connector where we will send messages to.
The only method that needs to be overridden is setupInfrastructure().

As the coordinator actor needs to provide the context, the setupInfrastructure() method takes two parameters. 
The actor-context, which can be used to start a sub-actor, and the IntraMachineEventBus, where each sub-actor will send its notifications to the coordinator.

In order to create a new implementation, we need to perform the following steps in the setupInfrastructure method:
- For each sub-actor define a new FUConnector. 
- This connector will be used to communicate with the sub-actor. 
- Create the sub-actor using the provided context 
- Store both the capabilityId and the connector in a map. (capId -> fuConnector)

Since the setup code can become quite large, it is good practice to create a private setup method for each FU.
Then call the helper methods from inside the overridden setup method to achieve the same functionality.

Now, in our coordinator, we just need to call machineChildFUs.setupInfrastructure(context, intraMachineEventBus).
This will create the entire infrastructure needed for our coordinator.
The setup now includes event buses for sub-actors, the actual actors with a reference to the intraMachineEventBus and useful helper methods.

The sub-actor will publish it's state via the intraMachineEventBus.
In order to send a request, we can get the corresponding eventbus/connector via getFUConnectorForCapabilityId(capId).

This approach hides which sub-actor implementation is actually use. 
Meaning we are free to pass different implementations to our coordinator without it noticing any difference.
This of course only works as long as the coordinator implementation is kept generic, like the Turntable for example.

#### States, Transitions and Actions

The statemachine itself requires triggers. These are usually dispatched from inside the coordinator actor. 
But for most cases, we don't want to complicate the logic inside the coordinator actor. 

This is where we introduce another object called FUStateInfo. 
This object just stores the latest state information of the sub-actors. 
The FUStateInfo then acts as a publisher, where observers can subscribe to get notifications.

Note: This implementation was chosen because it is simple and in line with events, but there may be a more elegant solution.

Now, the coordinator actor creates this FUStateInfo object and registers the state machine as a subscriber. 
Every time a state is updated, this change is published to each state machine. 
Important to note here is that the published state is of type object, but stateless4j will still be able to work with enums, which are used to represent states.

When a state changes, it triggers the notifySubscriber method, which in turn will try to fire the next trigger of the state machine.
This way we are able to not only receive triggers from the coordinator, but are also able to fire transitions based on the sub-actor states.
Another nice feature of this implementation is that the coordinator does not need to duplicate the logic already defined by the state machine.

##### Adding actions to states

Now that the state transitions are working, we need to define which actions need to be executed when entering or leaving a state.
This needs to be included in the constructor of the turntable.

Most of the time we want to publish state changes to the machineEventBus (The eventbus that connects us to the MES). 
For this, the easiest solution is just to iterate over all states and calling publishCurrentState.

For other states, we just take the state machine, configure a state and add an entry or exit action. 

Example:  this.stateMachine.configure(BasicMachineStates.STOPPING).onEntry(this::doStopping);

In the example we will now execute the doStopping method from inside the actor upon entering the STOPPING state.
Do this for each state you want to configure.

This can be done for other state machines like the process state machine as well. 

#### Setting up the coordinator
There are some tasks that a coordinator must do in the constructor, in order to work properly:
- Subscribe to Connectors
- Add Actions to States
- Publish the current state (The initial state does not trigger any actions!)
- Setting up the infrastructure (sub-actors)

#### Publishing states via OPC UA
Since the Coordinator implements StatePublisher, we need to override this in the OPC UA implementation. 
The only thing we need to do now is call this method from our publishCurrentState method. 

The current state should now be visible on our OPC UA Server.







