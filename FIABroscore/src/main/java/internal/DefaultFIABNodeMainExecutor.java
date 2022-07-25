/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * NOTE: This class is a derivative of rosjava_core/DefaultNodeMainExecutor and contains modifications
 */

package internal;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ScheduledExecutorService;

import internal.node.FIABNodeFactory;
import org.ros.concurrent.DefaultScheduledExecutorService;
import org.ros.internal.node.DefaultNode;
import org.ros.namespace.GraphName;
import org.ros.node.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultFIABNodeMainExecutor implements FIABNodeMainExecutor {
    private static final boolean DEBUG = false;
    private static final Logger log = LoggerFactory.getLogger(DefaultFIABNodeMainExecutor.class);
    private final FIABNodeFactory nodeFactory;
    private final ScheduledExecutorService scheduledExecutorService;
    private final Multimap<GraphName, ConnectedNode> connectedNodes;
    private final BiMap<Node, NodeMain> nodeMains;

    public static FIABNodeMainExecutor newDefault() {
        return newDefault(new DefaultScheduledExecutorService());
    }

    public static FIABNodeMainExecutor newDefault(ScheduledExecutorService executorService) {
        return new DefaultFIABNodeMainExecutor(new FIABNodeFactory(executorService), executorService);
    }

    private DefaultFIABNodeMainExecutor(FIABNodeFactory nodeFactory, ScheduledExecutorService scheduledExecutorService) {
        this.nodeFactory = nodeFactory;
        this.scheduledExecutorService = scheduledExecutorService;
        this.connectedNodes = Multimaps.synchronizedMultimap(HashMultimap.create());
        this.nodeMains = Maps.synchronizedBiMap(HashBiMap.create());
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                DefaultFIABNodeMainExecutor.this.shutdown();
            }
        }));
    }

    public ScheduledExecutorService getScheduledExecutorService() {
        return this.scheduledExecutorService;
    }

    public DefaultNode execute(final NodeMain nodeMain, NodeConfiguration nodeConfiguration, final Collection<NodeListener> nodeListeners) {
        final NodeConfiguration nodeConfigurationCopy = NodeConfiguration.copyOf(nodeConfiguration);
        nodeConfigurationCopy.setDefaultNodeName(nodeMain.getDefaultNodeName());
        Preconditions.checkNotNull(nodeConfigurationCopy.getNodeName(), "Node name not specified.");
        //this.scheduledExecutorService.execute(new Runnable() {
        //    public void run() {
        Collection<NodeListener> nodeListenersCopy = Lists.newArrayList();
        nodeListenersCopy.add(DefaultFIABNodeMainExecutor.this.new RegistrationListener());
        nodeListenersCopy.add(nodeMain);
        if (nodeListeners != null) {
            nodeListenersCopy.addAll(nodeListeners);
        }

        DefaultNode node = DefaultFIABNodeMainExecutor.this.nodeFactory.newNode(nodeConfigurationCopy, nodeListenersCopy);
        DefaultFIABNodeMainExecutor.this.nodeMains.put(node, nodeMain);
        //    }
        //});
        return node;
    }

    public DefaultNode execute(NodeMain nodeMain, NodeConfiguration nodeConfiguration) {
        return this.execute(nodeMain, nodeConfiguration, (Collection) null);
    }

    public void shutdownNodeMain(NodeMain nodeMain) {
        Node node = (Node) this.nodeMains.inverse().get(nodeMain);
        if (node != null) {
            this.safelyShutdownNode(node);
        }

    }

    public void shutdown() {
        synchronized (this.connectedNodes) {
            Iterator var2 = this.connectedNodes.values().iterator();

            while (var2.hasNext()) {
                ConnectedNode connectedNode = (ConnectedNode) var2.next();
                this.safelyShutdownNode(connectedNode);
            }

        }
    }

    private void safelyShutdownNode(Node node) {
        boolean success = true;

        try {
            node.shutdown();
        } catch (Exception var4) {
            log.error("Exception thrown while shutting down node.", var4);
            this.unregisterNode(node);
            success = false;
        }

        if (success) {
            log.info("Shutdown successful.");
        }

    }

    private void registerNode(ConnectedNode connectedNode) {
        GraphName nodeName = connectedNode.getName();
        synchronized (this.connectedNodes) {
            Iterator var4 = this.connectedNodes.get(nodeName).iterator();

            while (var4.hasNext()) {
                ConnectedNode illegalConnectedNode = (ConnectedNode) var4.next();
                System.err.println(String.format("Node name collision. Existing node %s (%s) will be shutdown.", nodeName, illegalConnectedNode.getUri()));
                illegalConnectedNode.shutdown();
            }

            this.connectedNodes.put(nodeName, connectedNode);
        }
    }

    private void unregisterNode(Node node) {
        node.removeListeners();
        this.connectedNodes.get(node.getName()).remove(node);
        this.nodeMains.remove(node);
    }

    private class RegistrationListener implements NodeListener {
        private RegistrationListener() {
        }

        public void onStart(ConnectedNode connectedNode) {
            DefaultFIABNodeMainExecutor.this.registerNode(connectedNode);
        }

        public void onShutdown(Node node) {
        }

        public void onShutdownComplete(Node node) {
            DefaultFIABNodeMainExecutor.this.unregisterNode(node);
        }

        public void onError(Node node, Throwable throwable) {
            DefaultFIABNodeMainExecutor.log.error("Node error.", throwable);
            DefaultFIABNodeMainExecutor.this.unregisterNode(node);
        }
    }
}
