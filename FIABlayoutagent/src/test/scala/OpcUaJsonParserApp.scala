import commons.QrCodeUtils.QrCodeExt
import io.nayuki.qrcodegen.QrCode
import layoutTracker.utils.Visualization
import qrCodeGenerator.OpcUaNodesToJsonParser

import java.io.File
import javax.imageio.ImageIO

object OpcUaJsonParserApp {

  private val filePathPrefix = s"${System.getProperty("user.dir")}/qrCodes/"

  private val endpoints = List(
    "opc.tcp://127.0.0.1:4840",
    "opc.tcp://127.0.0.1:4841",
    "opc.tcp://127.0.0.1:4842",
    "opc.tcp://127.0.0.1:4843",
    "opc.tcp://127.0.0.1:4844",
    "opc.tcp://127.0.0.1:4845",
    "opc.tcp://127.0.0.1:4846",
    "opc.tcp://127.0.0.1:4847",
  )

  def main(args: Array[String]): Unit = {
    endpoints.foreach(endpoint => {
      val capabilityInfos = OpcUaNodesToJsonParser(endpoint).parseCapabilityInfosForMachine()
      capabilityInfos.foreach { case (machineId, info) =>
        println(s"Parsed Info for machine $machineId: $info")
        val qrCode = QrCode.encodeText(info, QrCode.Ecc.LOW)
        val qrCodeAsImg = qrCode.toBufferedImage()
        new Visualization(qrCodeAsImg)
        val outputFile = new File(s"$filePathPrefix$machineId.png")
        ImageIO.write(qrCodeAsImg, "png", outputFile)
      }
    })
  }
}
