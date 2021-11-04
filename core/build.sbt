import Dependencies._

ThisBuild / scalaVersion     := "2.13.6"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "ch.epfl.smos"
ThisBuild / organizationName := "epfl"

lazy val root = (project in file("."))
  .settings(
    name := "core",
    libraryDependencies += scalaTest % Test,
    libraryDependencies += "com.lihaoyi" %% "upickle" % "1.4.2",
    libraryDependencies += "com.lihaoyi" %% "os-lib" % "0.7.8",
    libraryDependencies += "ru.tinkoff" %% "phobos-core" % "0.13.0"
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.