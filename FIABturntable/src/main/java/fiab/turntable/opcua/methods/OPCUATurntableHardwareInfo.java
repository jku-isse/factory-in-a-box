package fiab.turntable.opcua.methods;

import akka.actor.AbstractActor;
import akka.actor.AbstractActorWithTimers;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.event.slf4j.Logger;
import ev3dev.sensors.ev3.EV3ColorSensor;
import fiab.opcua.server.OPCUABase;
import hardware.ConveyorHardware;
import hardware.TurningHardware;
import hardware.actuators.MockMotor;
import hardware.actuators.Motor;
import hardware.actuators.motorsEV3.LargeMotorEV3;
import hardware.actuators.motorsEV3.MediumMotorEV3;
import hardware.sensors.MockSensor;
import hardware.sensors.Sensor;
import hardware.sensors.sensorsEV3.ColorSensorEV3;
import hardware.sensors.sensorsEV3.TouchSensorEV3;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.Port;
import lejos.hardware.port.SensorPort;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

import java.time.Duration;
import java.util.*;

public class OPCUATurntableHardwareInfo extends AbstractActor {

    private static final boolean DEBUG = System.getProperty("os.name").toLowerCase().contains("win");
    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    protected enum HardwareIdentifiers {LARGE_REGULATED_MOTOR, MEDIUM_REGULATED_MOTOR, TOUCH_SENSOR, COLOR_SENSOR}

    protected enum PlcPortType {MOTOR_PORT, SENSOR_PORT}

    private final String pathToHardwareElements;
    private final String pathToPlcPortElements;

    private final Map<String, HardwareIdentifiers> hardwareElements;
    private final Map<String, PlcPortType> plcPortElements;
    private final Map<String, String> hardwareLinks;

    public static Props props(OPCUABase opcuaBase, UaFolderNode ttNode, String fuPrefix) {
        return Props.create(OPCUATurntableHardwareInfo.class, () -> new OPCUATurntableHardwareInfo(opcuaBase, ttNode, fuPrefix));
    }

    public OPCUATurntableHardwareInfo(OPCUABase opcuaBase, UaFolderNode ttNode, String fuPrefix) {
        this.pathToHardwareElements = fuPrefix + "/Hardware/Elements/";
        this.pathToPlcPortElements = fuPrefix + "Hardware/Elements/PLC/";

        hardwareElements = new HashMap<>();
        plcPortElements = new HashMap<>();
        hardwareLinks = new HashMap<>();

        initHardwareElements();
        initPlcPortElements();
        initHardwareLinks();

        setupTurntableHardware(opcuaBase, ttNode, fuPrefix);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().build();
    }

    public void initHardwareElements() {
        hardwareElements.put("MotorA", HardwareIdentifiers.MEDIUM_REGULATED_MOTOR);
        hardwareElements.put("MotorD", HardwareIdentifiers.LARGE_REGULATED_MOTOR);
        hardwareElements.put("Sensor2", HardwareIdentifiers.TOUCH_SENSOR);
        hardwareElements.put("Sensor3", HardwareIdentifiers.COLOR_SENSOR);
        hardwareElements.put("Sensor4", HardwareIdentifiers.TOUCH_SENSOR);
    }

    private void initPlcPortElements() {
        plcPortElements.put("Port1", PlcPortType.SENSOR_PORT);
        plcPortElements.put("Port2", PlcPortType.SENSOR_PORT);
        plcPortElements.put("Port3", PlcPortType.SENSOR_PORT);
        plcPortElements.put("Port4", PlcPortType.SENSOR_PORT);
        plcPortElements.put("PortA", PlcPortType.MOTOR_PORT);
        plcPortElements.put("PortB", PlcPortType.MOTOR_PORT);
        plcPortElements.put("PortC", PlcPortType.MOTOR_PORT);
        plcPortElements.put("PortD", PlcPortType.MOTOR_PORT);
    }

    private void initHardwareLinks() {
        hardwareLinks.put(pathToPlcPortElements + "Port2", pathToHardwareElements + "Sensor2");
        hardwareLinks.put(pathToPlcPortElements + "Port3", pathToHardwareElements + "Sensor3");
        hardwareLinks.put(pathToPlcPortElements + "Port4", pathToHardwareElements + "Sensor4");
        hardwareLinks.put(pathToPlcPortElements + "PortA", pathToHardwareElements + "MotorA");
        hardwareLinks.put(pathToPlcPortElements + "PortD", pathToHardwareElements + "MotorD");
    }

    private void setupTurntableHardware(OPCUABase opcuaBase, UaFolderNode ttNode, String path) {
        String hwFolderName = "Hardware";
        UaFolderNode hardwareFolder = opcuaBase.generateFolder(ttNode, path, hwFolderName);
        path = path + "/" + hwFolderName;

        addHardwareLinks(opcuaBase, hardwareFolder, path);

        UaFolderNode elementsFolder = opcuaBase.generateFolder(hardwareFolder, path, "Elements");
        path = path + "/Elements";
        for (String hardwareElement : hardwareElements.keySet()) {
            addHardwareElement(opcuaBase, elementsFolder, path, hardwareElement, hardwareElements.get(hardwareElement));
        }

        addPlcElement(opcuaBase, elementsFolder, path);
    }

