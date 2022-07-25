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
