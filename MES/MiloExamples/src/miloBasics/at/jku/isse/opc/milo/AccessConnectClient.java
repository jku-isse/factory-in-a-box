/*
 * Copyright (c) 2016 Kevin Herron
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *   http://www.eclipse.org/org/documents/edl-v10.html.
 */

package miloBasics.at.jku.isse.opc.milo;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.model.nodes.objects.ServerNode;
import org.eclipse.milo.opcua.sdk.client.model.nodes.variables.ServerStatusNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.enumerated.ServerState;
import org.eclipse.milo.opcua.stack.core.types.structured.BuildInfo;
import org.eclipse.milo.opcua.stack.core.types.structured.ServerStatusDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccessConnectClient implements Client {

    public static void main(String[] args) throws Exception {
    	System.out.println("Starting.");
        AccessConnectClient example = new AccessConnectClient();

        System.out.println("Started.");
        new HighLevelClientRunner(example).run();
        System.out.println("Done.");
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void run(OpcUaClient client, CompletableFuture<OpcUaClient> future) throws Exception {
        // synchronous connect
        System.out.println("Connecting.");
        client.connect().get();

        // Get a typed reference to the Server object: ServerNode
        ServerNode serverNode = client.getAddressSpace().getObjectNode(
            Identifiers.Server,
            ServerNode.class
        ).get();

        // Read properties of the Server object...
        String[] serverArray = serverNode.getServerArray().get();
        String[] namespaceArray = serverNode.getNamespaceArray().get();

        logger.info("ServerArray={}", Arrays.toString(serverArray));
        logger.info("NamespaceArray={}", Arrays.toString(namespaceArray));

        // Read the value of attribute the ServerStatus variable component
        ServerStatusDataType serverStatus = serverNode.getServerStatus().get();

        logger.info("ServerStatus={}", serverStatus);

        System.out.println("Done.");

        future.complete(client);
    }

}