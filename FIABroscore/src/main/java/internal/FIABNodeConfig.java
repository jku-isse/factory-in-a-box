package internal;

import org.ros.node.NodeConfiguration;

import java.net.URI;

public class FIABNodeConfig {

    /**
     * Create a {@link NodeConfiguration}
     *
     * @param rosHostIp    the ip of the computer running the node
     * @param nodeName     the name of the node
     * @param rosMasterUri the uri of the rosMaster that this node should connect to
     *
     * @return the completed {@link NodeConfiguration} with the arguments provided
     */
    public static NodeConfiguration createNodeConfiguration(final String rosHostIp, final String nodeName, final URI rosMasterUri) {
        //Create a node configuration
        final NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(rosHostIp);
        nodeConfiguration.setNodeName(nodeName);
        nodeConfiguration.setMasterUri(rosMasterUri);
        return nodeConfiguration;
    }
}