    private void addHardwareElement(OPCUABase opcuaBase, UaFolderNode elementsFolder, String path, String elementName, HardwareIdentifiers identifiers) {
        path = path + "/" + elementName;
        UaFolderNode elementFolder = opcuaBase.generateFolder(elementsFolder, path, elementName);
        UaVariableNode status = opcuaBase.generateStringVariableNode(elementFolder, path + "/Status", "Status", "Undefined");
        UaVariableNode type = opcuaBase.generateStringVariableNode(elementFolder, path + "/Type", "Type", identifiers.name());
        UaVariableNode value = opcuaBase.generateStringVariableNode(elementFolder, path + "/Value", "Value", "Undefined");
        Optional<String> assignedPort = hardwareLinks.values().stream().filter(elem -> elem.contains(elementName)).findFirst();
        log.info("Found Port " + assignedPort + " for " + elementName);
        if (assignedPort.isPresent() && (identifiers.equals(HardwareIdentifiers.COLOR_SENSOR) || identifiers.equals(HardwareIdentifiers.TOUCH_SENSOR))) {
            Optional<Sensor> sensor = getSensorInstanceForPort(identifiers, parseSensorPortFromName(assignedPort.get()));
            log.info("Found Sensor " + sensor + " for " + elementName);
            if (sensor.isPresent()) {
                status.setValue(new DataValue(new Variant("OK")));

                getContext().getSystem().getScheduler().schedule(Duration.ZERO, Duration.ofSeconds(2), () -> {
                    value.setValue(new DataValue(new Variant(String.valueOf(sensor.get().hasDetectedInput()))));
                }, getContext().getDispatcher());
            } else {
                status.setValue(new DataValue(new Variant("Could not initialize Sensor")));
            }
        } else if (assignedPort.isPresent() && (identifiers.equals(HardwareIdentifiers.LARGE_REGULATED_MOTOR) || identifiers.equals(HardwareIdentifiers.MEDIUM_REGULATED_MOTOR))) {
            Optional<Motor> motor = getMotorInstanceForPort(identifiers, parseSensorPortFromName(assignedPort.get()));
            log.info("Found Motor " + motor + " for " + elementName);
            if (motor.isPresent()) {
                status.setValue(new DataValue(new Variant("OK")));
                getContext().getSystem().getScheduler().schedule(Duration.ZERO, Duration.ofSeconds(2), () -> {
                    value.setValue(new DataValue(new Variant(String.valueOf(motor.get().isRunning()))));
                }, getContext().getDispatcher());
            } else {
                status.setValue(new DataValue(new Variant("Could not initialize Motor")));
            }
        } else {
            status.setValue(new DataValue(new Variant("Sensor has no assigned Port")));
        }
    }

    private void addPlcElement(OPCUABase opcuaBase, UaFolderNode elementsFolder, String path) {
        path = path + "/PLC";
        UaFolderNode folderNode = opcuaBase.generateFolder(elementsFolder, path, "PLC");
        for (String portName : plcPortElements.keySet()) {
            addPlcPort(opcuaBase, folderNode, path + "/" + portName, portName, plcPortElements.get(portName));
        }
    }

    private void addPlcPort(OPCUABase opcuaBase, UaFolderNode plcFolder, String path, String portName, PlcPortType portIdentifier) {
        path = path + "/" + portName;
        UaFolderNode folderNode = opcuaBase.generateFolder(plcFolder, path, portName);
        opcuaBase.generateStringVariableNode(folderNode, path + "/Type", "Type", portIdentifier.name());
    }

    private void addHardwareLinks(OPCUABase opcuaBase, UaFolderNode hardwareFolder, String path) {
        path = path + "/Links";
        UaFolderNode linksFolder = opcuaBase.generateFolder(hardwareFolder, path, "Links");
        int counter = 0;
        for (String hardwareLink : hardwareLinks.keySet()) {
            counter++;
            String linkPath = path + "/Link" + counter;
            UaFolderNode linkFolder = opcuaBase.generateFolder(linksFolder, linkPath, "Link" + counter);
            opcuaBase.generateStringVariableNode(linkFolder, linkPath + "/From", "From", hardwareLink);
            opcuaBase.generateStringVariableNode(linkFolder, linkPath + "/To", "To", hardwareLinks.get(hardwareLink));
        }
    }

    private Port parseSensorPortFromName(String name) {
        switch (name) {
            case "Port1":
                return SensorPort.S1;
            case "Port2":
                return SensorPort.S2;
            case "Port3":
                return SensorPort.S3;
            case "Port4":
                return SensorPort.S4;
            case "PortA":
                return MotorPort.A;
            case "PortB":
                return MotorPort.B;
            case "PortC":
                return MotorPort.C;
            case "PortD":
                return MotorPort.D;
        }
        return null;    //Better handling than null needed?
    }

    private Optional<Sensor> getSensorInstanceForPort(HardwareIdentifiers identifier, Port sensorPort) {
        Sensor sensor;
        if (DEBUG) {
            sensor = new MockSensor();
            return Optional.of(sensor);
        } else {
            if (identifier.equals(HardwareIdentifiers.COLOR_SENSOR)) {
                sensor = new ColorSensorEV3(sensorPort);
                return Optional.of(sensor);
            } else if (identifier.equals(HardwareIdentifiers.TOUCH_SENSOR)) {
                sensor = new TouchSensorEV3(sensorPort);
                return Optional.of(sensor);
            }
        }
        return Optional.empty();
    }

    private Optional<Motor> getMotorInstanceForPort(HardwareIdentifiers identifier, Port motorPort) {
        Motor motor;
        if (DEBUG) {
            motor = new MockMotor(20);
            return Optional.of(motor);
        } else {
            if (identifier.equals(HardwareIdentifiers.LARGE_REGULATED_MOTOR)) {
                motor = new LargeMotorEV3(motorPort);
                return Optional.of(motor);
            } else if (identifier.equals(HardwareIdentifiers.MEDIUM_REGULATED_MOTOR)) {
                motor = new MediumMotorEV3(motorPort);
                return Optional.of(motor);
            }
        }
        return Optional.empty();
    }
}
