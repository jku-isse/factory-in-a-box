package fiab.mes.transport.actor.transportsystem;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.basicmachine.events.MachineEvent.MachineEventType;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.eventbus.MESSubscriptionClassifier;
import fiab.mes.general.HistoryTracker;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.machine.msg.MachineDisconnectedEvent;
import fiab.mes.planer.actor.MachineOrderMappingManager.MachineOrderMappingStatusLifecycleException;
import fiab.mes.planer.msg.PlanerStatusMessage;
import fiab.mes.planer.msg.PlanerStatusMessage.PlannerState;
import fiab.mes.restendpoint.requests.MachineHistoryRequest;
import fiab.mes.transport.actor.transportsystem.TransportModuleUsageTracker.TransportModuleOrderMappingStatus;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface.Position;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface.RoutingException;
import fiab.mes.transport.msg.CancelTransportRequest;
import fiab.mes.transport.msg.RegisterTransportRequest;
import fiab.mes.transport.msg.RegisterTransportRequestStatusResponse;
import fiab.mes.transport.msg.RegisterTransportRequestStatusResponse.ResponseType;
import fiab.mes.transport.msg.TransportModuleRequest;
import fiab.mes.transport.msg.TransportSystemStatusMessage;

public class TransportSystemCoordinatorActor extends AbstractActor {

	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	public static final String WELLKNOWN_LOOKUP_NAME = "TransportSystemCoordinatorActor";

	static public Props props(TransportRoutingInterface routing, TransportPositionLookupInterface dns, int expectedTTs, String lookupPrefix) {
		return Props.create(TransportSystemCoordinatorActor.class, () -> new TransportSystemCoordinatorActor(routing, dns, expectedTTs, lookupPrefix));
	}
	
	static public Props props(TransportRoutingInterface routing, TransportPositionLookupInterface dns, int expectedTTs) {	    
		return Props.create(TransportSystemCoordinatorActor.class, () -> new TransportSystemCoordinatorActor(routing, dns, expectedTTs));
	}
		
	// Externally provided:
	protected TransportRoutingInterface routing;
	protected TransportPositionLookupInterface dns;
	protected HistoryTracker externalHistory=null;

	protected ActorSelection machineEventBus;
	// tracks which transport module is in which state and how used by this actor
	protected TransportModuleUsageTracker tmut = new TransportModuleUsageTracker();
	protected AtomicInteger incrementalId = new AtomicInteger(0);
	protected List<TransportModuleRequest> requestQueue = new ArrayList<>();
	protected List<TransportModuleRequest> allocatedQueue = new ArrayList<>();
	protected Map<String, RegisterTransportRequest> queuedRequests = new HashMap<>();
	protected ActorRef self;
	protected TransportSystemStatusMessage.State state = TransportSystemStatusMessage.State.STOPPED;
	protected int expectedTTs = 1;
	private String lookupPrefix = "";

	public TransportSystemCoordinatorActor(TransportRoutingInterface routing, TransportPositionLookupInterface dns, int expectedTTs, String lookupPrefix) {
		this.lookupPrefix = lookupPrefix;
		this.routing = routing;
		this.dns = dns;
		this.self = getSelf();
		this.externalHistory = new HistoryTracker(WELLKNOWN_LOOKUP_NAME);
		getEventBusAndSubscribe();
		this.state = TransportSystemStatusMessage.State.STARTING;
		this.expectedTTs = expectedTTs;
		publishLocalState(MachineEventType.UPDATED, this.state, "TransportSystem Started and waiting for transport modules");
	}
	
	public TransportSystemCoordinatorActor(TransportRoutingInterface routing, TransportPositionLookupInterface dns, int expectedTTs) {
		this.routing = routing;
		this.dns = dns;
		this.self = getSelf();
		this.externalHistory = new HistoryTracker(WELLKNOWN_LOOKUP_NAME);
		getEventBusAndSubscribe();
		this.state = TransportSystemStatusMessage.State.STARTING;
		this.expectedTTs = expectedTTs;
		publishLocalState(MachineEventType.UPDATED, this.state, "TransportSystem Started and waiting for transport modules");
	}
	
