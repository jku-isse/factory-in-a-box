package internal;

import org.ros.internal.node.DefaultNode;
import org.ros.node.Node;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeListener;
import org.ros.node.NodeMain;

import java.util.Collection;
import java.util.concurrent.ScheduledExecutorService;

public interface FIABNodeMainExecutor {

    ScheduledExecutorService getScheduledExecutorService();

    DefaultNode execute(NodeMain var1, NodeConfiguration var2, Collection<NodeListener> var3);

    DefaultNode execute(NodeMain var1, NodeConfiguration var2);

    void shutdownNodeMain(NodeMain var1);

    void shutdown();
}
