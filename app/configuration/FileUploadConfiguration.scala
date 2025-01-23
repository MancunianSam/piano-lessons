package configuration

import java.nio.file.Path

trait FileUploadConfiguration {
  def uploadImage(name: String, path: Path): String
}
