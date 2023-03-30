package utils

import com.dimafeng.testcontainers.scalatest.TestContainersForEach
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.dimafeng.testcontainers.JdbcDatabaseContainer.CommonParams
import org.scalatestplus.play.PlaySpec

trait TestContainersUtils extends PlaySpec with TestContainersForEach {
  override type Containers = PostgreSQLContainer

  override def startContainers(): PostgreSQLContainer = {
    val container = PostgreSQLContainer.Def(
      databaseName = "piano-lessons",
      username = "piano",
      password = "password",
      commonJdbcParams = CommonParams(initScriptPath = Option("init.sql"))
    ).createContainer()

    container.start()
    container
  }

}
