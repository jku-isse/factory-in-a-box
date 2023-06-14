package qrCodeGenerator

import commons.OpcUaUtils.FiabOpcUaClientExt
import commons.{QrCodeUtils, WiringEndpoint}
import fiab.core.capabilities.handshake.{HandshakeCapability, IOStationCapability}
import fiab.core.capabilities.meta.OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITIES
import fiab.core.capabilities.transport.{TransportModuleCapability, TurntableModuleWellknownCapabilityIdentifiers}
import fiab.opcua.client.{FiabOpcUaClient, OPCUAClientFactory}
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId

object OpcUaNodesToJsonParser {
  def apply(endpointUrl: String): OpcUaNodesToJsonParser = new OpcUaNodesToJsonParser(endpointUrl)

}

class OpcUaNodesToJsonParser(val endpointUrl: String) {

  private val client: FiabOpcUaClient = OPCUAClientFactory.createFIABClientAndConnect(endpointUrl)

  def parseCapabilityInfosForMachine(): Map[String, String] = {
    val machineId = client.getMachineRootNode.getBrowseName.getName
    var jsonData: Map[String, String] = Map()
    for (handshakeCapabilityNodeId <- getHandshakeCapabilityNodeIds) {
      val handshakeId = getHandshakeCapabilityId(handshakeCapabilityNodeId)
      val wiringEndpoint = WiringEndpoint(machineId, handshakeId, handshakeCapabilityNodeId.toParseableString)
      val wiringJson = QrCodeUtils.encodeWiringEndpointJson(wiringEndpoint)
      jsonData += (s"${machineId}_$handshakeId" -> wiringJson)
    }
    jsonData
  }

  private def getHandshakeCapabilityId(handshakeCapabilityNodeId: NodeId): String = {
    val handshakeId = client.getChildNodeByBrowseName(handshakeCapabilityNodeId, "ID")
    client.readStringVariableNode(handshakeId)
  }

  private def getHandshakeCapabilityNodeIds: List[NodeId] = {
    val serverHSNodeIds = client.getNodeIdsContainingSubstring(HandshakeCapability.SERVER_CAPABILITY_ID)
    val clientHSNodeIds = client.getNodeIdsContainingSubstring(HandshakeCapability.CLIENT_CAPABILITY_ID)
    val ioHSNodeIds = getIOHandshakeCapabilityNodes //Workaround as IO is a handshake
    val handshakeFUNodeIds: List[NodeId] = (serverHSNodeIds ::: clientHSNodeIds ::: ioHSNodeIds).distinct

    handshakeFUNodeIds.flatMap { nodeId => parseHandshakeCapabilitiesFolder(nodeId) }
  }

  private def getIOHandshakeCapabilityNodes: List[NodeId] = {
    val machineRootNode = client.getMachineRootNode
    val browseNameAsLowercaseString = machineRootNode.getBrowseName.getName.toLowerCase
    if (browseNameAsLowercaseString.contains("input") || browseNameAsLowercaseString.contains("output"))
      List(machineRootNode.getNodeId)
    else List()
  }

  private def parseHandshakeCapabilitiesFolder(handshakeRootNodeId: NodeId): Option[NodeId] = {
    val capabilitiesNodeId = client.getChildNodeByBrowseName(handshakeRootNodeId, CAPABILITIES)
    val capabilitiesNode = client.getNodeForId(capabilitiesNodeId)

    val capabilityCandidates = client.browseAddressSpace(capabilitiesNode, client.forwardBrowseOption)
      .map { node => node.getNodeId }

    client.findHandshakeCapabilityNodeId(capabilityCandidates) match {
      case NodeId.NULL_GUID => Option.empty
      case nodeId => Option(nodeId)
    }
  }
}
