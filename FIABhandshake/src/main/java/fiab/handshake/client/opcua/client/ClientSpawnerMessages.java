package fiab.handshake.client.opcua.client;

import fiab.core.capabilities.wiring.WiringInfo;
import fiab.handshake.client.opcua.RemoteServerHandshakeNodeIds;

public class ClientSpawnerMessages {

    public static class CreateNewClient{
        private final WiringInfo wiringInfo;
        public CreateNewClient(WiringInfo nodeIds){
            this.wiringInfo = nodeIds;
        }

        public WiringInfo getNodeIds() {
            return wiringInfo;
        }
    }

    public static class CancelClientCreation{

    }

    //We already have the NodeIds from the wiringInfo
    /*static class ConnectToHandshakeNodes{
        private final ServerHandshakeNodeIds nodeIds;
        public ConnectToHandshakeNodes(ServerHandshakeNodeIds nodeIds){
            this.nodeIds = nodeIds;
        }

        public ServerHandshakeNodeIds getNodeIds() {
            return nodeIds;
        }
    }*/

    public static class ClientCreated{
        private RemoteServerHandshakeNodeIds nodeIds;
        public ClientCreated(RemoteServerHandshakeNodeIds nodeIds){
            this.nodeIds = nodeIds;
        }

        public RemoteServerHandshakeNodeIds getServerNodeIds() {
            return nodeIds;
        }
    }

    public static class ClientCreationCancelled{

    }

    public static class ClientCreationFailed{

    }
}
