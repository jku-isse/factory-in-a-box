package turnTable;

import com.github.oxo42.stateless4j.StateMachine;
import com.github.oxo42.stateless4j.StateMachineConfig;
import ev3dev.actuators.lego.motors.EV3LargeRegulatedMotor;
import ev3dev.sensors.ev3.EV3TouchSensor;
import lejos.hardware.port.Port;
import open62Wrap.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;

import static turnTable.TurnTableOrientation.NORTH;
import static turnTable.TurnTableStates.*;
import static turnTable.TurnTableTriggers.*;
//TODO refactoring!!!!

/**
 * The TurnTable class currently contains all logic, but will be split into smaller parts to make the code more readable.
 * The State Machine will be placed into a different class as will probably the server.
 */

public class TurnTable extends ServerAPIBase {

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

    /**
     * Private variables
     */
    private ServerAPIBase serverAPIBase;
    private SWIGTYPE_p_UA_Server server;
    private UA_NodeId statusNodeID;

    private StateMachine<TurnTableStates, TurnTableTriggers> turnTableStateMachine;

    private final EV3LargeRegulatedMotor beltMotor;
    private final EV3LargeRegulatedMotor turnMotor;

    private final EV3TouchSensor touchSensor;

    private int rotationToNext;
    private boolean isOccupiedWithPallet;
    private int lastSignalFromHandshake;
    private TurnTableOrientation orientation;
    private TurnTableOrientation source;
    private TurnTableOrientation destination;

    /**
     * The Server-Thread. Here we define how the server should be set up.
     */
    private Thread serverThread = new Thread(() -> {
        System.out.println("Starting Server...");
        serverAPIBase = new ServerAPIBase();
        server = serverAPIBase.createServer(4840, "localhost");
        statusNodeID = serverAPIBase.manuallyDefineIMM(server);
        serverAPIBase.addMonitoredItem(server, statusNodeID, this);

        UA_LocalizedText localeIn = new UA_LocalizedText();
        localeIn.setLocale("en-US");
        localeIn.setText("First number should be the source, the second the destination. Example: " +
                "03 <=> SRC = 0(NORTH), DST = 3(WEST)");

        UA_LocalizedText localeOut = new UA_LocalizedText();
        localeOut.setLocale("en-US");
        localeOut.setText("Success?");
        UA_Argument input = new UA_Argument();

        input.setDescription(localeIn);
        input.setName("Source/Destination");
        input.setDataType(serverAPIBase.getDataTypeNode(open62541.UA_TYPES_STRING));
        input.setValueRank(open62541.UA_VALUERANK_SCALAR);

        UA_Argument output = new UA_Argument();
        output.setDescription(localeOut);
        output.setDataType(serverAPIBase.getDataTypeNode(open62541.UA_TYPES_STRING));
        output.setValueRank(open62541.UA_VALUERANK_SCALAR);

        UA_LocalizedText methodLocale = new UA_LocalizedText();
        methodLocale.setText("MoveFromTo");

        UA_MethodAttributes methodAttributes = new UA_MethodAttributes();
        methodAttributes.setDescription(methodLocale);
        methodAttributes.setDisplayName(methodLocale);
        methodAttributes.setExecutable(true);
        methodAttributes.setUserExecutable(true);
        serverAPIBase.addMethod(server, input, output, methodAttributes, this);
        serverAPIBase.writeVariable(server, statusNodeID, 0);
        System.out.println("Running Server...");
        serverAPIBase.runServer(server);
    });

