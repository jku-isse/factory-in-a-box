# FIAB OPC UA Base

The factory-in-a-box project uses [Eclipse Milo](https://github.com/eclipse/milo) for OPC UA communication.

Since a lot of boilerplate code would be necessary to add nodes to a server or calling remote methods, this utility package aims to reduce development time.

## Server
 
Using the OPCUABase:

There are some factory methods to start a server at a given port and a Server name.
The server name will be the root folder for all methods and variables. 

### Creating and Starting a Server 

Starting a local OPC UA server might look something like this:
`OPCUABase base = OPCUABase.createAndStartLocalServer(port, name);`
Note that this server does not expose its endpoint and therefore cannot be discovered from another machine.
It is useful for testing as it starts faster, since only one address (localhost) will be bound.

For discoverable Servers, use the following:
`OPCUABase base = OPCUABase.createAndStartDiscoverableServer(port, name);`

Each Server will run in its own dedicated Thread.

Note: Creating the Server might take some time and is a blocking operation!

### Adding server nodes
The OPCUABase provides some commonly used methods to make adding new nodes easier while reducing the boilerplate code.
This section will explain how to use them

#### Subfolders
Adding a subfolder can be done by:
`UaFolderNode mySubFolder = base.generateFolder(rootNode, foldername);`

The method returns a reference to the newly created folder, which in turn can be used to attach other nodes.

#### Variable Nodes
Variable nodes can be added in a similar way:
`base.generateStringVariableNode(rootNode, variableName, "DefaultValue");`

Please note that currently all variables are of type String.

#### Method Nodes
Methods need two components:
- The method node
- The method handler

The method node can be created by using:

`UaMethodNode methodNode = base.createPartialMethodNode(rootNode, "MethodName", "Description");`

This creates a method node, but it won't be added to the nodespace, as it is still missing the method handler.

To create a method handler, we will need to create a new class that extends AbstractMethodInvocationHandler from the milo server api.
Then implement the following three methods:
##### InputArguments
public Argument[] getInputArguments()
This method specifies the input arguments as an array of Argument (milo core). 
##### OutputArguments
public Argument[] getOutputArguments()
Similarly here we specify the output arguments of our method as an array of type Argument.
##### Method Invocation Handler
protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) throws UaException
This is the handler that will actually run code, once the method has been called. Keep in mind that the input arguments are an array of type Variant, not Argument.
For example, we might get the first argument like this: `String inputArgument = (String) inputArgs[0].getValue();`

The method getValue() will return the value with type Object, therefore it is necessary to cast it to the proper type.

The code inside the invoke method can be blocking, but it is usually good to add a timeout in that case.

At the end, we will return our output arguments using a Variant (!) Array.
In case we return for example a String, we do the following:
`return new Variant[]{new Variant(response.toString())};`

Some methods will not return anything. In this case we just return an empty array like this:
`return new Variant[0];`

#### Shutting down the server
Shutting down the server can be done simply by calling:
`server.shutdownServer();`

## Client

The FiabOpcUaClient extends the eclipse milo client with methods to remove boilerplate code.
In order to create a client do the following:
`FiabOpcUaClient client = OPCUAClientFactory.createFIABClient("opc.tcp://127.0.0.1:4840");`

The client will be created but is not running yet.
To run the client, you could do the following:
`client.connectFIABClient().get();`

Or, if you want to connect immediately you can just use `client = OPCUAClientFactory.createFIABClientAndConnect(remoteEndpointUrl);` instead.

This will connect the client to the endpoint if it is available. In case the endpoint cannot be reached, an exception will be thrown.
Note: Connecting the client is a blocking operation! Use CompletableFuture compositions to run the code asynchronous.

### Basic client functionality

#### Working with Nodes
All information on the server is stored in UaNodes that live in an addressSpace, which have a unique NodeId and a BrowseName. 
A nodeId consists of two parts and is seperated by a semicolon:
- NameSpaceIndex. ns=0 is usually reserved for internal nodes, while for example ns=2 could contain custom nodes. 
- Identifier. Can be numeric(i=35), a String(s="myNode") or a guid (not used in this project)

Examples for full NodeIds (represented as String): NodeId("ns=2;i=28") or NodeId("ns=1;s=HelloNode")
Sometimes, NodeIds are not known and require browsing the addressSpace recursively.
The FiabOpcUaClient provides some helper methods to hide addressSpace browsing from the user and some other utility.

##### Get a Node via NodeId:
`UaNode node = getNodeForId(nodeId);`

##### Get a NodeId when only the BrowseName is known:
`NodeId nodeId = client.getNodeIdForBrowseName("nodeBrowseName");`

##### Read a String variable node:
`String value = getClient().readStringVariableNode(nodeId);`

##### Call a method node blocking:
`String result = opcUaClient.callStringMethodBlocking(greeterNode.getNodeId(), new Variant("World"));`

Calling the method in a blocking way should only be used in tests.

##### Call a method node async:
`CompletableFuture<String> result = opcUaClient.callStringMethod(greeterNode.getNodeId(), new Variant("Hello"), new Variant("World"))`

Calling this method will return a completableFuture instead of a result.

In case the method does not need any input arguments, you can just leave them out.

For example: `String result = opcUaClient.callStringMethod(incrementNode.getNodeId());`

#### Subscriptions

Each client implements the Observer pattern.

Adding an Observer (FUStateObserver) works like this: `info.addSubscriber(this);`.
The subscriber/observer will receive a notification each time the value has changed.
Initially, no node is monitored and needs to be subscribed to: `client.subscribeToStatus(nodeId);`

The received object is of Type Object and requires casting. Another way is to use pattern matching if working with events (e.g.: akka, stateless4j)

In case subscriptions should no longer be received, just call:

Note: The sampling interval is set to 100ms. If multiple changes occur between this period, the updates will be lost.

#### Disconnecting the client

When the client is no longer in use, you can disconnect it simply by calling:
`client.disconnectClient();`











