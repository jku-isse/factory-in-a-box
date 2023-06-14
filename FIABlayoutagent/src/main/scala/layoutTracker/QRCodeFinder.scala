package layoutTracker

import boofcv.alg.fiducial.qrcode.QrCode
import boofcv.factory.fiducial.{ConfigQrCode, FactoryFiducial}
import boofcv.io.image.ConvertBufferedImage
import boofcv.struct.image.GrayU8
import georegression.struct.point.Point2D_F64

import java.awt.image.BufferedImage
import scala.jdk.CollectionConverters.asScalaIteratorConverter

case class QrCodeNeighbourhood(neighbours: List[(QrCode, QrCode)], loners: List[QrCode])


/**
 * Finds the QR Codes and their location (center point) in an image
 */
object QRCodeFinder {

  /**
   * Retrieves all Qr Codes from an image
   */
  def getQrCodesFromImage(image: BufferedImage): List[QrCode] = {
    val qrCodeDetector = FactoryFiducial.qrcode(new ConfigQrCode(), classOf[GrayU8])
    val grayscaleImage = ConvertBufferedImage.convertFrom(image, new GrayU8())
    qrCodeDetector.process(grayscaleImage)
    qrCodeDetector.getDetections.stream().iterator().asScala.toList//.toScala(List)
  }

  /**
   * Retrieves all neighbouring QR Codes in an image
   */
  def getNeighbouringQrCodesFromImage(image: BufferedImage): QrCodeNeighbourhood = {
    val detectedQrCodes = getQrCodesFromImage(image)
    val qrCodePairs = getQRCodePairs(detectedQrCodes)
    val threshold = calculateThreshold(qrCodePairs)
    val neighbours = calculateQRCodeDistances(qrCodePairs)
      .filter { case (_, _, distance) => distance <= threshold }
      .map { case (a, b, _) => (a, b) }
    val neighbouringQrCodes = neighbours.flatMap { case (a, b) => Seq(a, b) }
    val loners = detectedQrCodes.filter(qrCode => !neighbouringQrCodes.contains(qrCode))
    //println(s"neighbours=$neighbouringQrCodes, outliers=$loners")
    QrCodeNeighbourhood(neighbours, loners)
  }

  /**
   * Gets all QR Codes as pairs.
   * A commutative pair (a,b) and (b,a) is seen as identical and one will be discarded
   * Reflexive pairs (a,a) will be discarded as well
   */
  private def getQRCodePairs(qrCodes: List[QrCode]): List[(QrCode, QrCode)] = {
    //Performs a cartesian product of all qr codes, while skipping reflexive and commutative pairs
    qrCodes
      .flatMap(qrCode => qrCodes.map(other => qrCode -> other))
      .filter { case (a, b) => a.centerPoint.hashCode() != b.centerPoint.hashCode() }
      .map { case (a, b) => if (a.centerPoint.hashCode() > b.centerPoint.hashCode()) (a, b) else (b, a) }
      .distinct
  }

  /**
   * Calculates the threshold where two QR Codes are seen as neighbours
   */
  private def calculateThreshold(qrCodePairs: List[(QrCode, QrCode)]): Double = {
    val qrCodeDistances = calculateQRCodeDistances(qrCodePairs).map { case (_, _, distance) => distance }
    val sumDistances = qrCodeDistances.sum
    val numDistances = qrCodeDistances.size

    def averageDistance: Double = sumDistances / numDistances

    val threshold = numDistances match {
      case i if i >= 1 && i < 3 => qrCodeDistances.min
      case _ => averageDistance * 0.45
    }
    threshold
  }

  /**
   * Calculates the distance between two QR Codes
   */
  private def calculateQRCodeDistances(qrCodePairs: List[(QrCode, QrCode)]): List[(QrCode, QrCode, Double)] = {
    qrCodePairs.map {
      case (a, b) =>
        val distance = a.centerPoint.distance(b.centerPoint)
        (a, b, distance)
    }
  }

  implicit class CenterQrCode(qrCode: QrCode) {

    def centerPoint: Point2D_F64 = {
      val topLeftPos: Point2D_F64 = qrCode.bounds.get(0)
      val bottomRightPos: Point2D_F64 = qrCode.bounds.get(2)
      new Point2D_F64((topLeftPos.x + bottomRightPos.x) / 2, (topLeftPos.y + bottomRightPos.y) / 2)
    }
  }
}