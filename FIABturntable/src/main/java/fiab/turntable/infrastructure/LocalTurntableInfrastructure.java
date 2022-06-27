package fiab.turntable.infrastructure;

import akka.actor.ActorContext;
import fiab.functionalunit.connector.FUConnector;
import fiab.functionalunit.connector.IntraMachineEventBus;
import fiab.handshake.client.ClientSideHandshakeActor;
import fiab.handshake.connector.ServerNotificationConnector;
import fiab.handshake.connector.ServerResponseConnector;
import fiab.handshake.server.ServerSideHandshakeActor;
import fiab.conveyor.ConveyorActor;
import fiab.conveyor.ConveyorCapability;
import fiab.turntable.turning.TurningActor;
import fiab.turntable.turning.TurningCapability;

import static fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers.*;
import static fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_CLIENT;

public class LocalTurntableInfrastructure extends TurntableInfrastructure {

    private ActorContext context;
    private IntraMachineEventBus intraMachineEventBus;

    public LocalTurntableInfrastructure() {
        super();
    }

    @Override
    public void setupInfrastructure(ActorContext context, IntraMachineEventBus intraMachineEventBus) {
        this.context = context;
        this.intraMachineEventBus = intraMachineEventBus;
        createAndLinkTurningFU();
        createAndLinkConveyorFU();
        createAndLinkHandshakePair(TRANSPORT_MODULE_NORTH_SERVER, TRANSPORT_MODULE_NORTH_CLIENT);
        createAndLinkHandshakePair(TRANSPORT_MODULE_EAST_SERVER, TRANSPORT_MODULE_EAST_CLIENT);
        createAndLinkHandshakePair(TRANSPORT_MODULE_SOUTH_SERVER, TRANSPORT_MODULE_SOUTH_CLIENT);
        createAndLinkHandshakePair(TRANSPORT_MODULE_WEST_SERVER, TRANSPORT_MODULE_WEST_CLIENT);
    }

    private void createAndLinkTurningFU() {
        String capID = TurningCapability.CAPABILITY_ID;
        FUConnector turningConnector = new FUConnector();
        context.actorOf(TurningActor.props(turningConnector, intraMachineEventBus), capID);
        this.fuConnectors.put(capID, turningConnector);
    }

    private void createAndLinkConveyorFU() {
        String capID = ConveyorCapability.CAPABILITY_ID;
        FUConnector conveyorConnector = new FUConnector();
        context.actorOf(ConveyorActor.props(conveyorConnector, intraMachineEventBus), capID);
        this.fuConnectors.put(capID, conveyorConnector);
    }

    private void createAndLinkServerHandshakeFU(String serverId,
                                                ServerResponseConnector responseConnector, ServerNotificationConnector notificationConnector) {
        FUConnector handshakeConnector = new FUConnector();
        context.actorOf(ServerSideHandshakeActor.props(handshakeConnector, intraMachineEventBus, responseConnector, notificationConnector), serverId);
        this.fuConnectors.put(serverId, handshakeConnector);
    }

    private void createAndLinkClientHandshakeFU(String clientId, FUConnector serverRequestBus,
                                                ServerResponseConnector responseConnector, ServerNotificationConnector notificationConnector) {
        FUConnector handshakeConnector = new FUConnector();
        context.actorOf(ClientSideHandshakeActor.propsLocalHandshake(handshakeConnector, intraMachineEventBus, serverRequestBus,
                responseConnector, notificationConnector), clientId);
        this.fuConnectors.put(clientId, handshakeConnector);
    }

    private void createAndLinkHandshakePair(String serverId, String clientId) {
        FUConnector serverHandshakeConnector = new FUConnector();
        FUConnector clientHandshakeConnector = new FUConnector();
        ServerResponseConnector responseConnector = new ServerResponseConnector();
        ServerNotificationConnector notificationConnector = new ServerNotificationConnector();
        context.actorOf(ServerSideHandshakeActor.props(serverHandshakeConnector, intraMachineEventBus,
                responseConnector, notificationConnector), serverId);
        context.actorOf(ClientSideHandshakeActor.propsLocalHandshake(clientHandshakeConnector, intraMachineEventBus,
                serverHandshakeConnector, responseConnector, notificationConnector), clientId);
        this.fuConnectors.put(serverId, serverHandshakeConnector);
        this.fuConnectors.put(clientId, clientHandshakeConnector);
    }
}
