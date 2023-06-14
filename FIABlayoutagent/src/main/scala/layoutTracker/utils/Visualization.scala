package layoutTracker.utils

import boofcv.alg.fiducial.qrcode.QrCode
import boofcv.gui.feature.VisualizeShapes
import boofcv.gui.image.{ImagePanel, ScaleOptions, ShowImages}
import layoutTracker.QRCodeFinder.CenterQrCode

import java.awt.{BasicStroke, BorderLayout, Color, Dimension, Graphics2D, Toolkit}
import java.awt.image.BufferedImage
import javax.swing.{JFrame, SwingUtilities, WindowConstants}

final case class ImageWithQRCodes(bufferedImage: BufferedImage, qrCodes: List[QrCode])

final case class ImageWithQRCodePairs(bufferedImage: BufferedImage, qrPairs: List[(QrCode, QrCode)])

object Visualization {
  def apply(initialImage: BufferedImage): Visualization = new Visualization(initialImage)
}

class Visualization(val initialImage: BufferedImage) {

  private val imagePanel: ImagePanel = ShowImages.showWindow(initialImage, "QR Code Detection View", true)

  def displayImage(nextImage: Any): Unit = {
    nextImage match {
      case image: BufferedImage => displayOriginalImage(image)
      case image =>
        val modifiedImage = image match {
          case ImageWithQRCodes(bufferedImage, qrCodes) => markQrCodes(bufferedImage, qrCodes)
          case ImageWithQRCodePairs(bufferedImage, qrPairs) => markQrCodeConnections(bufferedImage, qrPairs)
        }
        displayModifiedImage(modifiedImage)
    }
  }

  private def displayOriginalImage(nextImage: BufferedImage): Unit = {
    imagePanel.setImageRepaint(nextImage)
  }

  private def displayModifiedImage(modifiableImage: ModifiableImage): Unit = {
    imagePanel.setImageRepaint(modifiableImage.bufferedImage)
  }

  private def markQrCodes(nextImage: BufferedImage, qrCodes: List[QrCode]): ModifiableImage = {
    val modifiableImage = ModifiableImage(nextImage)
    modifiableImage.markQrCodesInImage(qrCodes)
  }

  private def markQrCodeConnections(nextImage: BufferedImage, qrCodePairs: List[(QrCode, QrCode)]): ModifiableImage = {
    val qrCodes = qrCodePairs.flatMap { case (a, b) => List(a, b) }.distinct
    val imageWithMarkedQrCodes = markQrCodes(nextImage, qrCodes)
    ModifiableImage(imageWithMarkedQrCodes.bufferedImage)
      .markQrCodesInImage(qrCodes)
      .markConnectedQrCodes(qrCodePairs)
  }
}

object ModifiableImage {
  def apply(image: BufferedImage, graphics2D: Graphics2D): ModifiableImage = new ModifiableImage(image, graphics2D)

  def apply(image: BufferedImage): ModifiableImage = extractGraphicsFromImage(image)

  private def extractGraphicsFromImage(image: BufferedImage): ModifiableImage = {
    val stroke = new BasicStroke(Math.max(4f, (image.getWidth() / 200).toFloat))
    val graphics2D = image.createGraphics()
    graphics2D.setStroke(stroke)
    ModifiableImage(image, graphics2D)
  }
}

case class ModifiableImage(bufferedImage: BufferedImage, graphics2D: Graphics2D) {

  def markQrCodesInImage(qrCodes: List[QrCode]): ModifiableImage = {
    graphics2D.setColor(Color.GREEN)
    for (qrCode <- qrCodes) {
      VisualizeShapes.drawPolygon(qrCode.bounds, true, graphics2D)
      val qrCodeCenter = qrCode.centerPoint
      graphics2D.drawOval((qrCodeCenter.x - 2).toInt, (qrCodeCenter.y - 2).toInt, 4, 4)
      graphics2D.drawString(qrCode.message, qrCodeCenter.x.toInt, qrCodeCenter.y.toInt)
    }
    copy(graphics2D = graphics2D)
  }

  def markConnectedQrCodes(qrCodePairs: List[(QrCode, QrCode)]): ModifiableImage = {
    graphics2D.setColor(Color.CYAN)
    for ((qrThis, qrOther) <- qrCodePairs) {
      graphics2D.drawLine(qrThis.centerPoint.x.toInt, qrThis.centerPoint.y.toInt,
        qrOther.centerPoint.x.toInt, qrOther.centerPoint.y.toInt)
    }
    copy(graphics2D = graphics2D)
  }
}
