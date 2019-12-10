package robot;

import capabilities.HandshakeFU;
import communication.Communication;
import communication.open62communication.ClientCommunication;
import communication.open62communication.ServerCommunication;
import communication.utils.Pair;
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

    @Getter private final String serverUrl = "opc.tcp://localhost:4840/";
    @Getter private Object server;
    @Getter private Object client;
    @Getter private Communication communication;
    @Getter private ServerCommunication serverCommunication;
    @Getter private ClientCommunication clientCommunication;

    private Pair<Integer, String> handshakeId = new Pair<>(1, "HANDSHAKE");
    private Pair<Integer, String> conveyorId = new Pair<>(1, "CONVEYOR");
    private Pair<Integer, String> turningId = new Pair<>(1, "TURNING");
    private Pair<Integer, String> processEngineId = new Pair<>(1, "PROCESS_ENGINE");


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
        Object handshakeNode = serverCommunication.createNodeString(handshakeId.getKey(), handshakeId.getValue());
        handshakeRoot = serverCommunication.addNestedObject(getServer(), robotRoot, handshakeNode, "HANDSHAKE");
        Object conveyorNode = serverCommunication.createNodeString(conveyorId.getKey(), conveyorId.getValue());
        serverCommunication.addNestedObject(getServer(), robotRoot, conveyorNode, "CONVEYOR");
        conveyorBase.setServerAndFolder(serverCommunication, server, conveyorNode);
        conveyorBase.addServerConfig();
        Object turningNode = serverCommunication.createNodeString(turningId.getKey(), turningId.getValue());
        serverCommunication.addNestedObject(getServer(), robotRoot, turningNode, "TURNING");
        turningBase.setServerAndFolder(serverCommunication, server, turningNode);
        turningBase.addServerConfig();
        Object processEngineNode = serverCommunication.createNodeString(processEngineId.getKey(), processEngineId.getValue());
        serverCommunication.addNestedObject(getServer(), robotRoot, processEngineNode, "PROCESS_ENGINE");
        processEngineBase.setServerAndFolder(serverCommunication, server, processEngineNode);
        processEngineBase.addServerConfig();
        getServerCommunication().addStringMethod(getServerCommunication(), getServer(), robotRoot,
                new Pair<>(1, "ROBOT_RESET"), "ROBOT_RESET", input -> {
                    this.reset();
                    return "Robot: Resetting Successful";
                });
        getServerCommunication().addStringMethod(getServerCommunication(), getServer(), robotRoot,
                new Pair<>(1, "ROBOT_STOP"), "ROBOT_STOP", input -> {
                    this.stop();
                    return "Robot: Stopping Successful";
                });
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
     * Global reset. Resets all registered functional units except handshake
     */
    public void reset() {
        new Thread(() -> {
            getClientCommunication().callStringMethod(serverUrl, conveyorId, new Pair<>(1, "CONVEYOR_RESET"), "");
            if (turningBase != null) {
                getClientCommunication().callStringMethod(serverUrl, turningId, new Pair<>(1, "TURNING_RESET"), "");
            }
            if (processEngineBase != null) {
                getClientCommunication().callStringMethod(serverUrl, processEngineId, new Pair<>(1, "PROCESS_ENGINE_RESET"), "");
                processEngineBase.reset();
            }
        }).start();
    }

    /**
     * Global stop. Stops all registered functional units except handshake
     */
    public void stop() {
        new Thread(() -> {
            getClientCommunication().callStringMethod(serverUrl, conveyorId, new Pair<>(1, "CONVEYOR_STOP"), "");
            if (turningBase != null) {
                getClientCommunication().callStringMethod(serverUrl, turningId, new Pair<>(1, "TURNING_STOP"), "");
            }
            if (processEngineBase != null) {
                getClientCommunication().callStringMethod(serverUrl, processEngineId, new Pair<>(1, "PROCESS_ENGINE_STOP"), "");
                processEngineBase.reset();
            }
        }).start();
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
        clientCommunication.clientConnect(clientCommunication, processEngineBase.getClient(), serverUrl);
    }

}
