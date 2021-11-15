package fiab.mes.mockactors.transport;

import ActorCoreModel.Actor;

public class CoreModelActorProvider {
	public static Actor getDefaultTransportModuleModelActor(int lastIPAddrPos, int portOffset) {
		Actor actor = ActorCoreModel.ActorCoreModelFactory.eINSTANCE.createActor();
		actor.setID("MockTurntableActor"+lastIPAddrPos);
		actor.setActorName("MockTurntableActor"+lastIPAddrPos);
		actor.setDisplayName("MockTurntableActor"+lastIPAddrPos);
		actor.setUri("http://192.168.0."+lastIPAddrPos+":484"+portOffset+"/MockTurntableActor"+lastIPAddrPos);
		return actor;
	}
	
}
