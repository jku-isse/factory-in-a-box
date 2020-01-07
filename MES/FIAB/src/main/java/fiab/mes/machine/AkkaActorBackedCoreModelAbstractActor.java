package fiab.mes.machine;

import ActorCoreModel.Actor;
import akka.actor.ActorRef;

public class AkkaActorBackedCoreModelAbstractActor {

	protected String id;
	protected Actor modelActor;
	protected ActorRef akkaActor;
	
	// We only use modelActor.URI from modelActor (and id and akkaActor) for equals and hashCode!
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((akkaActor == null) ? 0 : akkaActor.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((modelActor != null && modelActor.getUri() != null) ? modelActor.getUri().hashCode() : 0);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AkkaActorBackedCoreModelAbstractActor other = (AkkaActorBackedCoreModelAbstractActor) obj;
		if (akkaActor == null) {
			if (other.akkaActor != null)
				return false;
		} else if (!akkaActor.equals(other.akkaActor))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (modelActor == null) {
			if (other.modelActor != null)
				return false;
		} else if (!modelActor.getUri().equals(other.modelActor.getUri()))
			return false;
		return true;
	}
	
	
}
