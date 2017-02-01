import sbt.Keys.libraryDependencies

val logging = Seq(
  "ch.qos.logback"               % "logback-classic"          %   "1.1.8",
  "org.slf4j"                    % "slf4j-api"                %   "1.7.22")

val `spray-json` = Seq("io.spray" %% "spray-json"             %   "1.3.3")

def akkaModule(name: String) = {
  val v = if (name.startsWith("http")) "10.0.3" else "2.4.16"
  "com.typesafe.akka" %% s"akka-$name" % v
}

val akka =
  Seq(
    akkaModule("actor"),
    akkaModule("http"),
    akkaModule("http-spray-json")
  )

val scalaz = Seq("org.scalaz"        %% "scalaz-core"         %   "7.2.8")

val misc = Seq(
  "com.github.nscala-time" %% "nscala-time"              %   "2.16.0",
  "com.thenewmotion"       %% "mobilityid"               %   "0.16")

val specs2 = {
  def module(name: String) = "org.specs2" %% s"specs2-$name" % "3.8.7" % "test"
  Seq(
    module("core"), module("junit"), module("mock")
  )
}

val akkaHttpTestKit = Seq(akkaModule("http-testkit") % "test")
val akkaStreamTestKit = Seq(akkaModule("stream-testkit") % "test")

val jsonLenses = Seq("net.virtual-void" %% "json-lenses" %  "0.6.2")

val commonSettings = Seq(
  organization := "com.thenewmotion.ocpi",
  licenses += ("Apache License, Version 2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))
)

val `ocpi-prelude` = project
  .enablePlugins(OssLibPlugin)
  .settings(
    commonSettings,
    description := "Definitions that are useful across all OCPI modules")

val `ocpi-msgs` = project
  .enablePlugins(OssLibPlugin)
  .dependsOn(`ocpi-prelude`)
  .settings(
    commonSettings,
    description := "OCPI messages",
    libraryDependencies := misc)

val `ocpi-msgs-spray-json` = project
  .enablePlugins(OssLibPlugin)
  .dependsOn(`ocpi-msgs`)
  .settings(
    commonSettings,
    description := "OCPI serialization library Spray Json",
    libraryDependencies := `spray-json` ++ specs2
  )

val `ocpi-endpoints-common` = project
  .enablePlugins(OssLibPlugin)
  .dependsOn(`ocpi-msgs-spray-json`)
  .settings(
    commonSettings,
    description := "OCPI endpoints common",
    libraryDependencies := logging ++ akka ++ scalaz ++ specs2 ++ akkaHttpTestKit ++ akkaStreamTestKit
  )

val `ocpi-endpoints-msp-locations` = project
  .enablePlugins(OssLibPlugin)
  .dependsOn(`ocpi-endpoints-common`)
  .settings(
    commonSettings,
    description := "OCPI endpoints MSP Locations",
    libraryDependencies := specs2 ++ akkaHttpTestKit
  )

val `ocpi-endpoints-msp-tokens` = project
  .enablePlugins(OssLibPlugin)
  .dependsOn(`ocpi-endpoints-common`)
  .settings(
    commonSettings,
    description := "OCPI endpoints MSP Tokens",
    libraryDependencies := specs2 ++ akkaHttpTestKit
  )

val `ocpi-endpoints-cpo-locations` = project
  .enablePlugins(OssLibPlugin)
  .dependsOn(`ocpi-endpoints-common`)
  .settings(
    commonSettings,
    description := "OCPI endpoints CPO Locations",
    libraryDependencies := specs2 ++ akkaHttpTestKit
  )

val `ocpi-endpoints-cpo-tokens` = project
  .enablePlugins(OssLibPlugin)
  .dependsOn(`ocpi-endpoints-common`)
  .settings(
    commonSettings,
    description := "OCPI endpoints CPO Tokens",
    libraryDependencies := specs2 ++ akkaHttpTestKit
  )

val `ocpi-endpoints-toplevel` = project
  .enablePlugins(OssLibPlugin)
  .dependsOn(`ocpi-endpoints-common`)
  .settings(
    commonSettings,
    description := "OCPI endpoints toplevel",
    libraryDependencies := specs2 ++ akkaHttpTestKit ++ jsonLenses.map(_ % "test")
  )

val `ocpi-endpoints-root` = (project in file("."))
  .aggregate(
    `ocpi-prelude`,
    `ocpi-msgs`,
    `ocpi-msgs-spray-json`,
    `ocpi-endpoints-common`,
    `ocpi-endpoints-toplevel`,
    `ocpi-endpoints-msp-locations`,
    `ocpi-endpoints-msp-tokens`,
    `ocpi-endpoints-cpo-locations`,
    `ocpi-endpoints-cpo-tokens`)
  .enablePlugins(OssLibPlugin)
  .settings(
    commonSettings,
    publish := {}
  )

val `example` = project
  .enablePlugins(AppPlugin)
  .dependsOn(`ocpi-endpoints-toplevel`)
  .settings(
    commonSettings,
    description := "OCPI endpoints example app"
  )
