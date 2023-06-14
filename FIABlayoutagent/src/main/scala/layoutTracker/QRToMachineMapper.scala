package layoutTracker

import boofcv.alg.fiducial.qrcode.QrCode
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import commons.{QrCodeUtils, WiringEndpoint}

object QRToMachineMapper {

  def mapQrCodesToGraphElements(qrCodePairs: QrCodeNeighbourhood): (Set[WiringEndpoint], Set[CapabilityInstanceEdge]) = {
    val machineLinks = mapQrCodesToMachineLinks(qrCodePairs.neighbours)
    val lonelyMachines = qrCodePairs.loners.map(qrCode => parseQRCodeToMachineLink(qrCode))
    val nodes = mapQrCodesToNodes(machineLinks) ++ (lonelyMachines)
    val edges = mapQrCodesToEdges(machineLinks)
    (nodes, edges)
  }

  private def mapQrCodesToNodes(machineLinks: List[(WiringEndpoint, WiringEndpoint)]): Set[WiringEndpoint] = {
    machineLinks
      .flatMap { case (a, b) => Seq(a, b) }
      .toSet
  }

  private def mapQrCodesToEdges(machineLinks: List[(WiringEndpoint, WiringEndpoint)]): Set[CapabilityInstanceEdge] = {
    machineLinks
      .flatMap { case (a, b) => Seq(CapabilityInstanceEdge(a, b)) }
      .toSet
  }

  private def mapQrCodesToMachineLinks(qrCodePairs: List[(QrCode, QrCode)]): List[(WiringEndpoint, WiringEndpoint)] = {
    qrCodePairs.map { case (a, b) => convertQrCodesToMachineLinks(a, b) }
  }

  private def convertQrCodesToMachineLinks(qrCodeA: QrCode, qrCodeB: QrCode): (WiringEndpoint, WiringEndpoint) = {
    val machineLinkA = parseQRCodeToMachineLink(qrCodeA)
    val machineLinkB = parseQRCodeToMachineLink(qrCodeB)
    (machineLinkA, machineLinkB)
  }

  private def parseQRCodeToMachineLink(qrCode: QrCode): WiringEndpoint = {
    QrCodeUtils.decodeWiringEndpointJson(qrCode.message).get
  }

}
