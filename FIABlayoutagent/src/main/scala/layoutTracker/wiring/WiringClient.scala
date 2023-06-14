package layoutTracker.wiring

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import commons.OpcUaUtils
import commons.OpcUaUtils.FiabOpcUaClientExt
import fiab.core.capabilities.handshake.HandshakeCapability
import fiab.core.capabilities.meta.OPCUACapabilitiesAndWiringInfoBrowsenames.{CAPABILITIES, CAPABILITY, ROLE_VALUE_PROVIDED, ROLE_VALUE_REQUIRED, TYPE}
import fiab.core.capabilities.wiring.{WiringInfo, WiringInfoBuilder}
import fiab.opcua.client.OPCUAClientFactory
import layoutTracker.CapabilityInstanceEdge
import org.eclipse.milo.opcua.stack.core.types.builtin.{NodeId, Variant}

import scala.util.{Failure, Success, Try}

object MachineCapabilityInfo {

  private val mapper: JsonMapper = JsonMapper
    .builder().
    addModule(DefaultScalaModule)
    .build()

  def encodeToJson(machineCapabilityInfo: MachineCapabilityInfo): String = {
    mapper.writeValueAsString(machineCapabilityInfo)
  }

  def parseToJson(jsonData: String): Option[MachineCapabilityInfo] = {
    val parsedValue = Try(mapper.readValue(jsonData, classOf[MachineCapabilityInfo]))
    parsedValue match {
      case Success(value) => Option(value)
      case Failure(_) => Option.empty
    }
  }


}

case class MachineCapabilityInfo(machineId: String, endpoint: String, capabilityRootNode: String, capabilityId: String, capabilityType: String, capabilityRole: String)

case class CapabilityInfo(endpoint: String, capabilityRootNode: String, capabilityId: String, capabilityType: String, capabilityRole: String)

object WiringClients {
  val endpoints: Seq[String] = for {i <- 0 until 8} yield s"opc.tcp://127.0.0.1:${4840 + i}"
  private val wiringClients = (for {endpoint <- endpoints} yield new WiringClient(endpoint)).toList
  private val emptyCapabilityInfo: CapabilityInfo = CapabilityInfo("", "", "", "", "")

  def getTopLevelCapabilityForMachineId(machineId: String): MachineCapabilityInfo = {
    wiringClients.find(machine => machine.machineId == machineId).get.parseMachineCapabilityInfo()
  }

  def wireMachines(capabilityInstanceEdge: CapabilityInstanceEdge): Unit = {
    val wiringInfoFromCapFolder = capabilityInstanceEdge.capInstanceFrom.capabilityFolder
    val wiringInfoToCapFolder = capabilityInstanceEdge.capInstanceTo.capabilityFolder
    val optClientFrom = wiringClients.find(client => client.machineId == capabilityInstanceEdge.capInstanceFrom.machineId)
    val optClientTo = wiringClients.find(client => client.machineId == capabilityInstanceEdge.capInstanceTo.machineId)
    optClientFrom match {
      case Some(clientFrom) =>
        optClientTo match {
          case Some(clientTo) =>
            //println(s"Performing wiring on machines: ${clientFrom.machineId} and ${clientTo.machineId}")
            clientFrom.wireIfClient(wiringInfoFromCapFolder, clientTo.parseCapabilityInfo(wiringInfoToCapFolder))
            clientTo.wireIfClient(wiringInfoToCapFolder, clientFrom.parseCapabilityInfo(wiringInfoFromCapFolder))
          case None =>
        }
      case None =>
    }
  }

  def disconnectMachines(capabilityInstanceEdge: CapabilityInstanceEdge): Unit = {
    val wiringInfoFromCapFolder = capabilityInstanceEdge.capInstanceFrom.capabilityFolder
    val wiringInfoToCapFolder = capabilityInstanceEdge.capInstanceTo.capabilityFolder
    val optClientFrom = wiringClients.find(client => client.machineId == capabilityInstanceEdge.capInstanceFrom.machineId)
    val optClientTo = wiringClients.find(client => client.machineId == capabilityInstanceEdge.capInstanceTo.machineId)
    optClientFrom match {
      case Some(clientFrom) =>
        optClientTo match {
          case Some(clientTo) =>
            println(s"Performing disconnect on machines: ${clientFrom.machineId} and ${clientTo.machineId}")
            clientFrom.wireIfClient(wiringInfoFromCapFolder, emptyCapabilityInfo)
            clientTo.wireIfClient(wiringInfoToCapFolder, emptyCapabilityInfo)
          case None =>
        }
      case None =>
    }
  }
}

class WiringClient(endpoint: String) {

  private val ID = "ID"
  private val TYPE = "TYPE"
  private val ROLE = "ROLE"

