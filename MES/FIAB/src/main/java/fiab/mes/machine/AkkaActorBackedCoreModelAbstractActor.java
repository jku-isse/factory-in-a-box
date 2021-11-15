package fiab.mes.machine;

import ActorCoreModel.Actor;
import akka.actor.ActorRef;

public class AkkaActorBackedCoreModelAbstractActor {

	protected String id;
	protected Actor modelActor;
	protected ActorRef akkaActor;
	
	// We only use modelActor.URI from modelActor!
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
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((modelActor == null) ? 0 : modelActor.hashCode());
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
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (modelActor == null) {
			if (other.modelActor != null)
				return false;
		} else if (modelActor.getUri() == null) {
			if (other.modelActor.getUri() != null)
				return false;
		} else if (!modelActor.getUri().equals(other.modelActor.getUri()))
			return false;
		return true;
	}

	@Override
	public String toString() {
		String uri = (modelActor != null && modelActor.getUri() != null) ? modelActor.getUri() : "No URI";
		String path = akkaActor != null ? akkaActor.path().toString() : "No Path";
		return "AkkaActorBackedCoreModelAbstractActor [id=" + id + ", URI=" + uri + ", akkaActor="
				+ path + "]";
	}


	
	
}
