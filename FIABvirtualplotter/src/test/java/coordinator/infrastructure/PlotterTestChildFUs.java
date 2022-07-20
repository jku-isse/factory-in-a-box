package coordinator.infrastructure;

import akka.actor.ActorContext;
import akka.testkit.javadsl.TestKit;
import fiab.conveyor.ConveyorCapability;
import fiab.core.capabilities.handshake.HandshakeCapability;
import fiab.functionalunit.MachineChildFUs;
import fiab.functionalunit.connector.FUConnector;
import fiab.functionalunit.connector.FUSubscriptionClassifier;
import fiab.functionalunit.connector.IntraMachineEventBus;
import fiab.handshake.connector.ServerNotificationConnector;
import fiab.handshake.connector.ServerResponseConnector;
import fiab.plotter.plotting.PlottingCapability;

public class PlotterTestChildFUs extends MachineChildFUs {

    public FUConnector addPlottingFU(TestKit plottingProbe, FUSubscriptionClassifier subscriptionClassifier){
        FUConnector plottingConnector = new FUConnector();
        plottingConnector.subscribe(plottingProbe.getRef(), subscriptionClassifier);
        this.fuConnectors.put(PlottingCapability.CAPABILITY_ID, plottingConnector);
        return plottingConnector;
    }

    public FUConnector addConveyorFU(TestKit conveyorProbe, FUSubscriptionClassifier subscriptionClassifier){
        FUConnector conveyorConnector = new FUConnector();
        conveyorConnector.subscribe(conveyorProbe.getRef(), subscriptionClassifier);
        this.fuConnectors.put(ConveyorCapability.CAPABILITY_ID, conveyorConnector);
        return conveyorConnector;
    }

    public FUConnector addServerSideHandshake(TestKit serverHsProbe, FUSubscriptionClassifier subscriptionClassifier){
        FUConnector serverHandshakeConnector = new FUConnector();
        serverHandshakeConnector.subscribe(serverHsProbe.getRef(), subscriptionClassifier);
        this.fuConnectors.put(HandshakeCapability.SERVER_CAPABILITY_ID, serverHandshakeConnector);
        return serverHandshakeConnector;
    }

    @Override
    public void setupInfrastructure(ActorContext context, IntraMachineEventBus intraMachineEventBus) {
        //We spawn the actors elsewhere and add the connectors before passing it to the constructor
    }
}
