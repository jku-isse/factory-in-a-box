package layoutTracker.utils

import boofcv.io.wrapper.DefaultMediaManager
import boofcv.struct.image.{GrayU8, ImageType}
import com.github.sarxos.webcam.ds.ipcam.{IpCamDeviceRegistry, IpCamDriver, IpCamMode}
import com.github.sarxos.webcam.{Webcam, WebcamResolution}
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.javacv.{FFmpegFrameGrabber, Java2DFrameConverter}
import org.jcodec.api.FrameGrab
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.model.Picture
import org.jcodec.scale.AWTUtil

import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.{Path, Paths}
import javax.imageio.ImageIO
import scala.collection.compat.immutable.LazyList
import scala.util.{Failure, Success, Try}

trait ImageSequenceGenerator {

  def getImagesAsStream: Seq[BufferedImage]

  def getNextImage: BufferedImage

  def peekNextImage: BufferedImage
}

class DroidCam(val ipAddress: String) extends ImageSequenceGenerator {
  Webcam.setDriver(new IpCamDriver())
  IpCamDeviceRegistry.register("DroidCam", s"${ipAddress}/mjpegfeed?680x480", IpCamMode.PUSH)
  private val webCams = Webcam.getWebcams(5000)
  private val webCam = webCams.get(0)
  webCam.open()

  override def getImagesAsStream: Seq[BufferedImage] = {
    LazyList.continually(webCam.getImage)
  }

  override def getNextImage: BufferedImage = webCam.getImage

  override def peekNextImage: BufferedImage = ???
}

class DefaultWebCam(val webcamResolution: WebcamResolution) extends ImageSequenceGenerator {

  private val webCam: Webcam = {
    val webCam = Webcam.getDefault
    webCam.setViewSize(webcamResolution.getSize)
    webCam.open()
    webCam
  }

  override def getImagesAsStream: Seq[BufferedImage] = {
    LazyList.continually(webCam.getImage)
  }

  override def getNextImage: BufferedImage = webCam.getImage

  override def peekNextImage: BufferedImage = ???
}

class MockMP4VideoCamera(videoFilePath: String) extends ImageSequenceGenerator {

  val file = new File(videoFilePath)
  val frameGrab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(file))

  override def getImagesAsStream: Seq[BufferedImage] = {

    LazyList.continually {
      val frame = frameGrab.getNativeFrame
      if (frame == null) null else AWTUtil.toBufferedImage(frame)
    }
  }

  override def getNextImage: BufferedImage = {
    val picture: Picture = frameGrab.getNativeFrame
    AWTUtil.toBufferedImage(picture)
  }

  override def peekNextImage: BufferedImage = getNextImage
}

class MockMJPEGVideoCamera(videoFilePath: String) extends ImageSequenceGenerator {

  avutil.av_log_set_level(avutil.AV_LOG_ERROR)
  private val frameConverter = new Java2DFrameConverter()
  private val frameGrabber = new FFmpegFrameGrabber(videoFilePath)
  frameGrabber.start()

  override def getImagesAsStream: Seq[BufferedImage] = {
    LazyList.continually {
      val frame = frameGrabber.grabImage()
      val image = frameConverter.convert(frame) //new BufferedImage(frame.imageWidth, frame.imageHeight, BufferedImage.TYPE_INT_ARGB)
      //Java2DFrameConverter.copy(frame, image)
      if (image == null) null else image
    }
  }

  override def getNextImage: BufferedImage = {
    val frame = frameGrabber.grabImage()
    val image = frameConverter.convert(frame)
    if (image == null) null else image
  }

  override def peekNextImage: BufferedImage = getNextImage
}

class MockCamera(val imagesFolder: String, val fileNameFilter: String = ".png") extends ImageSequenceGenerator {

  val rootPath = Paths.get("").toAbsolutePath.toString
  val resourcesPath: Path = Paths.get(imagesFolder).toAbsolutePath
  val images = LazyList.from(getListOfFiles(resourcesPath.toString)
    .filter(path => path.toString.contains(fileNameFilter))
    .map(file => ImageIO.read(file)))
  private var remainingImages = images

  private def getListOfFiles(dir: String): List[File] = {
    val d = new File(dir)
    if (d.exists && d.isDirectory) {
      d.listFiles.filter(_.isFile).toList
    } else {
      List[File]()
    }
  }

  override def getImagesAsStream: Seq[BufferedImage] = {
    images
  }

  override def peekNextImage: BufferedImage = {
    val nextImage = Try(remainingImages.head) match {
      case Success(image) =>
        image
      case Failure(_) => null
    }
    nextImage
  }

  override def getNextImage: BufferedImage = {
    val nextImage = Try(remainingImages.head) match {
      case Success(image) =>
        remainingImages = remainingImages.tail
        image
      case Failure(_) => null
    }
    nextImage
  }
}

