package fiab.mes.transport.actor.transportsystem;

import java.util.List;

import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.machine.msg.MachineDisconnectedEvent;
import fiab.mes.machine.msg.MachineStatusUpdateEvent;
import fiab.mes.planer.actor.MachineCapabilityManager;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface.Position;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface.RoutingException;
import fiab.mes.transport.msg.RegisterTransportRequest;
import fiab.mes.transport.msg.TransportModuleRequest;

public class TransportSystemCoordinatorActor extends AbstractActor {

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	
	protected TransportRoutingInterface routing;
	protected TransportPositionLookupInterface dns;
	// manages which transportmodule has which capabilities
	protected MachineCapabilityManager capMan = new MachineCapabilityManager();
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
		        .match(RegisterTransportRequest.class, req -> {
		        	handleNewIncomingRequest(req); // Message to register transport among machines (including input and output station)
		        })
		        // available transport systems
				.match(MachineConnectedEvent.class, machineEvent -> {
					handleNewlyAvailableMachine(machineEvent);
				})
				.match(MachineDisconnectedEvent.class, machineEvent -> {
					handleNoLongerAvailableMachine(machineEvent);
				})
				.match(MachineStatusUpdateEvent.class, machineEvent -> {
					handleMachineUpdateEvent(machineEvent);
				})
		        .build();
		
		// message from turntable on their state
		
	}

	private void handleMachineUpdateEvent(MachineStatusUpdateEvent machineEvent) {
		//check if one of the transport modules
		// for each module/actor store state,
		// then check if transport request can be completed
		
	}

	private void handleNoLongerAvailableMachine(MachineDisconnectedEvent machineEvent) {
		//check if one of the transport modules
		//check if currently in use,
		//check if transport request thus cannot be fulfilled
	}

	private void handleNewlyAvailableMachine(MachineConnectedEvent machineEvent) {
		//check if a transport modules, if so:
		capMan.setCapabilities(machineEvent);
		log.info("Storing Capabilities for TransportModule: "+machineEvent.getMachineId());
		// now wait for machine available event to make use of it (currently we dont know its state)
	}

	// handle Message to register transport among machines (including input and output station)
	private void handleNewIncomingRequest(RegisterTransportRequest rtr) {
		// for now we assume, that if a request comes in, the source/dest are ready for transport
		Position sourceP = dns.getPositionForActor(rtr.getSource());
		Position destP = dns.getPositionForActor(rtr.getDestination());
		if (sourceP != TransportRoutingInterface.UNKNOWN_POSITION && destP != TransportRoutingInterface.UNKNOWN_POSITION) {
			try {
				List<Position> route = routing.calculateRoute(sourceP, destP);
				// check if all transport modules are available to fulfull route 
				
				
				Integer routeSize = route.size();
				switch(routeSize) {
				case 2:
					TransportModuleRequest tmr2 = new TransportModuleRequest(route.get(0), route.get(1), rtr.getOrderId()); //move something onto turntable
					// check if transportmodule at pos 0 or 1 is available (and not reserved)
					// if not queue,
					// if so, mark transport module(s) as reserved
					break;
				case 3:
					TransportModuleRequest tmr3 = new TransportModuleRequest(route.get(0), route.get(2), rtr.getOrderId()); //move something through single turntable
					// check if transportmodule at pos 1 is available (and not reserved)
					break;
				case 4:	
					TransportModuleRequest tmr4a = new TransportModuleRequest(route.get(0), route.get(2), rtr.getOrderId()); //move from first turntable to second
					TransportModuleRequest tmr4b = new TransportModuleRequest(route.get(1), route.get(3), rtr.getOrderId()); //move something from second to destination
					// check if transportmodules at pos 1 and 2 are available (and not reserved)
					break;
				default:
					// TODO return unsuccessful route generation
					log.warning(String.format("Unable to establish route between s% with Position %s and %s with Position %s due to unsupported routesize %s", 
							rtr.getSource().getId(), sourceP.getPos(), rtr.getDestination().getId(), destP.getPos(), routeSize));
					break;
				}
				
			} catch (RoutingException e) {
				log.warning(String.format("Unable to establish route between s% with Position %s and %s with Position %s due to %s", 
						rtr.getSource().getId(), sourceP.getPos(), rtr.getDestination().getId(), destP.getPos(), e.getMessage()));
				//TODO return unsuccessful request instead of successfull registering
			}
		} else {
			//TODO return unsuccessful request instead of successfull registering
		}
	}
	
}
