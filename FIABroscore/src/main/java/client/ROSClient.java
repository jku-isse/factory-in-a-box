package client;

import internal.exception.ServiceClientNotFoundException;
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
    private NodeMain nodeMain;
    private FIABNodeMainExecutor nodeMainExecutor;
    private DefaultNode node;
    private final Map<String, ServiceClient<?, ?>> serviceClientMap;

    /**
     * Creates a new ROSClient. This is a blocking operation, since the client will immediately connect to the server
     * @param nodeClass class of the client ROS Node
     * @param nodeConfiguration configuration for given node (where is master located?, message factories, etc.)
     * @return connected ROSClient
     */
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

    /**
     * Starts the lifecycle of the Node
     * @param nodeClass node class to use
     * @param loader responsible for loading class into a Node
     */
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
     * @param serviceName the name for a service. Note that this value should be unique
     * @param serviceType the type of the service, usually identified by MsgType._type
     * @return new Service Client
     * @throws ServiceNotFoundException in case the service does not exist on the server or client not connected
     */
    public ServiceClient<? extends Message, ? extends Message> createServiceClient(String serviceName, String serviceType) throws ServiceNotFoundException {
        ServiceClient<? extends Message, ? extends Message> serviceClient = node.newServiceClient(serviceName, serviceType);
        this.serviceClientMap.put(serviceType, serviceClient);
        return serviceClient;
    }

    /**
     * Retrieve service client from map using serviceType as the id
     *
     * @param serviceType The type of the service, usually identified by MsgType._type
     * @return serviceClient that can be implicitly cast to ServiceClient<ReqClass, RespClass>
     */
    public <Q, S> ServiceClient<Q, S> getServiceClient(String serviceType) {
        return (ServiceClient<Q, S>) serviceClientMap.get(serviceType);
    }

    /**
     * This creates a new message using an available serviceClient that has a matching requestType
     * @param serviceType The type of the service, usually identified by MsgType._type
     * @param requestType Type of the request. Usually just add the Request Suffix e.g. MsgTypeRequest
     * @param <T> Class of the request, usually extends Message (from the ROSjava package)
     * @return request
     */
    public <T extends Message> T createNewMessage(String serviceType, Class<T> requestType) throws ServiceClientNotFoundException {
        if (serviceClientMap.containsKey(serviceType)) {
            return requestType.cast(this.serviceClientMap.get(serviceType).newMessage());
        } else {
            throw new ServiceClientNotFoundException();
        }
    }

    /**
     * Shuts down the client
     */
    public void shutdownClient() {
        this.nodeMainExecutor.shutdown();
    }

}
