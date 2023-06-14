package commons

import boofcv.alg.fiducial.qrcode.QrCode
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import georegression.struct.point.Point2D_F64
import scala.util.{Try, Success, Failure}

import java.awt.image.BufferedImage
import scala.util.Try

case class WiringEndpoint(machineId: String, localCapabilityId: String, capabilityFolder: String)

object QrCodeUtils {

  private val mapper: JsonMapper = JsonMapper
    .builder().
    addModule(DefaultScalaModule)
    .build()

  def encodeWiringEndpointJson(wiringEndpoint: WiringEndpoint): String = {
    mapper.writeValueAsString(wiringEndpoint)
  }

  def decodeWiringEndpointJson(data: String): Option[WiringEndpoint] = {
    val decodedValue = Try(mapper.readValue(data, classOf[WiringEndpoint]))
    decodedValue match {
      case Success(value) => Option(value)
      case Failure(exception) => Option.empty
    }
  }

  implicit class QrCodeExt(qrCode: io.nayuki.qrcodegen.QrCode) {
    def toBufferedImage(): BufferedImage = {
      if (qrCode.size + 4 * 2L > Integer.MAX_VALUE / 10) throw new IllegalArgumentException("Scale or border too large")

      val result = new BufferedImage((qrCode.size + 4 * 2) * 10, (qrCode.size + 4 * 2) * 10, BufferedImage.TYPE_INT_RGB)
      var y = 0
      while (y < result.getHeight) {
        var x = 0
        while (x < result.getWidth) {
          val color = qrCode.getModule(x / 10 - 4, y / 10 - 4)
          result.setRGB(x, y, if (color) 0 else 16777215)
          x += 1
        }
        y += 1
      }
      result
    }
  }
}

