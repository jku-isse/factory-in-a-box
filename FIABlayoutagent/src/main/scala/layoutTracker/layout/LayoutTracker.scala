package layoutTracker.layout

import commons.{QrCodeUtils, WiringEndpoint}
//import fiab.shopfloorutils.actor.AkkaActorBackedCoreModelAbstractActor
//import fiab.shopfloorutils.transport.TransportRoutingInterface.{Position, UNKNOWN_POSITION}
//import fiab.shopfloorutils.transport.{InternalCapabilityToPositionMapping, TransportPositionLookupInterface, TransportPositionParser, TransportRoutingInterface}
import layoutTracker.{QRCodeFinder, QRMachineGraph, QRToMachineMapper}
import layoutTracker.utils.{ImageSequenceGenerator, ImageWithQRCodePairs, MockCamera, Visualization}
import layoutTracker.wiring.MachineCapabilityInfo
import org.graphstream.graph.Graph

import scala.jdk.CollectionConverters._
import java.util
import java.util.Optional
import java.util.stream.{Collectors, IntStream}
import scala.collection.compat.toTraversableLikeExtensionMethods
import scala.collection.mutable

trait LayoutChangedListener {

  def onLayoutChanged(graph: Graph): Unit
}

object LayoutTracker {

  def apply(imageSequenceGenerator: ImageSequenceGenerator, onLayoutChanged: Graph => Unit): LayoutTracker = {
    new LayoutTracker(imageSequenceGenerator, (graph: Graph) => onLayoutChanged(graph))
  }

  def apply(imageSequenceGenerator: ImageSequenceGenerator, layoutChangedListener: LayoutChangedListener): LayoutTracker = {
    new LayoutTracker(imageSequenceGenerator, layoutChangedListener)
  }
}

class LayoutTracker(var imageSequenceGenerator: ImageSequenceGenerator,
                    layoutChangedListener: LayoutChangedListener) {

  private val gui = Visualization(imageSequenceGenerator.peekNextImage)
  private val graph = QRMachineGraph(graph => layoutChangedListener.onLayoutChanged(graph))

  def setImageSequenceGenerator(imageSequenceGenerator: ImageSequenceGenerator): Unit = {
    this.imageSequenceGenerator = imageSequenceGenerator
  }

  //Use either all or next, thread safety is not guaranteed!
  def processNextImage(): Boolean = {
    val image = imageSequenceGenerator.getNextImage
    if (image != null) {
      val neighbourhood = QRCodeFinder.getNeighbouringQrCodesFromImage(image)
      gui.displayImage(ImageWithQRCodePairs(image, neighbourhood.neighbours))
      val graphElements = QRToMachineMapper.mapQrCodesToGraphElements(neighbourhood)
      graph.addGraphElements(graphElements)
    }
    image != null
  }

  def processAllImages(): Unit = {
    imageSequenceGenerator
      .getImagesAsStream
      .takeWhile(image => image != null)
      .map(image => image -> QRCodeFinder.getNeighbouringQrCodesFromImage(image))
      .tapEach { case (image, qrCodes) => gui.displayImage(ImageWithQRCodePairs(image, qrCodes.neighbours)) }
      .map { case (_, qrCodes) => QRToMachineMapper.mapQrCodesToGraphElements(qrCodes) }
      .foreach(qrCodeElements => {
        graph.addGraphElements(qrCodeElements)
      })
  }

  def clearGraph(): Unit = {
    graph.clearGraphElements()
  }
}

