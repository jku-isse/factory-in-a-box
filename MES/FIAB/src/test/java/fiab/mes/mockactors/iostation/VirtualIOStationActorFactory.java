package fiab.mes.mockactors.iostation;

import java.time.Duration;

import ActorCoreModel.Actor;
import ProcessCore.AbstractCapability;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.core.capabilities.handshake.HandshakeCapability;
import fiab.core.capabilities.handshake.IOStationCapability;
import fiab.core.capabilities.handshake.HandshakeCapability.ServerSideStates;
import fiab.handshake.actor.LocalEndpointStatus;
import fiab.handshake.actor.messages.HSServerSideStateMessage;
import fiab.machine.iostation.IOStationServerHandshakeActor;
import fiab.mes.eventbus.InterMachineEventBus;
import fiab.mes.machine.actor.iostation.BasicIOStationActor;
import fiab.mes.machine.msg.IOStationStatusUpdateEvent;

public class VirtualIOStationActorFactory {

	public static VirtualIOStationActorFactory getMockedInputStation(ActorSystem system, ActorSelection eventBusByRef, boolean doAutoReload, int ipId) {
		return new VirtualIOStationActorFactory(system, true, eventBusByRef, doAutoReload, ipId);
	}
	
	public static VirtualIOStationActorFactory getMockedOutputStation(ActorSystem system, ActorSelection eventBusByRef, boolean doAutoReload, int ipId) {
		return new VirtualIOStationActorFactory(system, false, eventBusByRef, doAutoReload, ipId);
	}

	
	public static String WRAPPER_POSTFIX = "Wrapper";
	
	public ActorRef machine;
	public ActorRef wrapper;
	public InterMachineEventBus intraEventBus;
	public AbstractCapability capability;
	public Actor model;
	//private static AtomicInteger actorCount = new AtomicInteger();
	public static boolean doAutoComplete = true;
	
	private VirtualIOStationActorFactory(ActorSystem system, boolean isInputStation, ActorSelection eventBusByRef, boolean doAutoReload, int ipId) {
		model = getDefaultIOStationActor(isInputStation, ipId);
		intraEventBus = new InterMachineEventBus();
		ActorRef parent = system.actorOf(ParentActor.props(isInputStation, doAutoReload, intraEventBus), model.getActorName()+"PARENT");
		wrapper = isInputStation ? system.actorOf(IOStationServerHandshakeActor.propsForInputstation(parent, doAutoComplete, null), model.getActorName()+WRAPPER_POSTFIX)
				: system.actorOf(IOStationServerHandshakeActor.propsForOutputstation(parent, doAutoComplete, null), model.getActorName()+WRAPPER_POSTFIX);
		MockIOStationWrapperDelegate delegate = new MockIOStationWrapperDelegate(wrapper);
		capability = isInputStation ? IOStationCapability.getInputStationCapability() : IOStationCapability.getOutputStationCapability();
		machine = system.actorOf(BasicIOStationActor.props(eventBusByRef, capability, model, delegate, intraEventBus), model.getActorName());
	}
	
	public static Actor getDefaultIOStationActor(boolean isInputStation, int id) {
		//int id = actorCount.getAndIncrement();
		//int id = isInputStation ? 34 : 35; //at IP/Pos 34 is inputstation, at IP/Pos3 35 is outputstation
		String type = isInputStation ? "Input" : "Output";
		Actor actor = ActorCoreModel.ActorCoreModelFactory.eINSTANCE.createActor();
		actor.setID(type+"StationActor"+id);
		actor.setActorName(type+"StationActor"+id);
		actor.setDisplayName(type+"Actor"+id);
		actor.setUri("http://192.168.0."+id+"/"+type+"Actor"+id);
		return actor;
	}
	
	public static class ParentActor extends AbstractActor {
		
		private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
		
		boolean doAutoReload;
		boolean isInputStation;
		ActorRef child;
		ActorRef self;
		InterMachineEventBus bus;
		
		static public Props props(boolean isInputStation, boolean doAutoReload, InterMachineEventBus bus) {	    
			return Props.create(ParentActor.class, () -> new ParentActor(isInputStation, doAutoReload, bus));
		}
		
		public ParentActor(boolean isInputStation, boolean doAutoReload, InterMachineEventBus bus) {
				this.isInputStation = isInputStation;
				this.doAutoReload = doAutoReload;
				this.bus = bus;
				this.self = getSelf();
		}
		
		@Override
		public Receive createReceive() {
			
			return receiveBuilder()	.match(HSServerSideStateMessage.class,msg->{
						ServerSideStates req = msg.getBody();
						log.info(req.toString());
						bus.publish(new IOStationStatusUpdateEvent("", "Mock Endpoint has new State", req));
						if (req.equals(ServerSideStates.COMPLETE) && doAutoReload) { //we auto reload here
							child = getSender();
							reloadPallet();
						}				
						})			
					.match(ServerSideStates.class, req -> {
						log.info(req.toString());
						bus.publish(new IOStationStatusUpdateEvent("", "Mock Endpoint has new State", req));
						if (req.equals(ServerSideStates.COMPLETE) && doAutoReload) { //we auto reload here
								child = getSender();
								reloadPallet();
							}
						})
					.match(LocalEndpointStatus.LocalServerEndpointStatus.class, les -> {//ignore
						})
					.build();		
		}
		
		private void reloadPallet() {
			//tell handshake that the pallet is loaded if inputstation, otherwise setempty
				context().system()
		    	.scheduler()
		    	.scheduleOnce(Duration.ofMillis(1000), 
		    			 new Runnable() {
		            @Override
		            public void run() {
		            		if (isInputStation) {
		        				child.tell(HandshakeCapability.StateOverrideRequests.SetLoaded, self); 
		        			} else {
		        				child.tell(HandshakeCapability.StateOverrideRequests.SetEmpty, self); 
		        			}
		            }
		          }, context().system().dispatcher());
		}
		
	}
	
	
}
