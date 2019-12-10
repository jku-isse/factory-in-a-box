package fiab.mes.transport.actor.transportsystem;

import akka.actor.AbstractActor;

public class TransportSystemCoordinatorActor extends AbstractActor {

	TransportRoutingInterface routing;
	
	@Override
	public Receive createReceive() {
		// TODO Auto-generated method stub
		return null;
		// Message to register transport among machines (including input and output station)
		// message from turntable on their state
		
	}

}
