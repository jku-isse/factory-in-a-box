package fiab.mes.opcua;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ubyte;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.AccessContext;
import org.eclipse.milo.opcua.sdk.server.api.DataItem;
import org.eclipse.milo.opcua.sdk.server.api.MethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.api.MonitoredItem;
import org.eclipse.milo.opcua.sdk.server.api.Namespace;
import org.eclipse.milo.opcua.sdk.server.nodes.AttributeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.ServerNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.server.util.AnnotationBasedInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.util.SubscriptionModel;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.eclipse.milo.opcua.stack.core.types.structured.WriteValue;
import org.eclipse.milo.opcua.stack.core.util.FutureUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import miloBasics.at.jku.isse.opc.milo.ValueLoggingDelegate;
import miloBasics.at.pro2future.shopfloors.mockdiscovery.HelloWorldMethod;
import miloBasics.org.eclipse.milo.examples.server.methods.SqrtMethod;

public class NameSpace implements Namespace {

	public static final String NAMESPACE_URI = "urn:eclipse:milo:hello-world"; // TODO change
	private static String[] folderNames = { "ProcessEngine", "Conveyor", "Turntable" };

	private static final Object[][] STATIC_SCALAR_NODES = new Object[][] {
			{ "ProcessEngineStatus", Identifiers.String, new Variant("ProcessEngine") },
			{ "ConveyorStatus", Identifiers.String, new Variant("Conveyor")},
			{ "TurntableStatus", Identifiers.String, new Variant("Turntable") }
			// SOME MACHINE VALUES
			// MAYBE STATUSES FOR FUs
	};

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final SubscriptionModel subscriptionModel;

	private final OpcUaServer server;
	private final UShort namespaceIndex;

	public NameSpace(OpcUaServer server, UShort namespaceIndex) {
		this.server = server;
		this.namespaceIndex = namespaceIndex;

		subscriptionModel = new SubscriptionModel(server, this);

		try {

			NodeId folderNodeId = new NodeId(namespaceIndex, "FunctionalUnit");

			UaFolderNode folderNode = new UaFolderNode(server.getNodeMap(), folderNodeId,
					new QualifiedName(namespaceIndex, "FunctionalUnit"), LocalizedText.english("FunctionalUnit"));

			server.getNodeMap().addNode(folderNode);

			// Make sure our new folder shows up under the server's Objects folder
			server.getUaNamespace().addReference(Identifiers.ObjectsFolder, Identifiers.Organizes, true,
					folderNodeId.expanded(), NodeClass.Object);

			// Add the rest of the nodes
			addVariableNodes(folderNode);

		} catch (UaException e) {
			logger.error("Error adding nodes: {}", e.getMessage(), e);
		}
	}

	@Override
	public UShort getNamespaceIndex() {
		return namespaceIndex;
	}

	@Override
	public String getNamespaceUri() {
		return NAMESPACE_URI;
	}

	private void addVariableNodes(UaFolderNode rootNode) {
		addScalarNodes(rootNode);
	}

	@SuppressWarnings("unused")
	private void addSOME_METHODNode(UaFolderNode folderNode) {
		UaMethodNode methodNode = UaMethodNode.builder(server.getNodeMap())
				.setNodeId(new NodeId(namespaceIndex, "helloworld"))
				.setBrowseName(new QualifiedName(namespaceIndex, "HelloWorld"))
				.setDisplayName(new LocalizedText(null, "HelloWorld"))
				.setDescription(
						LocalizedText.english("Returns the correctly rounded positive square root of a double value."))
				.build();

		try {
			AnnotationBasedInvocationHandler invocationHandler = AnnotationBasedInvocationHandler
					.fromAnnotatedObject(server.getNodeMap(), new HelloWorldMethod());

			methodNode.setProperty(UaMethodNode.InputArguments, invocationHandler.getInputArguments());
			methodNode.setProperty(UaMethodNode.OutputArguments, invocationHandler.getOutputArguments());
			methodNode.setInvocationHandler(invocationHandler);

			server.getNodeMap().addNode(methodNode);

			folderNode.addReference(new Reference(folderNode.getNodeId(), Identifiers.HasComponent,
					methodNode.getNodeId().expanded(), methodNode.getNodeClass(), true));

			methodNode.addReference(new Reference(methodNode.getNodeId(), Identifiers.HasComponent,
					folderNode.getNodeId().expanded(), folderNode.getNodeClass(), false));
		} catch (Exception e) {
			logger.error("Error creating sqrt() method.", e);
		}
	}

	@SuppressWarnings("unused")
	private void addMethodNode(UaFolderNode folderNode) {
		UaMethodNode methodNode = UaMethodNode.builder(server.getNodeMap())
				.setNodeId(new NodeId(namespaceIndex, "HelloWorld/sqrt(x)"))
				.setBrowseName(new QualifiedName(namespaceIndex, "sqrt(x)"))
				.setDisplayName(new LocalizedText(null, "sqrt(x)"))
				.setDescription(
						LocalizedText.english("Returns the correctly rounded positive square root of a double value."))
				.build();

		try {
			AnnotationBasedInvocationHandler invocationHandler = AnnotationBasedInvocationHandler
					.fromAnnotatedObject(server.getNodeMap(), new SqrtMethod());

			methodNode.setProperty(UaMethodNode.InputArguments, invocationHandler.getInputArguments());
			methodNode.setProperty(UaMethodNode.OutputArguments, invocationHandler.getOutputArguments());
			methodNode.setInvocationHandler(invocationHandler);

			server.getNodeMap().addNode(methodNode);

			folderNode.addReference(new Reference(folderNode.getNodeId(), Identifiers.HasComponent,
					methodNode.getNodeId().expanded(), methodNode.getNodeClass(), true));

			methodNode.addReference(new Reference(methodNode.getNodeId(), Identifiers.HasComponent,
					folderNode.getNodeId().expanded(), folderNode.getNodeClass(), false));
		} catch (Exception e) {
			logger.error("Error creating sqrt() method.", e);
		}
	}

