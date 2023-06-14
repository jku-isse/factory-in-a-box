package layoutTracker

import boofcv.alg.fiducial.qrcode.QrCode
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import commons.{QrCodeUtils, WiringEndpoint}
import layoutTracker.QRMachineGraph.GraphExt
import layoutTracker.wiring.{MachineCapabilityInfo, WiringClient, WiringClients}
import org.graphstream.graph.implementations.MultiGraph
import org.graphstream.graph.{Edge, Graph, Node}
import org.graphstream.stream.ElementSink

import java.nio.file.{Files, Paths}
import scala.jdk.CollectionConverters.asScalaIteratorConverter
import scala.util.{Failure, Success, Try}

object CapabilityInstanceEdge {
  private val mapper: JsonMapper = JsonMapper.builder().addModule(DefaultScalaModule).build()

  def fromNodeIds(nodeIdFrom: String, nodeIdTo: String): Option[CapabilityInstanceEdge] = {
    val wiringEndpointFrom = QrCodeUtils.decodeWiringEndpointJson(nodeIdFrom) //WiringEndpoint.fromString(nodeIdFrom)
    val wiringEndpointTo = QrCodeUtils.decodeWiringEndpointJson(nodeIdTo) //WiringEndpoint.fromString(nodeIdTo)
    if (wiringEndpointFrom.isDefined && wiringEndpointTo.isDefined) {
      return Option(CapabilityInstanceEdge(wiringEndpointFrom.get, wiringEndpointTo.get))
    }
    Option.empty
  }

  def toJson(capabilityInstanceEdge: CapabilityInstanceEdge): String = {
    mapper.writeValueAsString(capabilityInstanceEdge)
  }

  def fromJson(edgeAsJson: String): Try[CapabilityInstanceEdge] = {
    Try(mapper.readValue(edgeAsJson, classOf[CapabilityInstanceEdge]))
  }
}

case class CapabilityInstanceEdge(capInstanceFrom: WiringEndpoint, capInstanceTo: WiringEndpoint)


/**
 * Creates a graph of QRCode connections
 */
object QRMachineGraph {

  def apply(graphObserver: GraphObserver): QRMachineGraph = {
    new QRMachineGraph(graphObserver: GraphObserver)
  }

  implicit class GraphExt(graph: Graph) {
    def addOrGetNode(id: String): Node = {
      if (graph.getNode(id) == null) graph.addNode(id) else graph.getNode(id)
    }

    private def addOrGetEdge(nodeFrom: String, nodeTo: String): Edge = {
      val id = generateEdgeId(nodeFrom, nodeTo)
      if (graph.getEdge(id) == null) graph.addEdge(id, nodeFrom, nodeTo) else graph.getEdge(id)
    }

    def addOrGetEdge(nodeFrom: Node, nodeTo: Node): Edge = {
      val nodeFromId = nodeFrom.getId
      val nodeToId = nodeTo.getId
      addOrGetEdge(nodeFromId, nodeToId)
    }

    def addOrGetEdge(nodeFrom: WiringEndpoint, nodeTo: WiringEndpoint): Edge = {
      val nodeFromId = QrCodeUtils.encodeWiringEndpointJson(nodeFrom)
      val nodeToId = QrCodeUtils.encodeWiringEndpointJson(nodeTo)
      addOrGetEdge(nodeFromId, nodeToId)
    }

    def removeEdgeBidirectional(nodeFrom: WiringEndpoint, nodeTo: String): List[Edge] = {
      val fromToId = generateEdgeId(QrCodeUtils.encodeWiringEndpointJson(nodeFrom), nodeTo)
      val toFromId = generateEdgeId(nodeTo, QrCodeUtils.encodeWiringEndpointJson(nodeFrom))
      val removedFromTo = Try(graph.removeEdge(fromToId)).getOrElse(null)
      val removedToFrom = Try(graph.removeEdge(toFromId)).getOrElse(null)
      List(removedFromTo, removedToFrom).filter(edge => edge != null)
    }

    private def generateEdgeId(nodeFrom: String, nodeTo: String): String = {
      val edgeId = if (nodeFrom > nodeTo) CapabilityInstanceEdge.fromNodeIds(nodeFrom, nodeTo) else CapabilityInstanceEdge.fromNodeIds(nodeFrom, nodeTo)
      edgeId match {
        case Some(edge) => CapabilityInstanceEdge.toJson(edge)
        case None => if (nodeFrom > nodeTo) s"$nodeFrom$nodeTo" else s"$nodeTo$nodeFrom" //This is a machine, endpoint link, so no valid capabilityInstance edge will be created
      }
    }
  }
}

trait GraphObserver {
  def onLayoutChanged(graph: Graph): Unit
}

class QRMachineGraph(graphObserver: GraphObserver) {
  private val defaultStyleSheetUrl = getClass.getClassLoader.getResource("stylesheets/graph.stylesheet").toURI
  println(defaultStyleSheetUrl)
  private val LABEL = "ui.label"
  private val machineGraph = createGraphInstance()
  machineGraph.addElementSink(new GraphElementSink(machineGraph,
    addedEdge => WiringClients.wireMachines(addedEdge),
    removedEdge => WiringClients.disconnectMachines(removedEdge),
    machineGraph => graphObserver.onLayoutChanged(machineGraph)))
  machineGraph.display

