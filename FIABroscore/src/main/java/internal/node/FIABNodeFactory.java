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
 * NOTE: This class is a derivative of rosjava_core/DefaultNodeFactory and contains modifications
 */

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
