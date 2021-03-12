package lila.db

import dsl._

final class FileUploader(coll: Coll, prefix: String) {

  import FileUploader.uploadMaxMb
  private val uploadMaxBytes = uploadMaxMb * 1024 * 1024
  private def fileId(id: String) = s"$prefix:$id"

  def apply(id: String, uploaded: Photographer.Uploaded): Fu[DbFile] = {
    if (uploaded.ref.file.length > uploadMaxBytes)
      fufail(s"File size must not exceed ${uploadMaxMb}MB.")
    else {
      val dbFile = DbFile.make(
        id = fileId(id),
        name = uploaded.filename,
        contentType = uploaded.contentType,
        file = uploaded.ref.file
      )
      coll.update($id(dbFile.id), dbFile, upsert = true) inject (dbFile)
    }
  }

  private def sanitizeName(name: String) = {
    java.net.URLEncoder.encode(name, "UTF-8").replaceIf('%', "")
  }
}

object FileUploader {

  val uploadMaxMb = 5

  type Uploaded = play.api.mvc.MultipartFormData.FilePart[play.api.libs.Files.TemporaryFile]

}

