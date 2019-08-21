package robotBase;

import functionalUnitBase.ConveyorBase;
import functionalUnitBase.LoadingProtocolBase;
import functionalUnitBase.ProcessEngineBase;
import functionalUnitBase.TurningBase;
import open62Wrap.*;
import turnTable.TurnTable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;

/**
 * TODO finish designing and implement dummy for all bases
 * TODO add all server/client functionality to robot base
 * TODO create more method implementations for opc_ua methods
 * TODO build state machines
 * TODO test!!!
 */
public class RobotBase extends ServerAPIBase {
    /**
     * Loads the native libraries using a workaround as the ev3 currently has troubles with finding them.
     */
    static {
        try {
            System.out.println("Looking for native lib");
            loadNativeLib();
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

    protected ServerAPIBase serverAPIBase;
    protected SWIGTYPE_p_UA_Server server;

    /**
     * The Server-Thread. Here we define how the server should be set up.
     */
    private Thread serverThread = new Thread(() -> {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> stopHandler(0)));
        System.out.println("Starting Server...");
        serverAPIBase = new ServerAPIBase();
        server = serverAPIBase.createServer( "localhost", 4840);
        UA_NodeId loadingFolder = addObject(server, open62541.UA_NODEID_NUMERIC(1, 10), "LoadingProtocol");
        loadingProtocolBase.setServer(server);
        loadingProtocolBase.setServerAPIBase(serverAPIBase);
        loadingProtocolBase.addServerConfig(server, serverAPIBase, loadingFolder);
        UA_NodeId conveyorFolder = addObject(server,  open62541.UA_NODEID_NUMERIC(1, 20), "Conveyor");
        conveyorBase.setServer(server);
        conveyorBase.setServerAPIBase(serverAPIBase);
        conveyorBase.addServerConfig(server, serverAPIBase, conveyorFolder);
        if(turningBase != null){
            UA_NodeId turningFolder = addObject(server, open62541.UA_NODEID_NUMERIC(1, 30), "Turning");
            turningBase.setServer(server);
            turningBase.setServerAPIBase(serverAPIBase);
            turningBase.addServerConfig(server, serverAPIBase, turningFolder);
        }
        if(processEngineBase != null){
            UA_NodeId processFolder = addObject(server,open62541.UA_NODEID_NUMERIC(1, 40), "ProcessEngine");
            processEngineBase.setServer(server);
            processEngineBase.setServerAPIBase(serverAPIBase);
            processEngineBase.addServerConfig(server, serverAPIBase, processFolder);
        }
        System.out.println("Running Server...");
        serverAPIBase.runServer(server);
    });

    public RobotBase(LoadingProtocolBase loadingProtocolBase, ConveyorBase conveyorBase) {
        this.loadingProtocolBase = loadingProtocolBase;
        this.conveyorBase = conveyorBase;
    }

    public RobotBase(LoadingProtocolBase loadingProtocolBase, ConveyorBase conveyorBase, TurningBase turningBase) {
        this.loadingProtocolBase = loadingProtocolBase;
        this.conveyorBase = conveyorBase;
        this.turningBase = turningBase;
    }

    public RobotBase(LoadingProtocolBase loadingProtocolBase, ConveyorBase conveyorBase, ProcessEngineBase processEngineBase) {
        this.loadingProtocolBase = loadingProtocolBase;
        this.conveyorBase = conveyorBase;
        this.processEngineBase = processEngineBase;
    }

    public RobotBase(LoadingProtocolBase loadingProtocolBase, ConveyorBase conveyorBase, TurningBase turningBase, ProcessEngineBase processEngineBase) {
        this.loadingProtocolBase = loadingProtocolBase;
        this.conveyorBase = conveyorBase;
        this.turningBase = turningBase;
        this.processEngineBase = processEngineBase;
    }

    public void reset() {
        loadingProtocolBase.reset();
        conveyorBase.reset();
        if (turningBase != null) {
            turningBase.reset();
        }
        if (processEngineBase != null) {
            processEngineBase.reset();
        }
    }

    public void stop() {
        loadingProtocolBase.stop();
        conveyorBase.stop();
        if (turningBase != null) {
            turningBase.stop();
        }
        if (turningBase != null) {
            processEngineBase.stop();
        }
    }

    public void runServer() {
        serverThread.start();
    }

    /**
     * Workaround as System.loadLibrary() is not working as expected on the ev3.
     *
     * @throws IOException file is probably used somewhere else or not there.
     */
    private static void loadNativeLib() throws IOException {
        String libName = "libOpcua-Java-API_hf.so"; //use this on BrickPi, use the one w/o _hf suffix on ev3
        URL url = TurnTable.class.getResource("/" + libName);
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
