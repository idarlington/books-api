import sbt._

object Dependencies {

  object Versions {
    val finch            = "0.34.1"
    val circe            = "0.14.3"
    val ciris            = "3.4.0"
    val fs2Data          = "1.9.1"
    val logback          = "1.4.7"
    val log4cats         = "2.6.0"
    val mules            = "0.7.0"
    val upickle          = "3.1.3"
    val weaver           = "0.8.3"
    val scalatest        = "3.2.15"
    val twitterServer    = "22.12.0"
    val refined          = "0.11.0"
    val betterMonadicFor = "0.3.1"
  }

  object Libraries {
    def circe(artifact: String): ModuleID = "io.circe" %% s"circe-$artifact" % Versions.circe
    def ciris(artifact: String): ModuleID = "is.cir"   %% artifact           % Versions.ciris

    def finch(artifact: String): ModuleID =
      "com.github.finagle" %% s"finch-${artifact}" % Versions.finch

    val refined = "eu.timepit" %% "refined" % Versions.refined

    val finchCore    = finch("core")
    val finchCirce   = finch("circe")
    val finchFs2     = finch("fs2")
    val finchRefined = finch("refined")

    val fs2Data      = "org.gnieh" %% "fs2-data-json"       % Versions.fs2Data
    val fs2DataCirce = "org.gnieh" %% "fs2-data-json-circe" % Versions.fs2Data

    val twitterServer = "com.twitter"   %% "twitter-server"  % Versions.twitterServer
    val finagleStats  = "com.twitter"   %% "finagle-stats"   % Versions.twitterServer
    val catbird       = "org.typelevel" %% "catbird-finagle" % Versions.twitterServer

    val circeCore    = circe("core")
    val circeGeneric = circe("generic")
    val circeParser  = circe("parser")
    val circeExtras  = circe("generic-extras")

    val cirisCore    = ciris("ciris")
    val cirisEnum    = ciris("ciris-enumeratum")
    val cirisRefined = ciris("ciris-refined")

    val mules   = "io.chrisdavenport" %% "mules"   % Versions.mules
    val upickle = "com.lihaoyi"       %% "upickle" % Versions.upickle

    val log4cats      = "org.typelevel" %% "log4cats-core"  % Versions.log4cats
    val log4catsSlf4j = "org.typelevel" %% "log4cats-slf4j" % Versions.log4cats

    val twitterServerSlf4j = "com.twitter" %% "twitter-server-slf4j-jdk14" % Versions.twitterServer

    val weaverTest = "com.disneystreaming" %% "weaver-cats" % Versions.weaver    % Test
    val scalaTest  = "org.scalatest"       %% "scalatest"   % Versions.scalatest % Test
  }

  object CompilerPlugin {
    val betterMonadicFor = compilerPlugin(
      "com.olegpy" %% "better-monadic-for" % Versions.betterMonadicFor
    )
  }

}
