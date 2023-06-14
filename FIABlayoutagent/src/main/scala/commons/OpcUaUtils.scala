package commons

import fiab.core.capabilities.AbstractCapabilityImpl
import fiab.core.capabilities.handshake.HandshakeCapability
import fiab.core.capabilities.meta.OPCUACapabilitiesAndWiringInfoBrowsenames.{CAPABILITIES, TYPE}
import fiab.opcua.client.FiabOpcUaClient
import org.eclipse.milo.opcua.sdk.client.AddressSpace.BrowseOptions
import org.eclipse.milo.opcua.sdk.client.nodes.UaNode
import org.eclipse.milo.opcua.stack.core.Identifiers
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseDirection

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.asScalaIteratorConverter

object OpcUaUtils {

  implicit class FiabOpcUaClientExt(fiabOpcUaClient: FiabOpcUaClient) {

    val forwardBrowseOption: BrowseOptions = BrowseOptions.builder().setBrowseDirection(BrowseDirection.Forward).build()

    def browseAddressSpace(rootNode: UaNode, browseOption: BrowseOptions): List[UaNode] = {
      fiabOpcUaClient.getAddressSpace.browseNodes(rootNode, browseOption).stream().iterator().asScala.toList//.toScala(List)
    }

    def getObjectsNode: UaNode = {
      fiabOpcUaClient.getNodeForId(Identifiers.ObjectsFolder)
    }

    def getMachineRootNodeId: NodeId = {
      fiabOpcUaClient.browseAddressSpace(getObjectsNode, forwardBrowseOption)
        .filter { node => node.getNodeId.getNamespaceIndex != UShort.valueOf(0) }
        .collectFirst { case node => node.getNodeId }
        .get
    }

    def getMachineRootNode: UaNode = {
      val nodeId = fiabOpcUaClient.getMachineRootNodeId
      fiabOpcUaClient.getNodeForId(nodeId)
    }

    def getNodeIdsContainingSubstring(browseNameSubstring: String, browseRootNode: UaNode = getMachineRootNode): List[NodeId] = {
      fiabOpcUaClient.browseAddressSpace(browseRootNode, forwardBrowseOption)
        .map { node => node.getNodeId }
        .filter { nodeId => nodeId.getIdentifier.toString.contains(browseNameSubstring) }
    }

    def findHandshakeCapabilityNodeId(nodeIds: List[NodeId]): NodeId = {
      for (nodeId <- nodeIds) {
        val capType = fiabOpcUaClient.getChildNodeByBrowseName(nodeId, TYPE)
        val value = fiabOpcUaClient.readStringVariableNode(capType)
        if (value == HandshakeCapability.HANDSHAKE_CAPABILITY_URI) return nodeId
      }
      NodeId.NULL_GUID
    }

    def findTopLevelCapabilityNode(): NodeId = {
      val capabilitiesNodeId = fiabOpcUaClient.getChildNodeByBrowseName(getMachineRootNodeId, CAPABILITIES)
      val capabilitiesNode = fiabOpcUaClient.getNodeForId(capabilitiesNodeId)

      val capabilityCandidates = fiabOpcUaClient.browseAddressSpace(capabilitiesNode, fiabOpcUaClient.forwardBrowseOption)
        .map { node => node.getNodeId }

      for (nodeId <- capabilityCandidates) {
        val capType = fiabOpcUaClient.getChildNodeByBrowseName(nodeId, TYPE)
        val value = fiabOpcUaClient.readStringVariableNode(capType)
        if (value != HandshakeCapability.HANDSHAKE_CAPABILITY_URI) return nodeId
      }
      NodeId.NULL_GUID
    }
  }
}