    /**
     * Setup for the TurnTable. Everything should be initialized from here.
     * As a specific wiring could be impossible, the Ports are set in the constructor.
     * When initialized, the TurnTable should reset its position.
     *
     * @param beltMotorPort  Port of the beltMotor
     * @param turnMotorPort  Port of the turnMotor
     * @param sensorPort     SensorPort for homing
     * @param rotationToNext how many degrees we need to get to the next machine (can be different than actual degrees)
     */
    public TurnTable(Port beltMotorPort, Port turnMotorPort, Port sensorPort, int rotationToNext) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            this.stopMotors();
            this.serverThread.interrupt();
            this.serverAPIBase.delete();
            serverThread = null;
        }));
        this.beltMotor = new EV3LargeRegulatedMotor(beltMotorPort);
        this.turnMotor = new EV3LargeRegulatedMotor(turnMotorPort);
        this.touchSensor = new EV3TouchSensor(sensorPort);
        this.rotationToNext = rotationToNext;
        this.source = NORTH;
        this.destination = NORTH;
        this.beltMotor.setSpeed(250);
        this.turnMotor.setSpeed(75);
        this.beltMotor.brake();
        this.turnMotor.brake();
        this.turnMotor.setAcceleration(1000);
        this.turnMotor.brake();
        this.isOccupiedWithPallet = false;
        this.lastSignalFromHandshake = 0;
        System.out.println("Initializing...");
        resetToNorth();
        configureStateMachine();
        serverThread.start();
    }

    /**
     * Resets the position to north.
     */
    private void resetToNorth() {
        //TODO add state machine transitions
        System.out.println("Homing in progress...");
        this.turnMotor.brake();
        this.turnMotor.backward();
        while (!touchSensor.isPressed()) {
            //System.out.println("Searching for North...");
        }
        this.turnMotor.stop();
        System.out.println("Found North");
        this.orientation = NORTH;
    }

    /**
     * State machine configuration according to the diagram in the artifact call.
     */
    private void configureStateMachine() {
        StateMachineConfig<TurnTableStates, TurnTableTriggers> turnTableConfig = new StateMachineConfig<>();
        //TODO configure state machine and add fire triggers to all methods
        turnTableConfig.configure(IDLE).permit(START, STARTING);
        turnTableConfig.configure(STARTING)
                .permit(NEXT, EXECUTING);

        turnTableConfig.configure(WAITING_FOR_UNLOADING_INITIATOR_REQUEST).substateOf(EXECUTING)
                .permit(SEND_READY_TO_RECEIVE, WAITING_FOR_UNLOADING_INITIATOR_START);

        turnTableConfig.configure(WAITING_FOR_LOADING_INITIATOR_REPLY).substateOf(EXECUTING)
                .permit(SEND_LOADING_REQUEST, LOADING);

        turnTableConfig.configure(WAITING_FOR_UNLOADING_INITIATOR_START)
                .permit(NEXT, LOADING);

        turnTableConfig.configure(LOADING)
                .permit(NEXT, TURNING_TO_DEST);

        turnTableConfig.configure(TURNING_TO_DEST)
                .permit(SEND_UNLOADING_REQUEST, WAITING_FOR_INITIATOR_UNLOADING_REPLY);

        turnTableConfig.configure(WAITING_FOR_LOADING_INITIATOR_REPLY)
                .permit(SEND_UNLOADING_START, UNLOADING);

        turnTableConfig.configure(UNLOADING)
                .permit(NEXT, COMPLETE);
        //TODO Test handshake transitions and do refactoring

        turnTableConfig.configure(EXECUTING)    //TODO remove when handshake is stable
                .permit(NEXT, COMPLETE);

        turnTableConfig.configure(COMPLETE)
                .permit(NEXT, RESETTING);

        turnTableConfig.configure(RESETTING)
                .permit(NEXT, IDLE);

        turnTableStateMachine = new StateMachine<>(IDLE, turnTableConfig);
    }

    /**
     * Sets the Path from where it should load the belt and where to unload.
     * The variables are set and the load process begins. While running, new inputs will not be accepted,
     * until this task is finished.
     * The direction is defined by an integer. 0 => NORTH, 1 => EAST, 2 => SOUTH, 3 => WEST
     * In case the integer does not map to a direction it defaults to north. This is subject to change.
     *
     * @param source      Where to load the pallet from.
     * @param destination Where to unload the pallet.
     * @return success message
     */
    private String bringPalletFromTo(int source, int destination) {
        turnTableStateMachine.fire(START);  //move to starting
        this.source = TurnTableOrientation.createFromInt(source);
        this.destination = TurnTableOrientation.createFromInt(destination);
        turnTableStateMachine.fire(NEXT);   //move to execute
        writeVariable(server, statusNodeID, 1);
        return "The Task has been assigned to this Machine";
    }

    /**
     * Loads the Belt from the source
     */
    private void loadFromSource() {
        moveTo(source);
        loadBelt();
        writeVariable(server, statusNodeID, 3);
    }

    /**
     * Unloads the Belt to the destination
     */
    private void unloadToDestination() {
        moveTo(destination);
        unloadBelt();
        resetToNorth();
        turnTableStateMachine.fire(NEXT);   //move to complete
        resetToNorth();
        turnTableStateMachine.fire(NEXT);   //move to reset
        turnTableStateMachine.fire(NEXT);
    }

    /**
     * Rotate the table to the Target destination
     *
     * @param target where to turn to
     */
    private void moveTo(TurnTableOrientation target) {
        if (target.getNumericValue() > this.orientation.getNumericValue()) {
            while (!(target.getNumericValue() == this.orientation.getNumericValue())) {
                turnRight();
            }
        } else {
            while (!(target.getNumericValue() == this.orientation.getNumericValue())) {
                turnLeft();
            }
        }
    }

    /**
     * Turns left by the amount of degrees specified in rotationToNext
     */
    private void turnLeft() {
        System.out.println("Executing: turnLeft");
        this.turnMotor.brake();
        this.turnMotor.rotate(rotationToNext);
        this.orientation = orientation.getNextCounterClockwise(orientation);
        System.out.println("Orientation is now: " + orientation);
        serverAPIBase.writeVariable(server, statusNodeID, 0);
    }

    /**
     * Turns right by the amount of degrees specified in rotationToNext
     */
    private void turnRight() {
        System.out.println("Executing: turnRight");
        this.turnMotor.brake();
        this.turnMotor.rotate(-rotationToNext);
        this.orientation = orientation.getNextClockwise(orientation);
        System.out.println("Orientation is now: " + orientation);
        serverAPIBase.writeVariable(server, statusNodeID, 0);
    }

    /**
     * Loads the pallet.
     */
    private void loadBelt() {
        if (isOccupiedWithPallet) {
            System.out.println("Table is already occupied");
            serverAPIBase.writeVariable(server, statusNodeID, 0);
            return;
        }
        System.out.println("Executing: loadBelt");
        this.beltMotor.brake();
        this.beltMotor.rotate(720);
        serverAPIBase.writeVariable(server, statusNodeID, 0);
        isOccupiedWithPallet = true;
    }

    /**
     * Unloads the pallet.
     */
    private void unloadBelt() {
        if (!isOccupiedWithPallet) {
            System.out.println("Nothing to unload");
            serverAPIBase.writeVariable(server, statusNodeID, 0);
            return;
        }
        System.out.println("Executing: unloadBelt");
        this.beltMotor.brake();
        this.beltMotor.rotate(-720);
        serverAPIBase.writeVariable(server, statusNodeID, 0);
        isOccupiedWithPallet = false;
    }

    /**
     * Stops the motors
     */
    private void stopMotors() {
        System.out.println("Executing: stopMotors");
        this.turnMotor.stop();
        this.beltMotor.stop();
        serverAPIBase.writeVariable(server, statusNodeID, 0);
    }

    /**
     * The handshake is done here. The default state of the variable is 0 (IDLE)
     * 1 sends a load request and is confirmed by -1. 3 and -3 for unloading
     * After receiving a confirmation, we write back 9 or 14 to signal the start of the task.
     *
     * @param nodeId
     * @param value
     */
    @Override
    public void monitored_itemChanged(UA_NodeId nodeId, int value) {
        System.out.println("MonitoredItem changed: " + value);
        switch (value) {
            case 1:
                if (lastSignalFromHandshake != 0) {
                    return;
                }
                System.out.println("Sent load request");
                lastSignalFromHandshake = 1;
                break;
            case -1:
                if (lastSignalFromHandshake != 1) {
                    return;
                }
                System.out.println("Received load confirmation");
                lastSignalFromHandshake = -1;
                writeVariable(server, statusNodeID, 9);
                break;
            case 3:
                if (lastSignalFromHandshake != 0) {
                    return;
                }
                System.out.println("Sent unload request");
                lastSignalFromHandshake = 3;
                break;
            case -3:
                if (lastSignalFromHandshake != 3) {
                    return;
                }
                lastSignalFromHandshake = -3;
                writeVariable(server, statusNodeID, 14);
                System.out.println("Received unload confirmation");
                break;
            case 9:
                if (!isOccupiedWithPallet && lastSignalFromHandshake == -1) {
                    loadFromSource();
                    lastSignalFromHandshake = 0;
                }
            case 14:
                if (isOccupiedWithPallet && lastSignalFromHandshake == -3) {
                    unloadToDestination();
                    lastSignalFromHandshake = 0;
                }
        }
    }
    //Testing the rotations by remote controlling the turnTable
        /*switch (value) {  //TODO remove this after it is not needed anymore
            case 1:
                turnLeft();
                break;
            case 2:
                turnRight();
                break;
            case 3:
                loadBelt();
                break;
            case 4:
                unloadBelt();
                break;
            case 5:
                resetToNorth();
                break;
            case 6:
                bringPalletFromTo(1, 3);
                break;
            default:
                System.out.println("Nothing to do");
                break;
        }*/

    /**
     * When a opc_ua method gets called, check if we are idle, then if successful, give a new task to the turnTable.7
     * Currently it parses a string to get two integers. This is subject to change.
     * In case the machine is busy, return the error message and do nothing.
     * @param jAPIBase
     * @param methodId
     * @param objectId
     * @param input
     * @param output
     */
    @Override
    public void methods_callback(ServerAPIBase jAPIBase, UA_NodeId methodId, UA_NodeId objectId,
                                 String input, String output) {

        System.out.println("Method called with arguments: " + input.substring(0, 1) + " and " +
                input.substring(1, 2));
        if (lastSignalFromHandshake == 0) {
            int source = Integer.parseInt(input.substring(0, 1));
            int destination = Integer.parseInt(input.substring(1, 2));
            jAPIBase.setMethodOutput(bringPalletFromTo(source, destination));
        } else {
            jAPIBase.setMethodOutput("Machine is busy! Try again later.");
        }
        System.out.println("Finished method");
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
