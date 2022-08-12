import Dependencies._
import sbt.Keys.libraryDependencies

ThisBuild / scalaVersion := "2.13.8"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.example"
ThisBuild / organizationName := "example"

enablePlugins(PackPlugin)

val appName = "svshi-web-service"

lazy val DeepIntegrationTest = IntegrationTest.extend(Test)

lazy val root = (project in file("."))
  .configs(DeepIntegrationTest)
  .settings(
    name := "web-service-backend",
    Defaults.itSettings,
    libraryDependencies += scalaTest % Test,
    libraryDependencies += "com.lihaoyi" %% "cask" % "0.8.3",
    libraryDependencies += "com.lihaoyi" %% "os-lib" % "0.8.1",
    libraryDependencies += "com.lihaoyi" %% "upickle" % "2.0.0",
    libraryDependencies += "com.lihaoyi" %% "mainargs" % "0.2.3",
    libraryDependencies += "com.lihaoyi" %% "requests" % "0.7.1",
    libraryDependencies += "org.mockito" % "mockito-scala_2.13" % "1.17.7" % "test"
  )

packMain := Map(appName -> "ch.epfl.web.service.main.App")

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
