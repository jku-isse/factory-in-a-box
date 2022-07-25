package client;

import internal.node.FIABAbstractNodeMain;
import org.ros.namespace.GraphName;

public class ClientNode extends FIABAbstractNodeMain {

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("fiab_ros/client");
    }

}
