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
      "com.typesafe.play" %% "play" % "2.6.7",
      "com.criteo.lolhttp" %% "lolhttp" % "0.8.1",

      scalaTest % Test
    ),
    scalacOptions ++= Seq(
      "-encoding", "UTF-8",
      "-target:jvm-1.8",
      "-Ywarn-adapted-args",
      "-Ywarn-inaccessible",
      "-Ywarn-nullary-override",
      "-Ywarn-infer-any",
      "-Ywarn-dead-code",
      "-Ywarn-unused",
      "-Ywarn-unused-import",
      "-Ywarn-value-discard",
      "-Ywarn-macros:after",
      "-Ywarn-numeric-widen",
      "-Ypartial-unification",
      "-unchecked",
      "-deprecation",
      "-feature",
      "-g:vars",
      "-Xlint:_",
      "-opt:l:inline",
      "-opt-inline-from"
    )
  )
