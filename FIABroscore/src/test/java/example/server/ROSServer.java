package example.server;

import internal.DefaultFIABNodeMainExecutor;
import internal.FIABNodeMainExecutor;
import internal.FIABRosLoader;
import internal.node.FIABAbstractNodeMain;
import org.ros.RosCore;
import org.ros.exception.RosRuntimeException;
import org.ros.internal.node.DefaultNode;
import org.ros.node.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * This class is currently only used for local testing. We do not want to use this.
 * A real robot running ROS will be the actual server
 */
public class ROSServer {

    /**
     * Run this method to start a local server for manual testing
     * @param args command line args
     */
    public static void main(String[] args) {
        ROSServer server = ROSServer.newInstanceWithMaster(ServerNode.class, ROSServer.DEFAULT_PORT);
        //ROSServer server = ROSServer.newInstance(ServerNode.class, ROSServer.DEFAULT_PORT);   //Start server without master node
    }

    /**
     * Starts a ROS Server node with local master node
     * @param nodeClass class of the server
     * @param port port of the server. Default port for ROS is 11311
     * @return ROSServer
     */
    public static ROSServer newInstanceWithMaster(Class<? extends FIABAbstractNodeMain> nodeClass, int port){
        ROSServer rosServer = new ROSServer(nodeClass, port, true);
        try {
            rosServer.initServerNode().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return rosServer;
    }

    /**
     * Starts a ROS Server node with already running local master node
     * @param nodeClass class of the server
     * @param port port of the server. Default port for ROS is 11311
     * @return ROSServer
     */
    public static ROSServer newInstance(Class<? extends FIABAbstractNodeMain> nodeClass, int port){
        ROSServer rosServer = new ROSServer(nodeClass, port, false);
        try {
            rosServer.initServerNode().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return rosServer;
    }

    public final static int DEFAULT_PORT = 11311;

    private final NodeConfiguration nodeConfiguration;
    private NodeMain nodeMain;
    private FIABNodeMainExecutor nodeMainExecutor;
    private DefaultNode node;

    private ROSServer(Class<? extends FIABAbstractNodeMain> nodeClass, int port, boolean isMaster) {
        if (isMaster) {
            RosCore rosCore = RosCore.newPublic(port);
            rosCore.start();
        }
        FIABRosLoader loader = new FIABRosLoader();
        System.out.println("Loading node class: " + nodeClass);
        this.nodeConfiguration = loader.build();

        this.nodeMain = null;
        try {
            nodeMain = loader.loadClass(nodeClass);
        } catch (ClassNotFoundException e) {
            throw new RosRuntimeException("Unable to locate node: " + nodeClass, e);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RosRuntimeException("Unable to instantiate node: " + nodeClass, e);
        }

        if (nodeMain != null) {
            this.nodeMainExecutor = DefaultFIABNodeMainExecutor.newDefault();
        }
    }

    private CompletableFuture<Void> initServerNode() {
        return CompletableFuture.supplyAsync(() -> nodeMainExecutor.execute(nodeMain, nodeConfiguration))
                .thenAccept(n -> this.node = n);
    }

    /**
     * Shuts down the Server
     */
    public void shutdownServer(){
        this.nodeMainExecutor.shutdown();
    }
}
