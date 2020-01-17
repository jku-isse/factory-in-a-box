package fiab.mes.mockactors.iostation;

import java.util.concurrent.atomic.AtomicInteger;

import ActorCoreModel.Actor;
import ProcessCore.AbstractCapability;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import fiab.mes.eventbus.InterMachineEventBus;
import fiab.mes.machine.actor.iostation.BasicIOStationActor;
import fiab.mes.mockactors.MockMachineWrapper;
import fiab.mes.transport.handshake.HandshakeProtocol;

public class MockIOStationFactory {

	public static MockIOStationFactory getMockedInputStation(ActorSystem system, ActorSelection eventBusByRef, boolean doAutoReload, int ipId) {
		return new MockIOStationFactory(system, true, eventBusByRef, doAutoReload, ipId);
	}
	
	public static MockIOStationFactory getMockedOutputStation(ActorSystem system, ActorSelection eventBusByRef, boolean doAutoReload, int ipId) {
		return new MockIOStationFactory(system, false, eventBusByRef, doAutoReload, ipId);
	}

	
	public static String WRAPPER_POSTFIX = "Wrapper";
	
	public ActorRef machine;
	public ActorRef wrapper;
	public InterMachineEventBus intraEventBus;
	public AbstractCapability capability;
	public Actor model;
	//private static AtomicInteger actorCount = new AtomicInteger();
	
	private MockIOStationFactory(ActorSystem system, boolean isInputStation, ActorSelection eventBusByRef, boolean doAutoReload, int ipId) {
		model = getDefaultIOStationActor(isInputStation, ipId);
		intraEventBus = new InterMachineEventBus();
		wrapper = system.actorOf(MockIOStationWrapper.props(intraEventBus, isInputStation, doAutoReload), model.getActorName()+WRAPPER_POSTFIX);
		MockIOStationWrapperDelegate delegate = new MockIOStationWrapperDelegate(wrapper);
		capability = isInputStation ? HandshakeProtocol.getInputStationCapability() : HandshakeProtocol.getOutputStationCapability();
		machine = system.actorOf(BasicIOStationActor.props(eventBusByRef, capability, model, delegate, intraEventBus), model.getActorName());
	}
	
	public Actor getDefaultIOStationActor(boolean isInputStation, int id) {
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
}
