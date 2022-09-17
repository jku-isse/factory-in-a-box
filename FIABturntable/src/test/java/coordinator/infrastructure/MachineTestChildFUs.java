package coordinator.infrastructure;

import akka.actor.ActorContext;
import akka.testkit.javadsl.TestKit;
import fiab.functionalunit.connector.FUConnector;
import fiab.functionalunit.connector.FUSubscriptionClassifier;
import fiab.functionalunit.connector.IntraMachineEventBus;
import fiab.handshake.connector.ServerNotificationConnector;
import fiab.handshake.connector.ServerResponseConnector;
import fiab.conveyor.ConveyorCapability;
import fiab.functionalunit.MachineChildFUs;
import fiab.turntable.turning.TurningCapability;

/**
 * This class adds the components in a way that a probe can be attached for testing purposes
 */
public class MachineTestChildFUs extends MachineChildFUs {

    public FUConnector addTurningFU(TestKit turningProbe, FUSubscriptionClassifier subscriptionClassifier){
        FUConnector turningConnector = new FUConnector();
        turningConnector.subscribe(turningProbe.getRef(), subscriptionClassifier);
        this.fuConnectors.put(TurningCapability.CAPABILITY_ID, turningConnector);
        return turningConnector;
    }

    public FUConnector addConveyorFU(TestKit conveyorProbe, FUSubscriptionClassifier subscriptionClassifier){
        FUConnector conveyorConnector = new FUConnector();
        conveyorConnector.subscribe(conveyorProbe.getRef(), subscriptionClassifier);
        this.fuConnectors.put(ConveyorCapability.CAPABILITY_ID, conveyorConnector);
        return conveyorConnector;
    }

    public FUConnector addServerSideHandshake(String capabilityId, TestKit serverHsProbe, FUSubscriptionClassifier subscriptionClassifier,
                                              ServerResponseConnector responseConnector, ServerNotificationConnector notificationConnector){
        FUConnector serverHandshakeConnector = new FUConnector();
        serverHandshakeConnector.subscribe(serverHsProbe.getRef(), subscriptionClassifier);
        this.fuConnectors.put(capabilityId, serverHandshakeConnector);
        return serverHandshakeConnector;
    }

    public FUConnector addClientSideHandshake(String capabilityId, TestKit serverHsProbe, FUSubscriptionClassifier subscriptionClassifier,
                                              FUConnector remoteConnector, ServerResponseConnector responseConnector, ServerNotificationConnector notificationConnector){
        FUConnector clientHandshakeConnector = new FUConnector();
        clientHandshakeConnector.subscribe(serverHsProbe.getRef(), subscriptionClassifier);
        this.fuConnectors.put(capabilityId, clientHandshakeConnector);
        return clientHandshakeConnector;
    }

    @Override
    public void setupInfrastructure(ActorContext context, IntraMachineEventBus intraMachineEventBus) {
        //We spawn the actors elsewhere and add the connectors before passing it to the constructor
    }
}