	private UaFolderNode createFolderNode(String name, UaFolderNode rootNode) {
		UaFolderNode folder = new UaFolderNode(server.getNodeMap(),
				new NodeId(namespaceIndex, "FunctionalUnit/" + name), new QualifiedName(namespaceIndex, name),
				LocalizedText.english(name));
		server.getNodeMap().addNode(folder);
		rootNode.addOrganizes(folder);
		return folder;
	}

	private void addScalarNodes(UaFolderNode rootNode) {
		int i = 0;
		for (Object[] os : STATIC_SCALAR_NODES) {
			String name = (String) os[0];
			NodeId typeId = (NodeId) os[1];
			Variant variant = (Variant) os[2];

			UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(server.getNodeMap())
					.setNodeId(new NodeId(namespaceIndex, "FunctionalUnit/" + folderNames[i] + "/" + name))
					.setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
					.setUserAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
					.setBrowseName(new QualifiedName(namespaceIndex, name)).setDisplayName(LocalizedText.english(name))
					.setDataType(typeId).setTypeDefinition(Identifiers.BaseDataVariableType).build();

			node.setValue(new DataValue(variant));

			node.setAttributeDelegate(new ValueLoggingDelegate());

			server.getNodeMap().addNode(node);
			createFolderNode(folderNames[i], rootNode).addOrganizes(node);
			i++;
		}
	}

	@Override
	public CompletableFuture<List<Reference>> browse(AccessContext context, NodeId nodeId) {
		ServerNode node = server.getNodeMap().get(nodeId);

		if (node != null) {
			return CompletableFuture.completedFuture(node.getReferences());
		} else {
			return FutureUtils.failedFuture(new UaException(StatusCodes.Bad_NodeIdUnknown));
		}
	}

	@Override
	public void read(ReadContext context, Double maxAge, TimestampsToReturn timestamps,
			List<ReadValueId> readValueIds) {

		List<DataValue> results = Lists.newArrayListWithCapacity(readValueIds.size());

		for (ReadValueId readValueId : readValueIds) {
			ServerNode node = server.getNodeMap().get(readValueId.getNodeId());

			if (node != null) {
				DataValue value = node.readAttribute(new AttributeContext(context), readValueId.getAttributeId(),
						timestamps, readValueId.getIndexRange(), readValueId.getDataEncoding());

				results.add(value);
			} else {
				results.add(new DataValue(StatusCodes.Bad_NodeIdUnknown));
			}
		}

		context.complete(results);
	}

	@Override
	public void write(WriteContext context, List<WriteValue> writeValues) {
		List<StatusCode> results = Lists.newArrayListWithCapacity(writeValues.size());

		for (WriteValue writeValue : writeValues) {
			ServerNode node = server.getNodeMap().get(writeValue.getNodeId());

			if (node != null) {
				try {
					node.writeAttribute(new AttributeContext(context), writeValue.getAttributeId(),
							writeValue.getValue(), writeValue.getIndexRange());

					results.add(StatusCode.GOOD);

					logger.info("Wrote value {} to {} attribute of {}", writeValue.getValue().getValue(),
							AttributeId.from(writeValue.getAttributeId()).map(Object::toString).orElse("unknown"),
							node.getNodeId());
				} catch (UaException e) {
					logger.error("Unable to write value={}", writeValue.getValue(), e);
					results.add(e.getStatusCode());
				}
			} else {
				results.add(new StatusCode(StatusCodes.Bad_NodeIdUnknown));
			}
		}

		context.complete(results);
	}

	@Override
	public void onDataItemsCreated(List<DataItem> dataItems) {
		subscriptionModel.onDataItemsCreated(dataItems);
	}

	@Override
	public void onDataItemsModified(List<DataItem> dataItems) {
		subscriptionModel.onDataItemsModified(dataItems);
	}

	@Override
	public void onDataItemsDeleted(List<DataItem> dataItems) {
		subscriptionModel.onDataItemsDeleted(dataItems);
	}

	@Override
	public void onMonitoringModeChanged(List<MonitoredItem> monitoredItems) {
		subscriptionModel.onMonitoringModeChanged(monitoredItems);
	}

	@Override
	public Optional<MethodInvocationHandler> getInvocationHandler(NodeId methodId) {
		Optional<ServerNode> node = server.getNodeMap().getNode(methodId);

		return node.flatMap(n -> {
			if (n instanceof UaMethodNode) {
				return ((UaMethodNode) n).getInvocationHandler();
			} else {
				return Optional.empty();
			}
		});
	}

	public static List<String> getScalarNodes() {
		List<String> rtrn = new ArrayList<String>();
		int i = 0;
		for (Object[] os : STATIC_SCALAR_NODES) {
			rtrn.add("FunctionalUnit/" + folderNames[i] + "/" + (String) os[0]);
		}
		return rtrn;
	}

}
