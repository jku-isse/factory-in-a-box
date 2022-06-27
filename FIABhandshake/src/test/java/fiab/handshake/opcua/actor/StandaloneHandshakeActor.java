package fiab.handshake.opcua.actor;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import fiab.core.capabilities.meta.OPCUACapabilitiesAndWiringInfoBrowsenames;
//import fiab.handshake.fu.client.ClientSideHandshakeFU;
import fiab.handshake.fu.server.ServerSideHandshakeFU;
import fiab.opcua.server.OPCUABase;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;

public class StandaloneHandshakeActor extends AbstractActor {

    public static Props props(OPCUABase opcuaBase, String rootName, boolean isServer) {
        return Props.create(StandaloneHandshakeActor.class, () -> new StandaloneHandshakeActor(opcuaBase, null,rootName, isServer));
    }

    public static Props props(OPCUABase opcuaBase, ActorRef parentActor,String rootName, boolean isServer) {
        return Props.create(StandaloneHandshakeActor.class, () -> new StandaloneHandshakeActor(opcuaBase, parentActor,rootName, isServer));
    }

    StandaloneHandshakeActor(OPCUABase opcuaBase, ActorRef parentActor, String rootName, boolean isServer) {
        UaFolderNode root = opcuaBase.prepareRootNode();
        if(isServer){
            UaFolderNode hsRootNode = opcuaBase.generateFolder(root, rootName, "ServerHandshake");
            String fuPrefix = rootName+"/"+"ServerHandshake";
            ActorRef parent = parentActor == null ? self() : parentActor;
            new ServerSideHandshakeFU(opcuaBase, hsRootNode, fuPrefix, parent, context(),
                    "DefaultServerSideHandshake", OPCUACapabilitiesAndWiringInfoBrowsenames.IS_PROVIDED, true);
        }else{
            UaFolderNode hsRootNode = opcuaBase.generateFolder(root, rootName, "ClientHandshake");
            String fuPrefix = rootName+"/"+"ClientHandshake";
            //new ClientSideHandshakeFU(opcuaBase, hsRootNode, fuPrefix, self(), context(),
            //        "DefaultClientSideHandshake", OPCUACapabilitiesAndWiringInfoBrowsenames.IS_REQUIRED, true);
        }
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create().build();
    }
}
