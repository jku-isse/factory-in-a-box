package fiab.machine.iostation.opcua;

import java.time.Duration;

import config.HardwareInfo;
import config.MachineType;
import fiab.machine.iostation.monitor.OpcUaInputStationHardwareMonitor;
import fiab.machine.iostation.monitor.OpcUaOutputStationHardwareMonitor;
import hardware.InputStationHardware;
import hardware.OutputStationHardware;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import main.java.fiab.core.capabilities.handshake.HandshakeCapability;
import main.java.fiab.core.capabilities.handshake.IOStationCapability;
import main.java.fiab.core.capabilities.handshake.HandshakeCapability.ServerSideStates;
import main.java.fiab.core.capabilities.meta.OPCUACapabilitiesAndWiringInfoBrowsenames;
import fiab.handshake.actor.LocalEndpointStatus;
import fiab.handshake.fu.HandshakeFU;
import fiab.opcua.CapabilityExposingUtils;
import fiab.opcua.CapabilityImplementationMetadata;
import fiab.opcua.CapabilityImplementationMetadata.ProvOrReq;
import fiab.opcua.server.NonEncryptionBaseOpcUaServer;
import fiab.opcua.server.OPCUABase;
import sensors.MockSensor;

//TODO separate input/output root actors into own actor
public class OPCUAIOStationRootActor extends AbstractActor {

    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private String machineName = "IO";
    static final String NAMESPACE_URI = "urn:factory-in-a-box";
    private UaVariableNode statusIOS = null;
    private HandshakeFU fu;
    protected ActorRef self;
    private boolean isInputStation;
    private ServerSideStates currentState;
    private HardwareInfo hardwareInfo;
    private InputStationHardware inputStationHardware;
    private OutputStationHardware outputStationHardware;
    private boolean isUnloading = false;

    static public Props propsForInputStation(String machineName, int portOffset) {
        return Props.create(OPCUAIOStationRootActor.class, () -> new OPCUAIOStationRootActor(machineName, portOffset, true));
    }

    static public Props propsForOutputStation(String machineName, int portOffset) {
        return Props.create(OPCUAIOStationRootActor.class, () -> new OPCUAIOStationRootActor(machineName, portOffset, false));
    }

    public OPCUAIOStationRootActor(String machineName, int portOffset, boolean isInputStation) {
        try {
            this.machineName = machineName;
            this.isInputStation = isInputStation;
            this.self = self();
            init(portOffset);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ServerSideStates.class, req -> {
                    if (req == ServerSideStates.RESETTING) {
                        if (isInputStation) {
                            waitForPalletReset();
                        }
                    } else if (req == ServerSideStates.COMPLETING || req == ServerSideStates.COMPLETE) {
                        if (isInputStation && !isUnloading) {
                            isUnloading = true;
                            startPalletHandover();
                        }
                    } else if (req == ServerSideStates.STOPPING) {
                        if (isInputStation) {
                            inputStationHardware.getReleaseMotor().stop();
                            isUnloading = false;
                        }
                    } else if (req == ServerSideStates.EXECUTE && !isInputStation) {
                        if (outputStationHardware.getPalletSensor() instanceof MockSensor) {
                            log.info("Setting mock sensor value to simulate pallet to true");
                            ((MockSensor) outputStationHardware.getPalletSensor()).setDetectedInput(true);
                        }
                        finishOutputStationHandover();
                    } else {
                        currentState = req;
                        setStatusValue(currentState.toString());
                    }
                })
                .match(LocalEndpointStatus.LocalServerEndpointStatus.class, les -> {//ignore
                })
                .build();
    }

    private void finishOutputStationHandover() {
        context().system().scheduler().scheduleOnce(Duration.ofSeconds(7), () -> {
            if (outputStationHardware.getPalletSensor() instanceof MockSensor) {
                log.info("Setting mock sensor value to simulate pallet to false");
                ((MockSensor) outputStationHardware.getPalletSensor()).setDetectedInput(false);
            }
            currentState = ServerSideStates.COMPLETING;
            setStatusValue(currentState.toString());
            completeOutputStationPalletHandover();
        }, context().dispatcher());
    }

    private void completeOutputStationPalletHandover() {
        context().system().scheduler().scheduleOnce(Duration.ofSeconds(1), () -> {
            currentState = ServerSideStates.COMPLETE;
            setStatusValue(currentState.toString());
        }, context().dispatcher());
    }

    private void startPalletHandover() {
        inputStationHardware.getReleaseMotor().forward();   //open gate
        waitForPalletUnloaded();
    }

    private void waitForPalletUnloaded() {
        if (!inputStationHardware.getPalletSensor().hasDetectedInput()) {
            inputStationHardware.getReleaseMotor().stop();  //pallet has been unloaded
            fu.getFUActor().tell(HandshakeCapability.StateOverrideRequests.SetEmpty, self);
            completePalletHandover();
        } else {
            context().system().scheduler().scheduleOnce(Duration.ofMillis(200),
                    () -> {
                        waitForPalletUnloaded();
                    },
                    context().system().dispatcher());
        }
    }

