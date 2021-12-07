package fiab.mes.productioncell;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.OrderEventBusWrapperActor;
import fiab.mes.planer.actor.OrderPlanningActor;
import fiab.mes.productioncell.foldingstation.DefaultFoldingCellTransportPositionLookup;
import fiab.mes.productioncell.foldingstation.FoldingProductionCellCoordinator;
import fiab.mes.productioncell.foldingstation.HardcodedFoldingCellTransportRoutingAndMapping;
import fiab.mes.transport.actor.transportsystem.DefaultTransportPositionLookup;
import fiab.mes.transport.actor.transportsystem.HardcodedDefaultTransportRoutingAndMapping;
import fiab.mes.transport.actor.transportsystem.TransportSystemCoordinatorActor;

public class DefaultProductionCellInfrastructure {

	// create machine and order event bus
	public DefaultProductionCellInfrastructure(ActorSystem system, int expectedTTs) {
		HardcodedFoldingCellTransportRoutingAndMapping routing = new HardcodedFoldingCellTransportRoutingAndMapping();
		DefaultFoldingCellTransportPositionLookup dns = new DefaultFoldingCellTransportPositionLookup();
		ActorRef orderEventBus = system.actorOf(OrderEventBusWrapperActor.props(), "Folding"+OrderEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
		ActorRef machineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), "Folding"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
		ActorRef coordActor = system.actorOf(TransportSystemCoordinatorActor.props(routing, dns, expectedTTs), "Folding"+TransportSystemCoordinatorActor.WELLKNOWN_LOOKUP_NAME);
		ActorRef orderPlanningActor = system.actorOf(FoldingProductionCellCoordinator.props(), FoldingProductionCellCoordinator.WELLKNOWN_LOOKUP_NAME);
	}
}
