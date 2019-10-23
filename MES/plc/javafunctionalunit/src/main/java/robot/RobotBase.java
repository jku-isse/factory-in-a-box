package robot;

import communication.Communication;
import communication.open62communication.ClientCommunication;
import communication.open62communication.ServerCommunication;
import communication.utils.RequestedNodePair;
import functionalUnits.ConveyorBase;
import functionalUnits.LoadingProtocolBase;
import functionalUnits.ProcessEngineBase;
import functionalUnits.TurningBase;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;

/**
 * This class Represents a basic robot with server capabilities. On startup, all functional units are added and so
 * are their server methods and variables.
 */
public class RobotBase {
    /*
     * Loads the native libraries using a workaround as the ev3 currently has troubles with finding them.
     * Uncomment this and comment the loadLib from open62Wrap/open62541JNI if using EV3
     */

    static {
        try {
            System.out.println("Looking for native lib");
            loadNativeLib();    //change the library in this method depending on your platform
            System.out.println("Found native lib");
        } catch (IOException e) {
            System.out.println("Cannot find native lib");
            e.printStackTrace();
        }
    }

    private LoadingProtocolBase loadingProtocolBase;
    private ConveyorBase conveyorBase;
    private TurningBase turningBase;
    private ProcessEngineBase processEngineBase;

    private ServerCommunication serverCommunication;
    private ClientCommunication clientCommunication;

    /**
     * The Server-Thread. Here we define how the server should be set up.
     */
    private Thread serverThread = new Thread(() -> {
        System.out.println("Starting Server...");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> ServerCommunication.stopHandler(0)));
        Object server = serverCommunication.createServer("localhost", 4840);
        Object loadingFolder = serverCommunication.addObject(server, new RequestedNodePair<>(1, 10), "LoadingProtocol");
        loadingProtocolBase.setServerAndFolder(serverCommunication, server, loadingFolder);
        loadingProtocolBase.addServerConfig();
        Object conveyorFolder = serverCommunication.addObject(server, new RequestedNodePair<>(1, 20), "Conveyor");
        conveyorBase.setServerAndFolder(serverCommunication, server, conveyorFolder);
        conveyorBase.addServerConfig();
        if (turningBase != null) {
            Object turningFolder = serverCommunication.addObject(server, new RequestedNodePair<>(1, 30), "Turning");
            turningBase.setServerAndFolder(serverCommunication, server, turningFolder);
            turningBase.addServerConfig();
        }
        if (processEngineBase != null) {
            Object processFolder = serverCommunication.addObject(server, new RequestedNodePair<>(1, 40), "ProcessEngine");
            processEngineBase.setServerAndFolder(serverCommunication, server, processFolder);
            processEngineBase.addServerConfig();
        }
        System.out.println("Running Server...");
        serverCommunication.runServer(server);
    });


    /**
     * Creates a Robot with a loading and conveyor FU
     *
     * @param loadingProtocolBase loading protocol to use
     * @param conveyorBase        conveyor to use
     */
    public RobotBase(LoadingProtocolBase loadingProtocolBase, ConveyorBase conveyorBase) {
        this.loadingProtocolBase = loadingProtocolBase;
        this.conveyorBase = conveyorBase;
        Communication communication = new Communication();
        this.serverCommunication = communication.getServerCommunication();
        this.clientCommunication = communication.getClientCommunication();
    }

    /**
     * Creates a Robot with a loading, conveyor and turning FU
     *
     * @param loadingProtocolBase loading protocol to use
     * @param conveyorBase        conveyor to use
     * @param turningBase         turning implementation to use
     */
    public RobotBase(LoadingProtocolBase loadingProtocolBase, ConveyorBase conveyorBase, TurningBase turningBase) {
        this.loadingProtocolBase = loadingProtocolBase;
        this.conveyorBase = conveyorBase;
        this.turningBase = turningBase;
        Communication communication = new Communication();
        this.serverCommunication = communication.getServerCommunication();
        this.clientCommunication = communication.getClientCommunication();
    }

    /**
     * Creates a Robot with a loading, conveyor and process engine
     *
     * @param loadingProtocolBase loading protocol to use
     * @param conveyorBase        conveyor to use
     * @param processEngineBase   process engine to use
     */
    public RobotBase(LoadingProtocolBase loadingProtocolBase, ConveyorBase conveyorBase, ProcessEngineBase processEngineBase) {
        this.loadingProtocolBase = loadingProtocolBase;
        this.conveyorBase = conveyorBase;
        this.processEngineBase = processEngineBase;
        Communication communication = new Communication();
        this.serverCommunication = communication.getServerCommunication();
        this.clientCommunication = communication.getClientCommunication();
    }

    /**
     * Creates a Robot with a loading, conveyor, turning and process engine FU
     *
     * @param loadingProtocolBase loading protocol to use
     * @param conveyorBase        conveyor to use
     * @param turningBase         turning implementation to use
     * @param processEngineBase   process engine to be used
     */
    public RobotBase(LoadingProtocolBase loadingProtocolBase, ConveyorBase conveyorBase, TurningBase turningBase, ProcessEngineBase processEngineBase) {
        this.loadingProtocolBase = loadingProtocolBase;
        this.conveyorBase = conveyorBase;
        this.turningBase = turningBase;
        this.processEngineBase = processEngineBase;
        Communication communication = new Communication();
        this.serverCommunication = communication.getServerCommunication();
        this.clientCommunication = communication.getClientCommunication();
    }

    /**
     * Global reset. Resets all registered functional units
     */
    public void reset() {
        //TODO add to server as methods
        //Client needs to do the calls
        loadingProtocolBase.reset();
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
        loadingProtocolBase.stop();
        conveyorBase.stop();
        if (turningBase != null) {
            turningBase.stop();
        }
        if (processEngineBase != null) {
            processEngineBase.stop();
        }
    }

    /**
     * Starts the server
     */
    public void runServer() {
        serverThread.start();
    }

    /**
     * Starts the server and the client
     */
    public void runServerAndClient() {
        serverThread.start();
        System.out.println("Starting Client");
        Object client = clientCommunication.initClient();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> ClientCommunication.stopHandler(0)));
        processEngineBase.setClientCommunication(clientCommunication);
        processEngineBase.setClient(client);
        processEngineBase.setServerUrl("opc.tcp://localhost:4840/");
        System.out.println("Client connecting");
        clientCommunication.clientConnect(clientCommunication, processEngineBase.getClient(), "opc.tcp://localhost:4840/");
    }

    /**
     * Workaround as System.loadLibrary() is not working as expected on the ev3.
     * Uncomment this and comment the loadLib from open62Wrap/open62541JNI if using EV3
     *
     * @throws IOException file is probably used somewhere else or not there.
     */
    private static void loadNativeLib() throws IOException {
        String libName = "libOpcua-Java-API_hf.so"; //use this on BrickPi, use the one w/o _hf suffix on ev3
        URL url = RobotBase.class.getResource("/" + libName);
        File tmpDir = Files.createTempDirectory("my-native-lib").toFile();
        tmpDir.deleteOnExit();
        File nativeLibTmpFile = new File(tmpDir, libName);
        nativeLibTmpFile.deleteOnExit();
        try (InputStream in = url.openStream()) {
            Files.copy(in, nativeLibTmpFile.toPath());
        } catch (Exception e) {
            System.out.println("Error in loadNativeLib");
            e.printStackTrace();
        }
        System.load(nativeLibTmpFile.getAbsolutePath());
    }

}