    private void completePalletHandover() {
        inputStationHardware.getReleaseMotor().backward();      //close gate
        context().system().scheduler().scheduleOnce(Duration.ofMillis(1000),
                () -> {
                    inputStationHardware.getReleaseMotor().stop();
                    isUnloading = false;
                    currentState = ServerSideStates.COMPLETE;
                    setStatusValue(currentState.toString());
                },
                context().system().dispatcher());
    }


    private void init(int portOffset) throws Exception {
        NonEncryptionBaseOpcUaServer server1 = new NonEncryptionBaseOpcUaServer(portOffset, machineName);
        OPCUABase opcuaBase = new OPCUABase(server1.getServer(), NAMESPACE_URI, machineName);
        UaFolderNode root = opcuaBase.prepareRootNode();
        UaFolderNode ioNode = opcuaBase.generateFolder(root, machineName, "IOSTATION");
        String fuPrefix = machineName + "/" + "IOSTATION";
        fu = isInputStation ? new IOStationHandshakeFU.InputStationHandshakeFU(opcuaBase, ioNode, fuPrefix, getSelf(), getContext(), "DefaultServerSideHandshake", OPCUACapabilitiesAndWiringInfoBrowsenames.IS_PROVIDED, true) :
                new IOStationHandshakeFU.OutputStationHandshakeFU(opcuaBase, ioNode, fuPrefix, getSelf(), getContext(), "DefaultServerSideHandshake", OPCUACapabilitiesAndWiringInfoBrowsenames.IS_PROVIDED, true);
        setupOPCUANodeSet(opcuaBase, ioNode, fuPrefix, fu.getFUActor());
        //Add hardware info
        if (isInputStation) {
            hardwareInfo = new HardwareInfo(MachineType.INPUTSTATION);
            if (hardwareInfo.getInputStationHardware().isPresent()) {
                inputStationHardware = hardwareInfo.getInputStationHardware().get();
            }
            context().actorOf(OpcUaInputStationHardwareMonitor.props(opcuaBase, ioNode, fuPrefix, hardwareInfo));
        } else {
            hardwareInfo = new HardwareInfo(MachineType.OUTPUTSTATION);
            if (hardwareInfo.getOutputStationHardware().isPresent()) {
                outputStationHardware = hardwareInfo.getOutputStationHardware().get();
                //hardwareInfo = new HardwareInfo(MachineType.OUTPUTSTATION);
            }
            context().actorOf(OpcUaOutputStationHardwareMonitor.props(opcuaBase, ioNode, fuPrefix, hardwareInfo));
        }

        CapabilityExposingUtils.setupCapabilities(opcuaBase, ioNode, fuPrefix, new CapabilityImplementationMetadata("DefaultStation",
                isInputStation ? IOStationCapability.INPUTSTATION_CAPABILITY_URI : IOStationCapability.OUTPUTSTATION_CAPABILITY_URI,
                ProvOrReq.PROVIDED));
        Thread s1 = new Thread(opcuaBase);
        s1.start();
        /*if (doAutoReload) {
            reloadPallet();     //We will use hardware for this
        }*/
    }


    private void setupOPCUANodeSet(OPCUABase opcuaBase, UaFolderNode folderNode, String path, ActorRef actor) {

        UaMethodNode n1 = opcuaBase.createPartialMethodNode(path, IOStationCapability.RESET_REQUEST, "Requests reset");
        opcuaBase.addMethodNode(folderNode, n1, new fiab.handshake.fu.server.methods.Reset(n1, actor));
        UaMethodNode n2 = opcuaBase.createPartialMethodNode(path, IOStationCapability.STOP_REQUEST, "Requests stop");
        opcuaBase.addMethodNode(folderNode, n2, new fiab.handshake.fu.server.methods.Stop(n2, actor));
        //if (IOStationCapability.STATE_VAR_NAME != IOStationCapability.OPCUA_STATE_SERVERSIDE_VAR_NAME) {
        statusIOS = opcuaBase.generateStringVariableNode(folderNode, path, IOStationCapability.STATE_VAR_NAME, ServerSideStates.STOPPED);
        //}
    }

    public void setStatusValue(String newStatus) {
        if (statusIOS != null) {
            statusIOS.setValue(new DataValue(new Variant(newStatus)));
        }
    }

    private void waitForPalletReset() {
        context().system()
                .scheduler()
                .scheduleOnce(Duration.ofMillis(200), () -> {
                    if (inputStationHardware.getPalletSensor().hasDetectedInput()) {
                        fu.getFUActor().tell(HandshakeCapability.StateOverrideRequests.SetLoaded, self);
                        currentState = ServerSideStates.IDLE_LOADED;
                        setStatusValue(currentState.toString());
                    } else {
                        waitForPalletReset();
                    }
                }, context().system().dispatcher());
    }


}
