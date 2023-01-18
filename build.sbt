ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "kafka-perf",
    libraryDependencies ++= {

      val zioVersion = "2.0.6"

      Seq(
        // ZIO
        "dev.zio" %% "zio-streams" % zioVersion,
        "dev.zio" %% "zio-kafka" % "2.0.5",

        "io.monix" %% "monix" % "3.4.1"
      )
    }
  )