  def addGraphElements(machineGraphElements: (Set[WiringEndpoint], Set[CapabilityInstanceEdge])): Graph = {
    val (nodes, edges) = machineGraphElements
    addNodes(nodes)
    addEdges(edges)
    machineGraph
  }

  def clearGraphElements(): Unit = {
    machineGraph.clear()
    machineGraph.setAttribute("ui.stylesheet", s"url('${defaultStyleSheetUrl.toString}')")
    machineGraph.setAttribute("ui.quality")
    machineGraph.setAttribute("ui.antialias")
  }

  private def addNodes(nodes: Set[WiringEndpoint]): Unit = {
    nodes.foreach(node => {
      val machineId = node.machineId
      val machineCapabilityInfo = WiringClients.getTopLevelCapabilityForMachineId(machineId)
      val machineNode = machineGraph.addOrGetNode(MachineCapabilityInfo.encodeToJson(machineCapabilityInfo))
      machineNode.setAttribute(LABEL, machineId)
      machineNode.setAttribute("ui.class", "machine")

      val capabilityNode = machineGraph.addOrGetNode(QrCodeUtils.encodeWiringEndpointJson(node))
      capabilityNode.setAttribute(LABEL, node.localCapabilityId)
      capabilityNode.setAttribute("ui.class", "wiringEndpoint")

      machineGraph.addOrGetEdge(machineNode, capabilityNode)
    })
  }

  private def addEdges(edges: Set[CapabilityInstanceEdge]): Unit = {
    edges.foreach(edge => {
      val endpointFrom = edge.capInstanceFrom
      val endpointTo = edge.capInstanceTo
      removePreviousLinks(endpointFrom, endpointTo)
      machineGraph.addOrGetEdge(endpointFrom, endpointTo)
    })
  }

  private def removePreviousLinks(endpointFrom: WiringEndpoint, endpointTo: WiringEndpoint): Unit = {
    val prevEndpointsFrom = collectNodesForWiringEndpoint(endpointFrom)
    val prevEndpointTo = collectNodesForWiringEndpoint(endpointTo)
    if (prevEndpointsFrom.nonEmpty) {
      println("From node has neighbours")
      prevEndpointsFrom.foreach(node => machineGraph.removeEdgeBidirectional(endpointFrom, node.getId))
    }
    if (prevEndpointTo.nonEmpty) {
      println("To node has neighbours")
      prevEndpointTo.foreach(node => machineGraph.removeEdgeBidirectional(endpointTo, node.getId))
    }
  }

  private def collectNodesForWiringEndpoint(endpointFrom: WiringEndpoint): List[Node] = {
    machineGraph.getNode(QrCodeUtils.encodeWiringEndpointJson(endpointFrom))
      .neighborNodes()
      .filter(node => MachineCapabilityInfo.parseToJson(node.getId).isEmpty)
      .iterator().asScala.toList//.toScala(List)
  }

  private def createGraphInstance(): Graph = {
    val graph = new MultiGraph("CapInstanceGraph")
    System.setProperty("org.graphstream.ui", "swing")
    graph.setAttribute("ui.stylesheet", s"url('${defaultStyleSheetUrl.toString}')")
    graph.setAttribute("ui.quality")
    graph.setAttribute("ui.antialias")
    graph
  }

  private class GraphElementSink(graph: Graph,
                                 onEdgeAdded: CapabilityInstanceEdge => Unit,
                                 onEdgeRemoved: CapabilityInstanceEdge => Unit,
                                 onLayoutChanged: Graph => Unit) extends ElementSink {
    override def nodeAdded(sourceId: String, timeId: Long, nodeId: String): Unit = {

    }

    override def nodeRemoved(sourceId: String, timeId: Long, nodeId: String): Unit = {

    }

    override def edgeAdded(sourceId: String, timeId: Long, edgeId: String,
                           fromNodeId: String, toNodeId: String, directed: Boolean): Unit = {
      //println(s"Added new edge: $edgeId. From: $fromNodeId, To: $toNodeId")
      CapabilityInstanceEdge.fromNodeIds(fromNodeId, toNodeId) match {
        case Some(edge) =>
          println(s"Calling handler on new edge: $edge")
          onEdgeAdded(edge)
          onLayoutChanged(graph)
        case None => //println(s"Ignoring parent-child link: $edgeId. From: $fromNodeId, To: $toNodeId")
      }
    }

    override def edgeRemoved(sourceId: String, timeId: Long, edgeId: String): Unit = {
      //println(s"Edge $edgeId was removed, parsed=${CapabilityInstanceEdge.fromJson(edgeId)}")
      CapabilityInstanceEdge.fromJson(edgeId) match {
        case Success(edge) =>
          onEdgeRemoved(edge)
          onLayoutChanged(graph)
        case Failure(_) => //println(s"The edge to be removed is not a capability instance edge")
      }
    }

    override def graphCleared(sourceId: String, timeId: Long): Unit = {}

    override def stepBegins(sourceId: String, timeId: Long, step: Double): Unit = {}
  }

}


