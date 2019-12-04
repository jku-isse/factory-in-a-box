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

	public static MockIOStationFactory getMockedInputStation(ActorSystem system, ActorSelection eventBusByRef) {
		return new MockIOStationFactory(system, true, eventBusByRef);
	}
	
	public static MockIOStationFactory getMockedOutputStation(ActorSystem system, ActorSelection eventBusByRef) {
		return new MockIOStationFactory(system, false, eventBusByRef);
	}

	
	public ActorRef machine;
	public ActorRef wrapper;
	public InterMachineEventBus intraEventBus;
	public AbstractCapability capability;
	public Actor model;
	private static AtomicInteger actorCount = new AtomicInteger();
	
	private MockIOStationFactory(ActorSystem system, boolean isInputStation, ActorSelection eventBusByRef) {
		model = getDefaultIOStationActor(isInputStation);
		intraEventBus = new InterMachineEventBus();
		wrapper = system.actorOf(MockIOStationWrapper.props(intraEventBus, isInputStation), model.getActorName()+"Wrapper");
		MockIOStationWrapperDelegate delegate = new MockIOStationWrapperDelegate(wrapper);
		capability = isInputStation ? HandshakeProtocol.getInputStationCapability() : HandshakeProtocol.getOutputStationCapability();
		machine = system.actorOf(BasicIOStationActor.props(eventBusByRef, capability, model, delegate, intraEventBus), model.getActorName());
	}
	
	public Actor getDefaultIOStationActor(boolean isInputStation) {
		int id = actorCount.getAndIncrement();
		String type = isInputStation ? "Input" : "Output";
		Actor actor = ActorCoreModel.ActorCoreModelFactory.eINSTANCE.createActor();
		actor.setID(type+"StationActor"+id);
		actor.setActorName(type+"StationActor"+id);
		actor.setDisplayName(type+"Actor"+id);
		actor.setUri("http://fiab.actors/Mock"+type+"Actor"+id);
		return actor;
	}
}
