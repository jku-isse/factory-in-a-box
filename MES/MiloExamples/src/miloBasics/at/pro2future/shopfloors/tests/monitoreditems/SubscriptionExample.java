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

package miloBasics.at.pro2future.shopfloors.tests.monitoreditems;

import static com.google.common.collect.Lists.newArrayList;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

// connects to the french methods from visualStudio.git/methodDemos

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoredItemCreateRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoringParameters;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import MyStuff.TestClient;

public class SubscriptionExample implements ClientExample {
	boolean running;
	private String nodeAddress;
	private String serverName;
	private TestClient client;
	
	@Override
    public String getEndpointUrl() {
    	return "opc.tcp://127.0.0.1:12686/" + serverName;
    }
	
	public SubscriptionExample(String nodeAddress, TestClient client, String serverName) {
		this.nodeAddress = nodeAddress;
		this.client = client;
		this.serverName = serverName;
		System.out.println("Subscription server name: " + serverName);
    	running = true;
	}
	

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final AtomicLong clientHandles = new AtomicLong(1L);

    @Override
    public void run(OpcUaClient client, CompletableFuture<OpcUaClient> future) throws Exception {
        // synchronous connect
        client.connect().get();

        // create a subscription @ 1000ms
        UaSubscription subscription = client.getSubscriptionManager().createSubscription(100.0).get();

        // subscribe to the Value attribute of the server's CurrentTime node
        /*
        ReadValueId readValueId = new ReadValueId(
            Identifiers.Server_ServerStatus_CurrentTime,
            AttributeId.Value.uid(), null, QualifiedName.NULL_VALUE);
        */
        ReadValueId readValueId = new ReadValueId(
                new NodeId(2, "HelloWorld/ScalarTypes/" + nodeAddress),
                AttributeId.Value.uid(), null, QualifiedName.NULL_VALUE);
        System.out.println(readValueId);

        // important: client handle must be unique per item
        UInteger clientHandle = uint(clientHandles.getAndIncrement());

        MonitoringParameters parameters = new MonitoringParameters(
            clientHandle,
            1000.0,     // sampling interval
            null,       // filter, null means use default
            uint(10),   // queue size
            true        // discard oldest
        );

        MonitoredItemCreateRequest request = new MonitoredItemCreateRequest(
            readValueId, MonitoringMode.Reporting, parameters);

        // when creating items in MonitoringMode.Reporting this callback is where each item needs to have its
        // value/event consumer hooked up. The alternative is to create the item in sampling mode, hook up the
        // consumer after the creation call completes, and then change the mode for all items to reporting.
        BiConsumer<UaMonitoredItem, Integer> onItemCreated =
            (item, id) -> item.setValueConsumer(this::onSubscriptionValue);

        List<UaMonitoredItem> items = subscription.createMonitoredItems(
            TimestampsToReturn.Both,
            newArrayList(request),
            onItemCreated
        ).get();

        for (UaMonitoredItem item : items) {
            if (item.getStatusCode().isGood()) {
            	System.out.println("item created for nodeId=" + item.getReadValueId().getNodeId());
                logger.info("item created for nodeId={}", item.getReadValueId().getNodeId());
            } else {
                logger.warn(
                    "failed to create item for nodeId={} (status={})",
                    item.getReadValueId().getNodeId(), item.getStatusCode());
            }
        }

        while(running) {
        	Thread.sleep(1000);
        }
        future.complete(client);
    }

    private void onSubscriptionValue(UaMonitoredItem item, DataValue value) {
    	System.out.println("received update for nodeId=" + item.getReadValueId().getNodeId() + ": " + value.getValue());
    	if(value.getValue().getValue().toString().equals("Hello")) {
    		if(client.transportReady()) client.changeValue("Ready for transport!");
    	} else if(value.getValue().getValue().toString().equals("Ready for transport!")) {
    		client.transport(); client.changeValue("item is on it's way");
    	} else if(value.getValue().getValue().toString().contentEquals("item is on it's way")) {
    		try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		client.changeValue("Recieved!");
    	}
        logger.info(
            "subscription value received: item={}, value={}",
            item.getReadValueId().getNodeId(), value.getValue());
        if(value.getValue().getValue().equals(0)) {
        	running = false;
        }
    }

}
