package internal.node;

import org.ros.concurrent.SharedScheduledExecutorService;
import org.ros.internal.node.DefaultNode;
import org.ros.node.Node;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeFactory;
import org.ros.node.NodeListener;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.ScheduledExecutorService;

public class FIABNodeFactory {

    private final ScheduledExecutorService scheduledExecutorService;

    public FIABNodeFactory(ScheduledExecutorService scheduledExecutorService) {
        this.scheduledExecutorService = new SharedScheduledExecutorService(scheduledExecutorService);
    }

    public DefaultNode newNode(NodeConfiguration nodeConfiguration, Collection<NodeListener> listeners) {
        return new DefaultNode(nodeConfiguration, listeners, this.scheduledExecutorService);
    }

    public DefaultNode newNode(NodeConfiguration nodeConfiguration) {
        return this.newNode(nodeConfiguration, new LinkedList());
    }
}