  private val client = OPCUAClientFactory.createFIABClientAndConnect(endpoint)
  var machineId: String = client.getMachineRootNode.getBrowseName.getName
  //println(s"Initialized client for endpoint $endpoint, machineId=$machineId")
  var cachedMachineCapabilityInfo: MachineCapabilityInfo = _
  var cachedCapabilityInfo: CapabilityInfo = _


  def parseMachineCapabilityInfo(): MachineCapabilityInfo = {
    val capabilityNodeId = client.findTopLevelCapabilityNode()
    val capId = client.readStringVariableNode(client.getChildNodeByBrowseName(capabilityNodeId, ID))
    val capType = client.readStringVariableNode(client.getChildNodeByBrowseName(capabilityNodeId, TYPE))
    val capRole = client.readStringVariableNode(client.getChildNodeByBrowseName(capabilityNodeId, ROLE))
    cachedMachineCapabilityInfo = MachineCapabilityInfo(machineId ,endpoint, capabilityNodeId.toString, capId, capType, capRole)
    cachedMachineCapabilityInfo
  }

  def parseCapabilityInfo(capNodeId: String): CapabilityInfo = {
    val capId = client.readStringVariableNode(NodeId.parse(s"$capNodeId/$ID"))
    val capType = client.readStringVariableNode(NodeId.parse(s"$capNodeId/$TYPE"))
    val capRole = client.readStringVariableNode(NodeId.parse(s"$capNodeId/$ROLE"))
    cachedCapabilityInfo = CapabilityInfo(endpoint, capNodeId, capId, capType, capRole)
    //println(s"Parsed Capability Info for $machineId at $capNodeId: $cachedCapabilityInfo")
    cachedCapabilityInfo
  }

  def wireIfClient(capNodeId: String, capabilityInfoOther: CapabilityInfo): Unit = {
    parseCapabilityInfo(capNodeId)
    if (cachedCapabilityInfo.capabilityRole == ROLE_VALUE_REQUIRED) {
      val wiringInfo = createWiring(capabilityInfoOther)
      val capabilitiesFolder = client.getParentNodeId(NodeId.parse(s"$capNodeId"))
      val handshakeClientFolder = client.getParentNodeId(capabilitiesFolder)
      val setWiringNodeId = client.getChildNodeByBrowseName(handshakeClientFolder, "SET_WIRING")
      if (capabilityInfoOther.capabilityRole == ROLE_VALUE_PROVIDED) {
        //println(s"Found client handshake of machine $machineId, starting wiring...")
        setWiring(setWiringNodeId, wiringInfo)
      } else {
        println(s"Found empty client handshake of machine $machineId and ${capabilityInfoOther.capabilityId}, removing wiring...")
        unwireIfClient(capNodeId)
      }
    } else {
      println(s"Skipping wiring for server handshake of machine $machineId")
    }
  }

  def unwireIfClient(capNodeId: String): Unit = {
    parseCapabilityInfo(capNodeId)
    if (cachedCapabilityInfo.capabilityRole == ROLE_VALUE_REQUIRED) {
      val capabilitiesFolder = client.getParentNodeId(NodeId.parse(s"$capNodeId"))
      val handshakeClientFolder = client.getParentNodeId(capabilitiesFolder)
      val setWiringNodeId = client.getChildNodeByBrowseName(handshakeClientFolder, "SET_WIRING")
      val emptyWiringInfo = new WiringInfo()
      setWiring(setWiringNodeId, emptyWiringInfo)
    } else {
      //println(s"Skipping unwiring for server handshake of machine $machineId")
    }
  }

  private def setWiring(setWiringNodeId: NodeId, wiringInfo: WiringInfo): Unit = {
    val args = Array[Variant](
      new Variant(wiringInfo.getLocalCapabilityId),
      new Variant(wiringInfo.getRemoteCapabilityId),
      new Variant(wiringInfo.getRemoteEndpointURL),
      new Variant(wiringInfo.getRemoteNodeId),
      new Variant(wiringInfo.getRemoteRole)
    )
    client.callStringMethodBlocking(setWiringNodeId, args: _*)
    println(s"Wiring set for machine $machineId at $endpoint, WiringInfo=$wiringInfo")
  }

  private def createWiring(capabilityInfoOther: CapabilityInfo): WiringInfo = {
    WiringInfoBuilder.create()
      .setLocalCapabilityId(cachedCapabilityInfo.capabilityId)
      .setRemoteCapabilityId(capabilityInfoOther.capabilityId)
      .setRemoteNodeId(capabilityInfoOther.capabilityRootNode)
      .setRemoteEndpointURL(capabilityInfoOther.endpoint)
      .setRemoteRole(capabilityInfoOther.capabilityRole)
      .build()
  }

}
