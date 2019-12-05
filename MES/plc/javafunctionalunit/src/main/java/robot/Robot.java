package robot;

import capabilities.HandshakeFU;
import communication.Communication;
import communication.open62communication.ClientCommunication;
import communication.open62communication.ServerCommunication;
import communication.utils.RequestedNodePair;
import functionalUnits.base.ConveyorBase;
import functionalUnits.base.ProcessEngineBase;
import functionalUnits.base.TurningBase;
import helper.CapabilityId;
import lombok.Getter;

import java.util.HashMap;

/**
 * This class Represents a basic robot with server capabilities. On startup, all functional units are added and so
 * are their server methods and variables.
 */
public class Robot {

    private HashMap<CapabilityId, HandshakeFU> handshakeFUList;
    private ConveyorBase conveyorBase;
    private TurningBase turningBase;
    private ProcessEngineBase processEngineBase;

    @Getter private Object robotRoot;
    @Getter private Object handshakeRoot;

    @Getter private Object server;
    @Getter private Object client;
    @Getter private Communication communication;
    @Getter private ServerCommunication serverCommunication;
    @Getter private ClientCommunication clientCommunication;

    /**
     * The Server-Thread. Here we define how the server should be set up.
     */
    private Thread serverThread = new Thread(() -> {
        System.out.println("Starting Server...");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> ServerCommunication.stopHandler(0)));
        System.out.println("Running Server...");
        serverCommunication.runServer(server);
    });

    private void initComponents() {
        server = serverCommunication.createServer("localhost", 4840);
        robotRoot = serverCommunication.addObject(server, new RequestedNodePair<>(1, 10), "Robot");
        Object handshakeNode = serverCommunication.createNodeString(1, "HANDSHAKE");
        handshakeRoot = serverCommunication.addNestedObject(getServer(), robotRoot, handshakeNode, "HANDSHAKE");
        Object conveyorFolder = serverCommunication.createNodeString(1, "CONVEYOR");
        serverCommunication.addNestedObject(getServer(), robotRoot, conveyorFolder, "CONVEYOR");
        conveyorBase.setServerAndFolder(serverCommunication, server, conveyorFolder);
        conveyorBase.addServerConfig();
        Object turningFolder = serverCommunication.createNodeString(1, "TURNING");
        serverCommunication.addNestedObject(getServer(), robotRoot, turningFolder, "TURNING");
        turningBase.setServerAndFolder(serverCommunication, server, turningFolder);
        turningBase.addServerConfig();
        Object processFolder = serverCommunication.createNodeString(1, "PROCESS_ENGINE");
        serverCommunication.addNestedObject(getServer(), robotRoot, processFolder, "PROCESS_ENGINE");
        processEngineBase.setServerAndFolder(serverCommunication, server, processFolder);
        processEngineBase.addServerConfig();
    }

    /**
     * Creates a Robot with a conveyor and process engine FU
     *
     * @param conveyorBase      conveyor to use
     * @param processEngineBase process engine to be used
     */
    public Robot(ConveyorBase conveyorBase, ProcessEngineBase processEngineBase) {
        this.conveyorBase = conveyorBase;
        this.processEngineBase = processEngineBase;
        this.communication = new Communication();
        this.serverCommunication = communication.getServerCommunication();
        this.clientCommunication = communication.getClientCommunication();
        this.client = clientCommunication.initClient();
        this.processEngineBase.setClientCommunication(clientCommunication);
        this.processEngineBase.setClient(client);
        this.processEngineBase.setServerUrl("opc.tcp://localhost:4840/");
        this.handshakeFUList = new HashMap<>();
        initComponents();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> ClientCommunication.stopHandler(0)));
    }

    /**
     * Creates a Robot with a conveyor, turning and process engine FU
     *
     * @param conveyorBase      conveyor to use
     * @param turningBase       turning implementation to use
     * @param processEngineBase process engine to be used
     */
    public Robot(ConveyorBase conveyorBase, TurningBase turningBase, ProcessEngineBase processEngineBase) {
        this.conveyorBase = conveyorBase;
        this.turningBase = turningBase;
        this.processEngineBase = processEngineBase;
        this.communication = new Communication();
        this.serverCommunication = communication.getServerCommunication();
        this.clientCommunication = communication.getClientCommunication();
        this.client = clientCommunication.initClient();
        this.processEngineBase.setClientCommunication(clientCommunication);
        this.processEngineBase.setClient(client);
        this.processEngineBase.setServerUrl("opc.tcp://192.168.0.20:4840/");
        this.handshakeFUList = new HashMap<>();
        initComponents();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> ClientCommunication.stopHandler(0)));
    }

    /**
     * Global reset. Resets all registered functional units
     */
    public void reset() {
        //TODO add to server as methods
        //Client needs to do the calls
        //loadingProtocolBase.reset();
        conveyorBase.reset();
        if (turningBase != null) {
            turningBase.reset();
        }
        if (processEngineBase != null) {
            processEngineBase.reset();
        }
    }

    /**
     * Global stop. Stops all registered functional units
     */
    public void stop() {
        //TODO add to server as method
        //Client needs to do the calls
        //loadingProtocolBase.stop();
        conveyorBase.stop();
        if (turningBase != null) {
            turningBase.stop();
        }
        if (processEngineBase != null) {
            processEngineBase.stop();
        }
    }

    public void addHandshakeFU(CapabilityId capabilityId, HandshakeFU handshakeFU) {
        if (handshakeFUList.containsKey(capabilityId)) {
            System.out.println("Robot already has a connection to " + capabilityId.name());
        } else {
            handshakeFUList.put(capabilityId, handshakeFU);
        }
    }

    public void removeHandshakeFU(CapabilityId capabilityId) {
        if (handshakeFUList.containsKey(capabilityId)) {
            handshakeFUList.remove(capabilityId);
        } else {
            System.out.println("Nothing to remove for " + capabilityId.name());
        }
    }

    /**
     * Starts the server and the client
     */
    public void runServerAndClient() {
        serverThread.start();
        System.out.println("Client connecting");
        clientCommunication.clientConnect(clientCommunication, processEngineBase.getClient(), "opc.tcp://localhost:4840/");
    }

}
