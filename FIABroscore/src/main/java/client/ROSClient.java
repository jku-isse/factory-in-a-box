package client;

import akka.actor.ActorRef;
import internal.node.FIABNodeMain;
import internal.DefaultFIABNodeMainExecutor;
import internal.FIABNodeMainExecutor;
import internal.FIABRosLoader;
import org.ros.exception.RosRuntimeException;
import org.ros.exception.ServiceNotFoundException;
import org.ros.internal.message.Message;
import org.ros.internal.node.DefaultNode;
import org.ros.node.*;
import org.ros.node.service.ServiceClient;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ROSClient {

    private final NodeConfiguration nodeConfiguration;
    private FIABNodeMain nodeMain;
    private FIABNodeMainExecutor nodeMainExecutor;
    private DefaultNode node;
    private final Map<String, ServiceClient<?, ?>> serviceClientMap;

    public static ROSClient newInstance(Class<? extends NodeMain> nodeClass) {
        ROSClient rosClient = new ROSClient(nodeClass, null);
        try {
            rosClient.initClientNode().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return rosClient;
    }

    public static ROSClient newInstance(Class<? extends NodeMain> nodeClass, NodeConfiguration nodeConfiguration) {
        ROSClient rosClient = new ROSClient(nodeClass, nodeConfiguration);
        try {
            rosClient.initClientNode().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return rosClient;
    }

    private ROSClient(Class<? extends NodeMain> nodeClass, NodeConfiguration nodeConfiguration) {
        FIABRosLoader loader = new FIABRosLoader();
        this.serviceClientMap = new HashMap<>();
        if (nodeConfiguration == null) {
            this.nodeConfiguration = loader.build();
        } else {
            this.nodeConfiguration = nodeConfiguration;
        }
        initRosNode(nodeClass, loader);
    }

    private void initRosNode(Class<? extends NodeMain> nodeClass, FIABRosLoader loader) {
        this.nodeMain = null;
        try {
            nodeMain = loader.loadClass(nodeClass);
        } catch (ClassNotFoundException e) {
            throw new RosRuntimeException("Unable to locate node: " + nodeClass.getName(), e);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RosRuntimeException("Unable to instantiate node: " + nodeClass.getName(), e);
        }
        initNodeExecutor(nodeClass);
    }

    private void initNodeExecutor(Class<? extends NodeMain> nodeClass) {
        if (nodeMain != null) {
            this.nodeMainExecutor = DefaultFIABNodeMainExecutor.newDefault();
        }
        if (nodeMainExecutor == null) {
            throw new RosRuntimeException("Could not create node main executor for client of class" + nodeClass.getName());
        }
        if (nodeConfiguration == null) {
            throw new RosRuntimeException("Could not create node configuration for client of class" + nodeClass.getName());
        }
    }

    private CompletableFuture<Void> initClientNode() {
        return CompletableFuture.supplyAsync(() -> nodeMainExecutor.execute(nodeMain, nodeConfiguration))
                .thenAccept(n -> this.node = n);
    }

    /**
     * Creates a new service client and adds it to local map (K=serviceType, V=serviceClient)
     *
     * @param serviceName
     * @param serviceType
     * @return
     * @throws ServiceNotFoundException
     */
    public ServiceClient<? extends Message, ? extends Message> createServiceClient(String serviceName, String serviceType) throws ServiceNotFoundException {
        ServiceClient<? extends Message, ? extends Message> serviceClient = node.newServiceClient(serviceName, serviceType);
        this.serviceClientMap.put(serviceType, serviceClient);
        return serviceClient;
    }

    /**
     * Retrieve service client from map using serviceType as the id
     *
     * @param serviceType
     * @return
     */
    public ServiceClient<?, ?> getServiceClient(String serviceType) {
        return serviceClientMap.get(serviceType);
    }

    /**
     * This creates a new message using an available serviceClient that has a matching requestType
     *
     * @param serviceType
     * @param requestType
     * @param <T>
     * @return
     */
    public <T extends Message> T createNewMessage(String serviceType, Class<?> requestType) {
        return (T) requestType.cast(this.serviceClientMap.get(serviceType).newMessage());
    }

    public void shutdownClient() {
        this.nodeMainExecutor.shutdown();
    }

}
