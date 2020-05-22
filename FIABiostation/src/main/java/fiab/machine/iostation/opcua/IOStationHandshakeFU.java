package fiab.machine.iostation.opcua;

import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import fiab.core.capabilities.meta.OPCUACapabilitiesAndWiringInfoBrowsenames;
import fiab.handshake.fu.server.ServerSideHandshakeFU;
import fiab.machine.iostation.IOStationServerHandshakeActor;
import fiab.opcua.server.OPCUABase;

public class IOStationHandshakeFU {

public static class InputStationHandshakeFU extends ServerSideHandshakeFU {

	public InputStationHandshakeFU(OPCUABase base, UaFolderNode root, String fuPrefix, ActorRef parentActor,
			ActorContext context, String capInstId, boolean exposeInternalControl, boolean isInputStation) {
		super(base, root, fuPrefix, parentActor, context, capInstId, OPCUACapabilitiesAndWiringInfoBrowsenames.IS_PROVIDED, exposeInternalControl);
	}

	@Override
	protected void setupActor() {
		localClient = context.actorOf(IOStationServerHandshakeActor.propsForInputstation(parentActor, true, this), capInstId);
	}
}

public static class OutputStationHandshakeFU extends ServerSideHandshakeFU {

	public OutputStationHandshakeFU(OPCUABase base, UaFolderNode root, String fuPrefix, ActorRef parentActor,
			ActorContext context, String capInstId, boolean exposeInternalControl, boolean isInputStation) {
		super(base, root, fuPrefix, parentActor, context, capInstId, OPCUACapabilitiesAndWiringInfoBrowsenames.IS_PROVIDED, exposeInternalControl);
	}

	@Override
	protected void setupActor() {
		localClient = context.actorOf(IOStationServerHandshakeActor.propsForOutputstation(parentActor, true, this), capInstId);
	}
}

}
