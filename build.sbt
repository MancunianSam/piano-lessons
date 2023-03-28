name := """piano-lessons"""
organization := "dev.sampalmer"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala).enablePlugins(SbtWeb)

lazy val testContainersVersion = "0.40.14"

scalaVersion := "2.13.10"
val circeVersion = "0.14.1"
val pac4jVersion = "5.7.0"

libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.11.4" % Test

libraryDependencies ++= Seq(
  guice,
  ws,
  cacheApi,
  ehcache,
  "com.google.api-client" % "google-api-client" % "1.32.1",
  "com.google.apis" % "google-api-services-calendar" % "v3-rev411-1.25.0",
  "com.google.auth" % "google-auth-library-oauth2-http" % "1.16.0",
  "com.typesafe.play" %% "play-slick" % "5.1.0",
  "org.postgresql" % "postgresql" % "42.5.4",
  "org.pac4j" %% "play-pac4j" % "11.1.0-PLAY2.8",
  "com.typesafe.slick" %% "slick-codegen" % "3.4.1",
  "org.pac4j" % "pac4j-http" % pac4jVersion exclude("com.fasterxml.jackson.core", "jackson-databind"),
  "org.pac4j" % "pac4j-oidc" % pac4jVersion exclude("commons-io", "commons-io") exclude("com.fasterxml.jackson.core", "jackson-databind"),
  "ch.qos.logback" % "logback-classic" % "1.4.5",
  "com.stripe" % "stripe-java" % "22.11.0",
  "com.softwaremill.sttp.client3" %% "core" % "3.8.13",
  "com.softwaremill.sttp.client3" %% "async-http-client-backend-future" % "3.8.13"
) ++ Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

libraryDependencies ++= Seq(
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test,
  "org.scalatestplus" %% "mockito-3-4" % "3.2.10.0" % Test,
  "org.mockito" % "mockito-core" % "2.7.22" % Test,
  "com.github.tomakehurst" % "wiremock-jre8" % "2.35.0" % Test,
  "com.dimafeng" %% "testcontainers-scala-scalatest" % testContainersVersion % Test,
  "com.dimafeng" %% "testcontainers-scala-postgresql" % testContainersVersion % Test
)

pipelineStages := Seq(cssCompress, digest)

lazy val databasePort = sys.env.getOrElse("DB_PORT", "5432")
lazy val databaseUser = "piano"
lazy val databasePassword = "password"

lazy val slick = taskKey[Seq[File]]("Generate Tables.scala")
slick := {
  val dir = (Compile / sourceManaged).value
  val outputDir = dir / "slick"
  val url = s"jdbc:postgresql://localhost:$databasePort/piano-lessons"
  val jdbcDriver = "org.postgresql.Driver"
  val slickDriver = "slick.jdbc.PostgresProfile"
  val pkg = "repositories"

  val cp = (Compile / dependencyClasspath).value
  val s = streams.value

  runner.value.run("slick.codegen.SourceCodeGenerator",
    cp.files,
    Array(slickDriver, jdbcDriver, url, outputDir.getPath, pkg, databaseUser, databasePassword),
    s.log).failed foreach (sys error _.getMessage)

  val file = outputDir / pkg / "Tables.scala"

  Seq(file)
}

