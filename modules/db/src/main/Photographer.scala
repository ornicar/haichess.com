package lila.db

import java.io.File

import com.sksamuel.scrimage.nio.JpegWriter
import dsl._

final class Photographer(coll: Coll, prefix: String) {

  import Photographer.uploadMaxMb
  private val uploadMaxBytes = uploadMaxMb * 1024 * 1024
  private def pictureId(id: String) = s"$prefix:$id"

  def apply(id: String, uploaded: Photographer.Uploaded, processFile: Boolean = true): Fu[DbImage] =
    if (uploaded.ref.file.length > uploadMaxBytes)
      fufail(s"File size must not exceed ${uploadMaxMb}MB.")
    else {
      val image = DbImage.make(
        id = pictureId(id),
        name = sanitizeName(uploaded.filename),
        contentType = uploaded.contentType,
        file = uploaded.ref.file
      )
      coll.update($id(image.id), image, upsert = true)

      if (processFile) {
        process(uploaded.ref.file)
        val image = DbImage.make(
          id = s"${pictureId(id)}:128",
          name = sanitizeName(uploaded.filename),
          contentType = uploaded.contentType,
          file = uploaded.ref.file
        )
        return coll.update($id(image.id), image, upsert = true) inject (image)
      }
      fuccess(image)
    }

  private def process(file: File) = {
    import com.sksamuel.scrimage._
    implicit def writer: JpegWriter = JpegWriter.Default
    Image.fromFile(file).cover(128, 128).output(file)
  }

  private def sanitizeName(name: String) = {
    // the char `^` breaks play, even URL encoded
    java.net.URLEncoder.encode(name, "UTF-8").replaceIf('%', "")
  }
}

object Photographer {

  val uploadMaxMb = 3

  type Uploaded = play.api.mvc.MultipartFormData.FilePart[play.api.libs.Files.TemporaryFile]

}
