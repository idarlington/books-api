import Dependencies.*

ThisBuild / organization := "com.idarlington"

lazy val root = (project in file("."))
  .enablePlugins(DockerPlugin)
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "books-api",
    version := "0.1.0",
    scalaVersion := "2.12.17",
    scalacOptions ++= List(
      "-feature",
      "-Yrangepos",
      "-language:higherKinds",
      "-Ypartial-unification"
    ),
    dockerBaseImage := "openjdk:11-jre-slim-buster",
    dockerExposedPorts ++= Seq(8087, 9990),
    dockerRepository := None,
    testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
    libraryDependencies ++= Seq(
      CompilerPlugin.betterMonadicFor,
      Libraries.finchCore,
      Libraries.finchCirce,
      Libraries.finchRefined,
      Libraries.refined,
      Libraries.mules,
      Libraries.fs2Data,
      Libraries.fs2DataCirce,
      Libraries.twitterServer,
      Libraries.twitterServerSlf4j,
      Libraries.finagleStats,
      Libraries.circeParser,
      Libraries.circeGeneric,
      Libraries.circeExtras,
      Libraries.log4cats,
      Libraries.log4catsSlf4j,
      Libraries.cirisCore,
      Libraries.cirisEnum,
      Libraries.cirisRefined,
      Libraries.weaverTest
    )
  )
