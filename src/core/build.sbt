import Dependencies._

ThisBuild / scalaVersion     := "2.13.8"
ThisBuild / version          := "1.3.4-SNAPSHOT"
ThisBuild / organization     := "ch.epfl.core"
ThisBuild / organizationName := "epfl"

Test / parallelExecution := false

enablePlugins(PackPlugin)

val appName = "svshi"

lazy val root = (project in file("."))
  .settings(
    name := appName,
    libraryDependencies += scalaTest % Test,
    libraryDependencies += "com.lihaoyi" %% "os-lib" % "0.8.1",
    libraryDependencies += "com.lihaoyi" %% "upickle" % "1.5.0",
    libraryDependencies += "com.lihaoyi" %% "mainargs" % "0.2.3",
    libraryDependencies += "com.lihaoyi" %% "fansi" % "0.3.1",
    libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "2.0.1",
    libraryDependencies += "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.4",
    libraryDependencies += "com.github.tototoshi" %% "scala-csv" % "1.3.10"
  )

packMain := Map(appName -> "ch.epfl.core.Main")

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.