	private void getEventBusAndSubscribe() {		
		SubscribeMessage machineSub = new SubscribeMessage(getSelf(), new MESSubscriptionClassifier(WELLKNOWN_LOOKUP_NAME, "*"));
		machineEventBus = this.context().actorSelection("/user/"+lookupPrefix+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
		machineEventBus.tell(machineSub, getSelf());			
	}
	
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(RegisterTransportRequest.class, req -> {
					log.info(String.format("Received Transport Request %s -> %s", req.getSource().getId(), req.getDestination().getId()));
					handleNewIncomingRequest(req); // Message to register transport among machines (including input and output station)
				})
				// available transport systems
				.match(MachineConnectedEvent.class, machineEvent -> {
					handleNewlyAvailableMachine(machineEvent);
				})
				.match(MachineDisconnectedEvent.class, machineEvent -> {
					handleNoLongerAvailableMachine(machineEvent);
				})
				// message from turntable (actually all machines) on their state --> LATER: filter to only receive events from transport modules
				.match(MachineStatusUpdateEvent.class, machineEvent -> {
					handleMachineUpdateEvent(machineEvent);
				})
				.match(CancelTransportRequest.class, req -> {
					handleCancelTransportRequest(req);
				})
				.match(MachineHistoryRequest.class, req -> {
		        	log.info(String.format("Machine %s received MachineHistoryRequest", WELLKNOWN_LOOKUP_NAME));
		        	externalHistory.sendHistoryResponseTo(req, getSender(), self);
		        })
				// TODO: include transportModuleErrorEvent/Message
				.matchAny(msg -> log.debug("TransportCoordinator received unknown message: {}", msg))
				.build();
	}
	
	private void publishLocalState(MachineEventType eventType, TransportSystemStatusMessage.State state, String message) {
		machineEventBus.tell(new TransportSystemStatusMessage(lookupPrefix+WELLKNOWN_LOOKUP_NAME, eventType, state, message), this.self());
	}
	
	//REFACTOR this when adding code to handle transport module error such as stopping unexpectedly
	private void handleCancelTransportRequest(CancelTransportRequest req) {
		// if just queued, then remove,
		Optional<TransportModuleRequest> tmrOp = requestQueue.stream()
			.filter(tmr -> tmr.getOrderId().equals(req.getOrderId()))
			.findAny();
		tmrOp.ifPresent(tmr -> { requestQueue.remove(tmr); 
			RegisterTransportRequest rtr = queuedRequests.remove(req.getOrderId());
			String msg = "Successfully canceled transport request "+req.getOrderId();
			log.info(msg);
			rtr.getRequestor().tell(new RegisterTransportRequestStatusResponse(rtr, RegisterTransportRequestStatusResponse.ResponseType.CANCELED ,msg), self);
		});
		// otherwise the transport might have started, lets just ask to remove it manually
		// requires stopping the transport module that will potentially deadlock waiting for a stopped machine
		// then needs resetting after a stop
		Optional<TransportModuleRequest> tmrOp2 = allocatedQueue.stream()
				.filter(tmr -> tmr.getOrderId().equals(req.getOrderId()))
				.findAny();
			tmrOp2.ifPresent(tmr -> { 
				allocatedQueue.remove(tmr); 
				RegisterTransportRequest rtr = queuedRequests.remove(req.getOrderId());
				String msg = "Aborted transport in progress, please remove order from transport system "+req.getOrderId();
				log.warning(msg);
				rtr.getRequestor().tell(new RegisterTransportRequestStatusResponse(rtr, RegisterTransportRequestStatusResponse.ResponseType.ABORTED ,msg), self);
			});
	}

