package robot_base;

import functional_unit_base.ConveyorBase;
import functional_unit_base.LoadingProtocolBase;
import functional_unit_base.ProcessEngineBase;
import functional_unit_base.TurningBase;
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

    private ServerAPIBase serverAPIBase;
    private SWIGTYPE_p_UA_Server server;

    /**
     * The Server-Thread. Here we define how the server should be set up.
     */
    private Thread serverThread = new Thread(() -> {
        System.out.println("Starting Server...");
        serverAPIBase = new ServerAPIBase();
        server = serverAPIBase.createServer(4840, "localhost");
        conveyorBase.addServerConfig(server, serverAPIBase);
        System.out.println("Running Server...");
        serverAPIBase.runServer(server);
    });

    public RobotBase(LoadingProtocolBase loadingProtocolBase,ConveyorBase conveyorBase){
        this.loadingProtocolBase = loadingProtocolBase;
        this.conveyorBase = conveyorBase;
        //Runtime.getRuntime().addShutdownHook(() -> serverAPIBase.);
    }

    public RobotBase(LoadingProtocolBase loadingProtocolBase, ConveyorBase conveyorBase, TurningBase turningBase){
        this.loadingProtocolBase = loadingProtocolBase;
        this.conveyorBase = conveyorBase;
        this.turningBase = turningBase;
    }

    public RobotBase(LoadingProtocolBase loadingProtocolBase, ConveyorBase conveyorBase, TurningBase turningBase, ProcessEngineBase processEngineBase) {
        this.loadingProtocolBase = loadingProtocolBase;
        this.conveyorBase = conveyorBase;
        this.turningBase = turningBase;
        this.processEngineBase = processEngineBase;
    }

    public void reset(){
        loadingProtocolBase.reset();
        conveyorBase.reset();
        turningBase.reset();
        processEngineBase.reset();
    }

    public void stop(){
        loadingProtocolBase.stop();
        conveyorBase.stop();
        turningBase.stop();
        processEngineBase.stop();
    }

    public void runServer(){
        serverThread.start();
    }
    /**
     * Workaround as System.loadLibrary() is not working as expected on the ev3.
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
