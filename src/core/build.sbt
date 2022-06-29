import Dependencies._

ThisBuild / scalaVersion := "2.13.8"
ThisBuild / version := "1.4.1-SNAPSHOT"
ThisBuild / organization := "ch.epfl"
ThisBuild / organizationName := "epfl"

name:= "svshi-core"

Test / parallelExecution := false

lazy val DeepIntegrationTest = IntegrationTest.extend(Test)

enablePlugins(PackPlugin)

val appName = "svshi"

lazy val root = (project in file("."))
  .configs(DeepIntegrationTest)
  .settings(
    name := appName,
    Defaults.itSettings,
    libraryDependencies += scalaTest % DeepIntegrationTest,
    libraryDependencies += scalaTest % Test,
    libraryDependencies += "com.lihaoyi" %% "os-lib" % "0.8.1",
    libraryDependencies += "com.lihaoyi" %% "upickle" % "1.5.0",
    libraryDependencies += "com.lihaoyi" %% "mainargs" % "0.2.3",
    libraryDependencies += "com.lihaoyi" %% "fansi" % "0.3.1",
    libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "2.0.1",
    libraryDependencies += "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.4",
    libraryDependencies += "com.github.tototoshi" %% "scala-csv" % "1.3.10",
    libraryDependencies += "com.lihaoyi" %% "cask" % "0.8.0",
    libraryDependencies += "com.lihaoyi" %% "requests" % "0.7.0",
    libraryDependencies += "org.mockito" % "mockito-scala_2.13" % "1.17.5" % "test"
  )

packMain := Map(appName -> "ch.epfl.core.Main")

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
