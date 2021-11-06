import Dependencies._

ThisBuild / scalaVersion     := "2.13.0"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "ch.epfl.core"
ThisBuild / organizationName := "epfl"

lazy val root = (project in file("."))
  .settings(
    name := "core",
    libraryDependencies += scalaTest % Test,
    libraryDependencies += "com.lihaoyi" %% "os-lib" % "0.7.8",
    libraryDependencies += "com.lihaoyi" %% "upickle" % "1.4.2",
    libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "2.0.1"
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.