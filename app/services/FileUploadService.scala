package services

import configuration.FileUploadConfiguration
import software.amazon.awssdk.http.apache.ApacheHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.{PutObjectRequest, PutObjectResponse}

import java.nio.file.Path
import javax.inject.Inject

class FileUploadService @Inject() extends FileUploadConfiguration {
  private val imageBucket = "piano-lessons-news-feed-images"

  override def uploadImage(name: String, path: Path): String = {
    val httpClient = ApacheHttpClient.builder.build
    val client = S3Client.builder.httpClient(httpClient).region(Region.EU_WEST_2).build()
    val putObjectRequest = PutObjectRequest.builder.bucket(imageBucket).key(name).build
    client.putObject(putObjectRequest, path)
    name
  }
}
