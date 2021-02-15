package fiab.machine.plotter.opcua;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

import actuators.Motor;
import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.actor.AbstractActor.Receive;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import config.HardwareInfo;
import fiab.opcua.server.OPCUABase;
import sensors.Sensor;

public class OPCUAPlotterHardwareMonitor extends AbstractActor{
	
	   private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

	    protected enum HardwareIdentifiers {LARGE_REGULATED_MOTOR, MEDIUM_REGULATED_MOTOR, TOUCH_SENSOR, COLOR_SENSOR}

	    protected enum PlcPortType {MOTOR_PORT, SENSOR_PORT}

	    private final String pathToHardwareElements;
	    private final String pathToPlcPortElements;

	    private final Map<String, HardwareIdentifiers> hardwareElements;
	    private final Map<String, PlcPortType> plcPortElements;
	    private final Map<String, String> hardwareLinks;
	    private final Map<String, Motor> connectedMotors;
	    private final Map<String, Sensor> connectedSensors;
	    private final HardwareInfo hardwareInfo;

	    public static Props props(OPCUABase opcuaBase, UaFolderNode ttNode, String fuPrefix, HardwareInfo hardwareInfo) {
	        return Props.create(OPCUAPlotterHardwareMonitor.class, () -> new OPCUAPlotterHardwareMonitor(opcuaBase, ttNode, fuPrefix, hardwareInfo));
	    }

	    public OPCUAPlotterHardwareMonitor(OPCUABase opcuaBase, UaFolderNode ttNode, String fuPrefix, HardwareInfo hardwareInfo) {
	        this.pathToHardwareElements = fuPrefix + "/Hardware/Elements/";
	        this.pathToPlcPortElements = fuPrefix + "Hardware/Elements/PLC/";
	        this.hardwareInfo = hardwareInfo;
	        hardwareElements = new HashMap<>();
	        plcPortElements = new HashMap<>();
	        hardwareLinks = new HashMap<>();
	        connectedMotors = new HashMap<>();
	        connectedSensors = new HashMap<>();

	        initHardwareElements();
	        initPlcPortElements();
	        initHardwareLinks();
	        linkElementsToConnectedHardware();

	        setupTurntableHardware(opcuaBase, ttNode, fuPrefix);
	    }

	    @Override
	    public Receive createReceive() {
	        //Does not receive messages
	        return receiveBuilder().build();
	    }

	    public void initHardwareElements() {
	        hardwareElements.put("MotorA", HardwareIdentifiers.LARGE_REGULATED_MOTOR);
	        hardwareElements.put("MotorB", HardwareIdentifiers.LARGE_REGULATED_MOTOR);
	        hardwareElements.put("MotorC", HardwareIdentifiers.LARGE_REGULATED_MOTOR);
	        hardwareElements.put("MotorD", HardwareIdentifiers.MEDIUM_REGULATED_MOTOR);
	        hardwareElements.put("Sensor1", HardwareIdentifiers.COLOR_SENSOR);
	        hardwareElements.put("Sensor2", HardwareIdentifiers.TOUCH_SENSOR);
	        hardwareElements.put("Sensor3", HardwareIdentifiers.TOUCH_SENSOR);
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
	    	hardwareLinks.put(pathToPlcPortElements + "Port1", pathToHardwareElements + "Sensor1");
	        hardwareLinks.put(pathToPlcPortElements + "Port2", pathToHardwareElements + "Sensor2");
	        hardwareLinks.put(pathToPlcPortElements + "Port3", pathToHardwareElements + "Sensor3");
	        hardwareLinks.put(pathToPlcPortElements + "Port4", pathToHardwareElements + "Sensor4");
	        hardwareLinks.put(pathToPlcPortElements + "PortA", pathToHardwareElements + "MotorA");
	        hardwareLinks.put(pathToPlcPortElements + "PortB", pathToHardwareElements + "MotorB");
	        hardwareLinks.put(pathToPlcPortElements + "PortC", pathToHardwareElements + "MotorC");
	        hardwareLinks.put(pathToPlcPortElements + "PortD", pathToHardwareElements + "MotorD");
	    }

	    private void linkElementsToConnectedHardware() {
	        if (hardwareInfo.getMotorA().isPresent()) connectedMotors.put("MotorA", hardwareInfo.getMotorA().get());
	        if (hardwareInfo.getMotorB().isPresent()) connectedMotors.put("MotorB", hardwareInfo.getMotorB().get());
	        if (hardwareInfo.getMotorC().isPresent()) connectedMotors.put("MotorC", hardwareInfo.getMotorC().get());
	        if (hardwareInfo.getMotorD().isPresent()) connectedMotors.put("MotorD", hardwareInfo.getMotorD().get());
	        if (hardwareInfo.getSensor1().isPresent()) connectedSensors.put("Sensor1", hardwareInfo.getSensor1().get());
	        if (hardwareInfo.getSensor2().isPresent()) connectedSensors.put("Sensor2", hardwareInfo.getSensor2().get());
	        if (hardwareInfo.getSensor3().isPresent()) connectedSensors.put("Sensor3", hardwareInfo.getSensor3().get());
	        if (hardwareInfo.getSensor4().isPresent()) connectedSensors.put("Sensor4", hardwareInfo.getSensor4().get());
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
	            Optional<Sensor> sensor = getSensorInstanceForPort(elementName);
	            log.info("Found Sensor " + sensor + " for " + elementName);
	            if (sensor.isPresent()) {
	                status.setValue(new DataValue(new Variant("OK")));
	                getContext().getSystem().getScheduler().schedule(Duration.ZERO, Duration.ofSeconds(3), () -> {
	                    value.setValue(new DataValue(new Variant(String.valueOf(sensor.get().hasDetectedInput()))));
	                }, getContext().getDispatcher());
	            } else {
	                status.setValue(new DataValue(new Variant("Could not initialize Sensor")));
	            }
	        } else if (assignedPort.isPresent() && (identifiers.equals(HardwareIdentifiers.LARGE_REGULATED_MOTOR)
	                || identifiers.equals(HardwareIdentifiers.MEDIUM_REGULATED_MOTOR))) {
	            Optional<Motor> motor = getMotorInstanceForPort(elementName);
	            log.info("Found Motor " + motor + " for " + elementName);
	            if (motor.isPresent()) {
	                status.setValue(new DataValue(new Variant("OK")));
	                getContext().getSystem().getScheduler().schedule(Duration.ZERO, Duration.ofSeconds(3), () -> {
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

	    private Optional<Sensor> getSensorInstanceForPort(String sensorId) {
	        if (connectedSensors.containsKey(sensorId)) {
	            return Optional.of(connectedSensors.get(sensorId));
	        }
	        log.info("Could not find Sensor with id " + sensorId);
	        return Optional.empty();
	    }

	    private Optional<Motor> getMotorInstanceForPort(String motorId) {
	        if (connectedMotors.containsKey(motorId)) {
	            return Optional.of(connectedMotors.get(motorId));
	        }
	        log.info("Could not find Motor with id " + motorId);
	        return Optional.empty();
	    }

}