	private void handleMachineUpdateEvent(MachineStatusUpdateEvent machineEvent) {
		//check if one of the transport modules --> done inside TMUT
		Optional<AbstractMap.SimpleEntry<String,String>> prevValues = tmut.updateIfExists(machineEvent);
		// if turntable is reset, 
		prevValues.ifPresent(prevOR -> {
			// check if associated previous order is now done across all turntables
			if ( prevOR.getKey() != null &&				
				 tmut.getTransportModulesInvolvedInOrder(prevOR.getKey()).size() == 0) {
					// if so then remove from allocatedQueue
					String msg = String.format("Transport for OrderId %s complete", prevOR.getKey());
					log.info(msg);
					allocatedQueue = allocatedQueue.stream()
							.filter(tmr -> !tmr.getOrderId().equals(prevOR.getKey()))
							.collect(Collectors.toList()); // basically remove that entry
					RegisterTransportRequest rtr = queuedRequests.remove(prevOR.getKey());
					if (rtr != null)
						rtr.getRequestor().tell(new RegisterTransportRequestStatusResponse(rtr, RegisterTransportRequestStatusResponse.ResponseType.COMPLETED ,msg), self);
				} // else wait for others turntables to complete			
		});
		//check if a new transport request can be completed
		matchRequestsToModules();
	}

	private void handleNoLongerAvailableMachine(MachineDisconnectedEvent machineEvent) {
		//TODO:
		//check if currently in use,
		//check if transport request thus cannot be fulfilled

		//capMan.removeActor(machineEvent.getMachine());
		//check if one of the transport modules --> not needed, as could have other capabilities
	}

