import layoutTracker.utils.{ImageWithQRCodePairs, MockCamera, Visualization}
import layoutTracker.{QRCodeFinder, QRMachineGraph, QRToMachineMapper}
import org.graphstream.graph.Graph

import java.io.File
import scala.collection.compat.toTraversableLikeExtensionMethods

object LayoutTrackerApp {
  //val imageSource = "qrCodes/TestDevice"

  val imageSource = new File(getClass.getClassLoader.getResource("testImages/lab").getFile).toPath.toString
  val videoSource = new File(getClass.getClassLoader.getResource("testVideos/lab").getFile).toPath.toString

  def main(args: Array[String]): Unit = {
    //val camera = new DefaultWebCam(WebcamResolution.VGA)
    //val camera = new MockCamera(imageSource, "FIAB_QR_LAB_O")
    val camera = new MockCamera(imageSource, "FIAB_QR_LAB")
    //val camera = new MockMJPEGVideoCamera(videoFilePath = s"$videoSource/fiab/FIAB_2.mjpeg.avi")
    val gui = Visualization(camera.getNextImage)
    val graph = QRMachineGraph((_: Graph) => println("Yay!"))
    camera.getImagesAsStream
      .takeWhile(image => image != null)
      .map(image => image -> QRCodeFinder.getNeighbouringQrCodesFromImage(image))
      .tapEach { case (image, qrCodes) => gui.displayImage(ImageWithQRCodePairs(image, qrCodes.neighbours)) }
      .map { case (_, qrCodes) => QRToMachineMapper.mapQrCodesToGraphElements(qrCodes) }
      .foreach(qrCodeElements => {
        graph.addGraphElements(qrCodeElements)
      })
  }
}



