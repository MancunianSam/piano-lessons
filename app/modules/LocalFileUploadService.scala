package modules

import configuration.FileUploadConfiguration

import java.nio.file.Path

class LocalFileUploadService extends FileUploadConfiguration {

  override def uploadImage(name: String, path: Path): String = s"$name/$path"
}
