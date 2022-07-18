package example.server;

import client.ROSClient;
import internal.DefaultFIABNodeMainExecutor;
import internal.FIABNodeMainExecutor;
import internal.FIABRosLoader;
import internal.node.FIABAbstractNodeMain;
import internal.node.FIABNodeMain;
import org.ros.RosCore;
import org.ros.exception.RosRuntimeException;
import org.ros.internal.loader.CommandLineLoader;
import org.ros.internal.node.DefaultNode;
import org.ros.node.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * This class is currently only used for local testing. We do not want to use this.
 * A real robot running ROS will be the actual server
 */
public class ROSServer {

    /**
     * Run this method to start a local server for manual testing
     * @param args
     */

    public static void main(String[] args) {
        ROSServer server = ROSServer.newInstanceWithMaster(ServerNode.class, ROSServer.DEFAULT_PORT);
    }

    public static ROSServer newInstanceWithMaster(Class<? extends FIABAbstractNodeMain> nodeClass, int port){
        ROSServer rosServer = new ROSServer(nodeClass, port, true);
        try {
            rosServer.initClientNode().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return rosServer;
    }

    public static ROSServer newInstance(Class<? extends FIABAbstractNodeMain> nodeClass, int port){
        ROSServer rosServer = new ROSServer(nodeClass, port, false);
        try {
            rosServer.initClientNode().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return rosServer;
    }

    public final static int DEFAULT_PORT = 11311;

    private final NodeConfiguration nodeConfiguration;
    private FIABNodeMain nodeMain;
    private FIABNodeMainExecutor nodeMainExecutor;
    private DefaultNode node;

    private ROSServer(Class<? extends FIABAbstractNodeMain> nodeClass, int port, boolean isMaster) {
        //String serverName = "nodes.Server";

        //RosCore rosCore = RosCore.newPublic(11311);
        if (isMaster) {
            RosCore rosCore = RosCore.newPublic(port);
//            rosCore.start();

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
            //nodeMainExecutor.execute(nodeMain, nodeConfiguration);
        }
    }

    private CompletableFuture<Void> initClientNode() {
        return CompletableFuture.supplyAsync(() -> nodeMainExecutor.execute(nodeMain, nodeConfiguration))
                .thenAccept(n -> this.node = n);
    }

    public void shutdownServer(){
        this.nodeMainExecutor.shutdown();
    }
}
