package fiab.iostation.opcua;

import akka.actor.Props;
import fiab.core.capabilities.handshake.HandshakeCapability;
import fiab.core.capabilities.handshake.IOStationCapability;
import fiab.core.capabilities.handshake.ServerSideStates;
import fiab.core.capabilities.handshake.client.CompleteHandshake;
import fiab.core.capabilities.handshake.server.TransportAreaStatusOverrideRequest;
import fiab.core.capabilities.meta.OPCUACapabilitiesAndWiringInfoBrowsenames;
import fiab.functionalunit.connector.FUConnector;
import fiab.functionalunit.connector.IntraMachineEventBus;
import fiab.functionalunit.connector.MachineEventBus;
import fiab.handshake.server.messages.ServerHandshakeStatusUpdateEvent;
import fiab.handshake.server.opcua.functionalunit.ServerHandshakeFU;
import fiab.opcua.server.OPCUABase;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;

import java.time.Duration;

public class OpcUaOutputStationActor extends ServerHandshakeFU {

    public static Props props(OPCUABase base, UaFolderNode root, MachineEventBus eventBus){
        return Props.create(OpcUaOutputStationActor.class, () -> new OpcUaOutputStationActor(base, root, eventBus));
    }

    private final String componentId = IOStationCapability.OUTPUTSTATION_CAPABILITY_ID;
    private final MachineEventBus eventBus;

    public OpcUaOutputStationActor(OPCUABase base, UaFolderNode root, MachineEventBus eventBus) {
        super(base, root, "", new FUConnector(), new IntraMachineEventBus());
        this.eventBus = eventBus;
    }

    @Override
    protected void addCapabilities(UaFolderNode clientFuNode) {
        super.addCapabilities(clientFuNode);
        addOutputStationCapability(capabilitiesFolder);
    }

    protected void addOutputStationCapability(UaFolderNode capabilitiesFolder) {
        UaFolderNode outputStationCapNode = base.generateFolder(capabilitiesFolder,
                OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITY + 1);

        base.generateStringVariableNode(outputStationCapNode,
                OPCUACapabilitiesAndWiringInfoBrowsenames.TYPE,
                IOStationCapability.OUTPUTSTATION_CAPABILITY_URI);
        base.generateStringVariableNode(outputStationCapNode,
                OPCUACapabilitiesAndWiringInfoBrowsenames.ID,
                IOStationCapability.OUTPUTSTATION_CAPABILITY_ID);
        base.generateStringVariableNode(outputStationCapNode,
                OPCUACapabilitiesAndWiringInfoBrowsenames.ROLE,
                OPCUACapabilitiesAndWiringInfoBrowsenames.ROLE_VALUE_PROVIDED);
    }

    @Override
    public void doResetting() {
        //super.do_resetting();   //Wait for the sensor to detect no pallet
        //For now simulate behaviour of sensor waiting for pallet
        self().tell(new TransportAreaStatusOverrideRequest(componentId, HandshakeCapability.StateOverrideRequests.SetEmpty), self());
        context().system().scheduler().scheduleOnce(Duration.ofSeconds(2), () -> super.doResetting(), context().dispatcher());
    }

    @Override
    public void doExecute() {
        //super.doExecute();    //We provide our own implementation here
        self().tell(new TransportAreaStatusOverrideRequest(componentId, HandshakeCapability.StateOverrideRequests.SetLoaded), self());
        self().tell(new TransportAreaStatusOverrideRequest(componentId, HandshakeCapability.StateOverrideRequests.SetEmpty), self());
        //For now we just simulate it, later we can use real hardware e.g. via ROS for example
        context().system().scheduler().scheduleOnce(Duration.ofSeconds(2),
                () -> self().tell(new CompleteHandshake(componentId), self()),
                context().dispatcher());
    }

    @Override
    public void publishCurrentState(ServerSideStates state) {
        super.publishCurrentState(state);
        if(this.eventBus != null) eventBus.publish(new ServerHandshakeStatusUpdateEvent(componentId, state));
    }
}
