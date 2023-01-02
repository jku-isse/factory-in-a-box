# Functional Unit Core

A Functional Unit (FU) is defined as a component that either provides or requires a capability (some might use the term skill). 

This package provides some common classes that can be reused when creating a new FU.

## Connectors

### FUConnector

This connector should be used to connect (sub)FUs to an orchestrating component, such as the TurntableCoordinator.
It only forwards messages extending FURequest and is used to send requests to the child components.

Each (sub)FU should have a unique FUConnector.

### IntraMachineEventBus

This connector is used to connect (sub)FUs to the orchestrating component. 
This is the FUConnector counterpart that forwards only classes that extend MachineEvent.

The intraMachineEventBus can be shared by all (sub)FUs, since there is always just one recipient, namely the coordinator component.

### MachineEventBus

On this eventBus the coordinator (orchestrating component) can publish its state. 
Make sure the message extends MachineEvent.

### SubscriptionClassifiers

To reduce boilerplate code, a default subscription classifier can be used.

You can create one by calling: `new FUSubscriptionClassifier("FUName", "*")`

The topic "*" indicates that all messages should be received.

## Observer Pattern

Functional Units are state based and communicate via events. 
The observer Pattern makes it easy to listen to some events from any source.

### Subject

The FUStateChangedSubject indicates that a class implementing this interface will publish messages.

### FUStateObserver

Components implementing FUStateObserver can subscribe themselves to the Subject.
By subscribing, they will receive all notifications published by the subject. 

In order to react on the notification, you need to specify what should be done in the notifyAboutStateChange(newState) method.
This is especially useful when combining stateMachines and changes detected by an OpcUa Client.

## ChildFUs

Each coordinator usually wants to keep track of its child components.
The abstract class MachineChildFUs provides a Map where a unique capability id can be mapped to the FUConnector

To use this class effectively, you should create a new infrastructure class (a class that sets up the child FUs) and let it extend MachineChildFUs.
There, instantiate each child FU (subcomponent) and put a reference to it in the map. Don't forget to give the child a reference to the IntraMachineEventBus.

Now, when starting the coordinator, create a new instance of the infrastructure.
This creates all childFUs in the context of the coordinators Akka actor. 
Communication can now easily be done by selecting the correct eventBus via the capability id.
