import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.12.4",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "PlayWithIO",
    libraryDependencies ++= Seq(
      "scalaz-base" %% "scalaz-base" % "0.1-SNAPSHOT",
      "scalaz-effect" %% "scalaz-effect" % "0.1-SNAPSHOT",
      scalaTest % Test
    )
  )
