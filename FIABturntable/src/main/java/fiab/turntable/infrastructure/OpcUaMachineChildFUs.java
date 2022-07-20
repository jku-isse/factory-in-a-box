package fiab.turntable.infrastructure;

import akka.actor.ActorContext;
import fiab.functionalunit.MachineChildFUs;
import fiab.functionalunit.connector.FUConnector;
import fiab.functionalunit.connector.IntraMachineEventBus;
import fiab.handshake.client.opcua.functionalunit.ClientHandshakeFU;
import fiab.handshake.server.opcua.functionalunit.ServerHandshakeFU;
import fiab.opcua.server.OPCUABase;
import fiab.conveyor.ConveyorCapability;
import fiab.conveyor.opcua.functionalunit.ConveyorFU;
import fiab.turntable.turning.TurningCapability;
import fiab.turntable.turning.opcua.functionalunit.TurningFU;

import static fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers.*;
import static fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_CLIENT;

public class OpcUaMachineChildFUs extends MachineChildFUs {

    private final OPCUABase base;
    private ActorContext context;
    private IntraMachineEventBus intraMachineEventBus;

    public OpcUaMachineChildFUs(OPCUABase base) {
        super();
        this.base = base;
    }

    @Override
    public void setupInfrastructure(ActorContext context, IntraMachineEventBus intraMachineEventBus) {
        this.context = context;
        this.intraMachineEventBus = intraMachineEventBus;
        createAndLinkTurningFU();
        createAndLinkConveyorFU();
        //We have 4 servers
        createAndLinkServerHandshakeFU(TRANSPORT_MODULE_NORTH_SERVER);
        createAndLinkServerHandshakeFU(TRANSPORT_MODULE_EAST_SERVER);
        createAndLinkServerHandshakeFU(TRANSPORT_MODULE_SOUTH_SERVER);
        createAndLinkServerHandshakeFU(TRANSPORT_MODULE_WEST_SERVER);
        //And 4 clients
        createAndLinkClientHandshakeFU(TRANSPORT_MODULE_NORTH_CLIENT);
        createAndLinkClientHandshakeFU(TRANSPORT_MODULE_EAST_CLIENT);
        createAndLinkClientHandshakeFU(TRANSPORT_MODULE_SOUTH_CLIENT);
        createAndLinkClientHandshakeFU(TRANSPORT_MODULE_WEST_CLIENT);
    }

    private void createAndLinkTurningFU() {
        FUConnector turningConnector = new FUConnector();
        context.actorOf(TurningFU.props(base, base.getRootNode(), turningConnector, intraMachineEventBus), "TurningFU");
        this.fuConnectors.put(TurningCapability.CAPABILITY_ID, turningConnector);
    }

    private void createAndLinkConveyorFU() {
        FUConnector conveyorConnector = new FUConnector();
        context.actorOf(ConveyorFU.props(base, base.getRootNode(), conveyorConnector, intraMachineEventBus), "ConveyorFU");
        this.fuConnectors.put(ConveyorCapability.CAPABILITY_ID, conveyorConnector);
    }

    private void createAndLinkServerHandshakeFU(String capabilityId) {
        FUConnector handshakeConnector = new FUConnector();
        context.actorOf(ServerHandshakeFU.props(base, base.getRootNode(), capabilityId, handshakeConnector, intraMachineEventBus), capabilityId);
        this.fuConnectors.put(capabilityId, handshakeConnector);
    }

    private void createAndLinkClientHandshakeFU(String capabilityId) {
        FUConnector handshakeConnector = new FUConnector();
        context.actorOf(ClientHandshakeFU.props(base, base.getRootNode(), capabilityId, handshakeConnector, intraMachineEventBus), capabilityId);
        this.fuConnectors.put(capabilityId, handshakeConnector);
    }

}
