package fiab.mes.machine;

import ActorCoreModel.Actor;
import akka.actor.ActorRef;

public class AkkaActorBackedCoreModelAbstractActor {

	protected String id;
	protected Actor modelActor;
	protected ActorRef akkaActor;
	
	public AkkaActorBackedCoreModelAbstractActor(String id, Actor modelActor, ActorRef akkaActor) {
		super();
		this.id = id;
		this.modelActor = modelActor;
		this.akkaActor = akkaActor;
	}

	public Actor getModelActor() {
		return modelActor;
	}

	public ActorRef getAkkaActor() {
		return akkaActor;
	}
	
	public String getId() {
		return id;
	}
	
	
}