	private void handleNewlyAvailableMachine(MachineConnectedEvent machineEvent) {
		//check if a transport module is irrelevant, we just then select machines that have turntable capability		
		//capMan.setCapabilities(machineEvent);
		Position pos = dns.getPositionForActor(machineEvent.getMachine()); // implicit coupling to hardcoded dns impl that needs first a call with actor to allow later for resolving by position
		tmut.trackIfTransportModule(machineEvent);
		log.info("Registered machine {} at position {} ", machineEvent.getMachineId(), pos);
		TransportSystemStatusMessage.State currState = state;
		TransportSystemStatusMessage.State newState = TransportSystemStatusMessage.State.STOPPED;
		if (tmut.getKnownTransportModules().size() >= expectedTTs) {
			newState = TransportSystemStatusMessage.State.FULLY_OPERATIONAL;
		} else {
			newState = TransportSystemStatusMessage.State.DEGRADED_MODE;
		}
		if (newState != currState) {
			state = newState;
			publishLocalState(MachineEventType.UPDATED, state, "Transportmodule availability changed");
		}
		//log.info("Storing Capabilities: "+machineEvent.getMachineId());
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
				Optional<TransportModuleRequest> tmrRoot = Optional.empty();
				Integer routeSize = route.size();
				switch(routeSize) {
				// for now we only support moving from machine to machine through 1 or 2 turntables
				case 3: 					
					Optional<AkkaActorBackedCoreModelAbstractActor> act1_3 = dns.getActorForPosition(route.get(1));					
					if (act1_3.isPresent()) {
						tmrRoot = act1_3.map(actor -> { if (tmut.isTransportModule(actor)) { // must be a turntable
							return new TransportModuleRequest(actor, route.get(0), route.get(2), rtr.getOrderId(), incrementalId.getAndIncrement()+""); 
						} else {
							// this should not be possible, perhaps we don't know about that turntable yet, thus we cant contact it, thus an error
							String msg = String.format("Unable to establish transport module for Position %s for request %s", route.get(1), rtr.getOrderId());
							log.warning(msg);
							this.getSender().tell(new RegisterTransportRequestStatusResponse(rtr, RegisterTransportRequestStatusResponse.ResponseType.MISSING_TRANSPORT_MODULE ,msg), self);
							return null;
						}
						});
					} else {
						String msg = String.format("Unable to establish transport module for Position %s for request %s", route.get(1), rtr.getOrderId());
						log.warning(msg);
						this.getSender().tell(new RegisterTransportRequestStatusResponse(rtr, RegisterTransportRequestStatusResponse.ResponseType.MISSING_TRANSPORT_MODULE ,msg), self);						
					}
					break;
				case 4:	// same as for case 3 but another stop in between, 
					Optional<AkkaActorBackedCoreModelAbstractActor> act1_4 = dns.getActorForPosition(route.get(1));
					Optional<AkkaActorBackedCoreModelAbstractActor> act2_4 = dns.getActorForPosition(route.get(2));					
					if (!act1_4.isPresent()) {
						String msg = String.format("Unable to establish transport module for position Position %s for request %s", route.get(1),  rtr.getOrderId());
						log.warning(msg);
						this.getSender().tell(new RegisterTransportRequestStatusResponse(rtr, RegisterTransportRequestStatusResponse.ResponseType.UNSUPPORTED_TRANSIT_POSITION ,msg), self);
						break;
					}
					if (!act2_4.isPresent()) {
						String msg = String.format("Unable to establish transport module for position Position %s for request %s", route.get(2),  rtr.getOrderId());
						log.warning(msg);
						this.getSender().tell(new RegisterTransportRequestStatusResponse(rtr, RegisterTransportRequestStatusResponse.ResponseType.UNSUPPORTED_TRANSIT_POSITION ,msg), self);
						break;
					}
					
					final Optional<TransportModuleRequest> tmr1_4 = act1_4.map(actor -> { 
						if (tmut.isTransportModule(actor)) { // must be a turntable
							return new TransportModuleRequest(actor, route.get(0), route.get(2), rtr.getOrderId(), incrementalId.getAndIncrement()+""); 
						} else {
							// this should not be possible, perhaps we don't know about that turntable yet, thus we cant contact it, thus an error
							String msg = String.format("Unable to establish transport module for Position %s for request %s", route.get(1), rtr.getOrderId());
							log.warning(msg);
							this.getSender().tell(new RegisterTransportRequestStatusResponse(rtr, RegisterTransportRequestStatusResponse.ResponseType.MISSING_TRANSPORT_MODULE ,msg), self);
							return null;
						}
					});
					if (tmr1_4.isPresent()) {
						tmrRoot = act2_4.map(actor -> { 
							if (tmut.isTransportModule(actor)) { // from turntable at pos1 to turntable at pos2
								return new TransportModuleRequest(actor, route.get(1), route.get(3), rtr.getOrderId(), incrementalId.getAndIncrement()+"", tmr1_4.get()); 
							} else {
								// this should not be possible, perhaps we don't know about that turntable yet, thus we cant contact it, thus an error
								String msg = String.format("Unable to establish transport module for Position %s for request %s", route.get(2), rtr.getOrderId());
								log.warning(msg);
								this.getSender().tell(new RegisterTransportRequestStatusResponse(rtr, RegisterTransportRequestStatusResponse.ResponseType.MISSING_TRANSPORT_MODULE ,msg), self);
								return null;
							}
						});
					} else {
						tmrRoot = Optional.empty();
					}
					break;
				default:
					String msg = String.format("Unable to process route between %s with Position %s and %s with Position %s due to unsupported routesize %s",
							rtr.getSource().getId(), sourceP.getPos(), rtr.getDestination().getId(), destP.getPos(), routeSize);
					log.warning(msg);					
					this.getSender().tell(new RegisterTransportRequestStatusResponse(rtr, RegisterTransportRequestStatusResponse.ResponseType.UNSUPPORTED_ENDPOINT_POSITIONS ,msg), self);
					break;
				}
				tmrRoot.ifPresent(tmr -> {
					requestQueue.add(tmr);					
					queuedRequests.put(rtr.getOrderId(), rtr);
					String msg = String.format("Queuing TMR from %s to %s for Order %s with RequestId %s", sourceP.getPos(), destP.getPos(), tmr.getOrderId(), tmr.getRequestId());
					this.getSender().tell(new RegisterTransportRequestStatusResponse(rtr, RegisterTransportRequestStatusResponse.ResponseType.QUEUED ,msg), self);		
					log.info(msg);
					matchRequestsToModules(); // lets check which requests we can fulfill
				});
			} catch (RoutingException e) {
				String msg = String.format("Unable to establish route between %s with Position %s and %s with Position %s due to %s", 
						rtr.getSource().getId(), sourceP.getPos(), rtr.getDestination().getId(), destP.getPos(), e.getMessage());
				log.warning(msg);
				this.getSender().tell(new RegisterTransportRequestStatusResponse(rtr, RegisterTransportRequestStatusResponse.ResponseType.NO_ROUTE ,msg), self);
			}
		} else {
			String msg = String.format("Unable to resolve positions for actors %s and/or %s", rtr.getSource().getId(), rtr.getDestination().getId());
			log.warning(msg);
			this.getSender().tell(new RegisterTransportRequestStatusResponse(rtr, RegisterTransportRequestStatusResponse.ResponseType.NO_ROUTE ,msg), self);
		}
	}


	private void matchRequestsToModules() {
		//iterate through request list and see which request can be fulfilled based on module state
		// for each request we iterate through linked TMRs if all individual/part requests are fulfillable
		List<TransportModuleRequest> fulfillables = this.requestQueue.stream().filter(tmr -> {			
			boolean fulfillable = false;
			while(tmr != null) {
				Optional<Boolean> avail = tmut.getUsageState(tmr.getExecutor()).map(state -> state.equals(TransportModuleOrderMappingStatus.AllocationState.AVAILABLE) ? true : null); 
				// optional isPresent and always true if available, otherwise null
				if (avail.isPresent()) {
					fulfillable = true;
					tmr = tmr.getSubsequentRequest().orElse(null);
				} else {
					fulfillable = false;
					break;
				}
			}
			return fulfillable;
		}).collect(Collectors.toList());		
		// contact all for transport, mark them internally
		Optional<TransportModuleRequest> firstAllocated = fulfillables.stream().sequential() // ensure we do this one by one
			.map(tmr -> {
				List<TransportModuleRequest> allocatedModules = new ArrayList<>();
				Optional<TransportModuleRequest> optTMR = Optional.ofNullable(tmr);
				// we iterate through list/sequence of modules on our path and call every one, to start, if an error, undo started ones
				while (optTMR.isPresent()) {
					try {
						tmut.requestTransportModuleForOrder(optTMR.get().getExecutor(), optTMR.get().getOrderId(), optTMR.get().getRequestId());
						allocatedModules.add(optTMR.get());
						// now send request to actor
						optTMR.get().getExecutor().getAkkaActor().tell(optTMR.get(), this.self);
						optTMR = optTMR.get().getSubsequentRequest();
					} catch (MachineOrderMappingStatusLifecycleException e) { // undo allocation
						log.warning("Tried to allocated available transport module but was rejected: "+e.getMessage());		
						// this is based on local information thus no feedback from actual module considered here			
						allocatedModules.stream().forEach(tmrPrev -> {
							try {
								//TODO: tell Transport Module Actor: tmrPrev.getExecutor().getAkkaActor().tell(optTMR.get(), this.self);
								tmut.unrequestTransportModule(tmrPrev.getExecutor(), tmrPrev.getOrderId());
							} catch (MachineOrderMappingStatusLifecycleException e1) {
								log.error("Tried to deallocated transport module but was rejected: "+e1.getMessage());
								// This should really never happen, TODO: further exception handling
							}
						});
						allocatedModules.clear();
						break;
					}
				}
				if (allocatedModules.size() > 0) {
					return tmr; 
				}
				else
					return null;
			})
			.filter(tmr -> tmr != null)
			.findFirst();
		// then move to allocated list, remove there when complete
		firstAllocated.ifPresent(tmr -> {
			requestQueue.remove(tmr);
			allocatedQueue.add(tmr);
			String msg = String.format("Issued Transport Request(s) to Transport Module(s) for Order %s", tmr.getOrderId());
			log.info(msg);
			// inform requestor about new status
			Optional.ofNullable(queuedRequests.get(tmr.getOrderId())).ifPresent(rtr -> {
				rtr.getRequestor().tell(new RegisterTransportRequestStatusResponse(rtr, ResponseType.ISSUED, msg), self);
			});
		});				
	}

	//	// handle Message to register transport among machines (including input and output station)
	//	private void handleNewIncomingRequest(RegisterTransportRequest rtr) {
	//		// for now we assume, that if a request comes in, the source/dest are ready for transport
	//		Position sourceP = dns.getPositionForActor(rtr.getSource());
	//		Position destP = dns.getPositionForActor(rtr.getDestination());
	//		if (sourceP != TransportRoutingInterface.UNKNOWN_POSITION && destP != TransportRoutingInterface.UNKNOWN_POSITION) {
	//			try {
	//				List<Position> route = routing.calculateRoute(sourceP, destP);
	//				// check if all transport modules are available to fulfull route 
	//				TransportModuleRequest tmrRoot = null;
	//				Integer routeSize = route.size();
	//				switch(routeSize) {
	//				case 2:			//move something onto or off turntable or between turntables
	//					Optional<AkkaActorBackedCoreModelAbstractActor> act0_2 = dns.getActorForPosition(route.get(0));
	//					Optional<AkkaActorBackedCoreModelAbstractActor> act1_2 = dns.getActorForPosition(route.get(1));
	//					TransportModuleRequest tmr0_2 = null;
	//					act0_2.ifPresent(actor -> { if (tmut.isTransportModule(actor)){ // from a turntable at pos1 to pos2
	//							tmr0_2 = new TransportModuleRequest(actor, route.get(0), route.get(1), rtr.getOrderId(), incrementalId.getAndIncrement()+""); 
	//						}});
	//					act1_2.ifPresent(actor -> { if (tmut.isTransportModule(actor)) { // from turntable (if tmr0 != null)  or some machine (if tmr0 == null) to turntable pos2
	//							tmrRoot = new TransportModuleRequest(actor, route.get(0), route.get(1), rtr.getOrderId(), incrementalId.getAndIncrement()+"", tmr0_2); // tmr is NULL, no problem
	//						} else {
	//							tmrRoot = tmr0_2;
	//						}
	//					});
	//					break;
	//				case 3: //move something through single turntable (perhaps from a turntable, perhaps to another turntable or both, middle one always a turntable
	//					Optional<AkkaActorBackedCoreModelAbstractActor> act0_3 = dns.getActorForPosition(route.get(0));
	//					Optional<AkkaActorBackedCoreModelAbstractActor> act1_3 = dns.getActorForPosition(route.get(1));
	//					Optional<AkkaActorBackedCoreModelAbstractActor> act2_3 = dns.getActorForPosition(route.get(2));
	//					TransportModuleRequest tmr0_3 = null;
	//					TransportModuleRequest tmr1_3 = null;
	//					act0_3.ifPresent(actor -> { if (tmut.isTransportModule(actor)){ // from a turntable at pos1 to turntable at pos2 for final destination pos 3
	//							tmr0_3 = new TransportModuleRequest(actor, route.get(0), route.get(1), rtr.getOrderId(), incrementalId.getAndIncrement()+""); 
	//						}});
	//					act1_3.ifPresent(actor -> { if (tmut.isTransportModule(actor)) { // must be a turntable
	//							tmr1_3 = new TransportModuleRequest(actor, route.get(0), route.get(2), rtr.getOrderId(), incrementalId.getAndIncrement()+"", tmr0_3); // if tmr0_3 is NULL, no problem
	//						} else {
	//							// this should not be possible, perhaps we don't know it yet, thus we cant contact it, thus an error
	//							// TODO return unsuccessful request generation
	//							log.warning(String.format("Unable to establish router at Position %s for request %s", route.get(1), rtr.getOrderId()));
	//							break;
	//						}
	//					});
	//					act2_3.ifPresent(actor -> { if (tmut.isTransportModule(actor)) { // from turntable at pos1 to turntable at pos2
	//							tmrRoot = new TransportModuleRequest(actor, route.get(2), route.get(2), rtr.getOrderId(), incrementalId.getAndIncrement()+"", tmr1_3); // trm1_3 is never null here
	//						} else {
	//							tmrRoot = tmr1_3;
	//						}
	//					});
	//					break;
	//				case 4:	// same as for case 3 but another stop in between, --> TODO: generalize this case statements
	//					Optional<AkkaActorBackedCoreModelAbstractActor> act0_4 = dns.getActorForPosition(route.get(0));
	//					Optional<AkkaActorBackedCoreModelAbstractActor> act1_4 = dns.getActorForPosition(route.get(1));
	//					Optional<AkkaActorBackedCoreModelAbstractActor> act2_4 = dns.getActorForPosition(route.get(2));
	//					Optional<AkkaActorBackedCoreModelAbstractActor> act3_4 = dns.getActorForPosition(route.get(3));
	//					TransportModuleRequest tmr0_4 = null;
	//					TransportModuleRequest tmr1_4 = null;
	//					TransportModuleRequest tmr2_4 = null;
	//					act0_4.ifPresent(actor -> { if (tmut.isTransportModule(actor)){ // from a turntable at pos1 to turntable at pos2 for final destination pos 3
	//							tmr0_4 = new TransportModuleRequest(actor, route.get(0), route.get(1), rtr.getOrderId(), incrementalId.getAndIncrement()+""); 
	//						}});
	//					act1_4.ifPresent(actor -> { if (tmut.isTransportModule(actor)) { // must be a turntable
	//							tmr1_4 = new TransportModuleRequest(actor, route.get(0), route.get(2), rtr.getOrderId(), incrementalId.getAndIncrement()+"", tmr0_4); // if tmr0_3 is NULL, no problem
	//						} else {
	//							// this should not be possible, perhaps we don't know it yet, thus we cant contact it, thus an error
	//							// TODO return unsuccessful request generation
	//							log.warning(String.format("Unable to establish router at Position %s for request %s", route.get(1), rtr.getOrderId()));
	//							break;
	//						}
	//					});
	//					act2_4.ifPresent(actor -> { if (tmut.isTransportModule(actor)) { // from turntable at pos1 to turntable at pos2
	//							tmrRoot = new TransportModuleRequest(actor, route.get(2), route.get(2), rtr.getOrderId(), incrementalId.getAndIncrement()+"", tmr1_4); // trm1_3 is never null here
	//						} else {
	//							tmrRoot = tmr1_4;
	//						}
	//					});
	//					break;
	//				default:
	//					// TODO return unsuccessful route generation
	//					log.warning(String.format("Unable to establish route between s% with Position %s and %s with Position %s due to unsupported routesize %s", 
	//							rtr.getSource().getId(), sourceP.getPos(), rtr.getDestination().getId(), destP.getPos(), routeSize));
	//					break;
	//				}
	//				
	//			} catch (RoutingException e) {
	//				log.warning(String.format("Unable to establish route between s% with Position %s and %s with Position %s due to %s", 
	//						rtr.getSource().getId(), sourceP.getPos(), rtr.getDestination().getId(), destP.getPos(), e.getMessage()));
	//				//TODO return unsuccessful request instead of successfull registering
	//			}
	//		} else {
	//			log.warning(String.format("Unable to resolve positions for actors %s and/or %s", rtr.getSource().getId(), rtr.getDestination().getId()));
	//			//TODO return unsuccessful request instead of successfull registering
	//		}
	//	}

}
