import sbt.Keys.libraryDependencies

val logging = Seq(
  "ch.qos.logback"               % "logback-classic"          %   "1.2.3",
  "org.slf4j"                    % "slf4j-api"                %   "1.7.25")

val `spray-json` = Seq("io.spray" %% "spray-json"             %   "1.3.3")



val `circe` = {
  val version = "0.8.0"

  Seq(
    "io.circe" %% "circe-core" % version,
    "io.circe" %% "circe-generic-extras" % version,
    "io.circe" %% "circe-parser" % version % "test"
  )
}

def akkaModule(name: String) = {
  val v = if (name.startsWith("http")) "10.0.10" else "2.5.6"
  "com.typesafe.akka" %% s"akka-$name" % v
}

val akka =
  Seq(
    akkaModule("actor"),
    akkaModule("stream"),
    akkaModule("http"),
    akkaModule("http-spray-json")
  )

val cats = Seq("org.typelevel" %% "cats-core" % "0.9.0")

val specs2 = {
  def module(name: String) = "org.specs2" %% s"specs2-$name" % "4.0.1" % "test"
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

val `prelude` = project
  .enablePlugins(OssLibPlugin)
  .settings(
    commonSettings,
    name := "ocpi-prelude",
    description := "Definitions that are useful across all OCPI modules")

val `msgs` = project
  .enablePlugins(OssLibPlugin)
  .dependsOn(`prelude`)
  .settings(
    commonSettings,
    name := "ocpi-msgs",
    description := "OCPI messages",
    libraryDependencies := specs2)

val `msgs-json-test` = project
  .enablePlugins(OssLibPlugin)
  .dependsOn(`msgs`)
  .settings(
    commonSettings,
    name := "ocpi-msgs-json-test",
    description := "OCPI serialization tests",
    libraryDependencies := specs2
  )

val `msgs-spray-json` = project
  .enablePlugins(OssLibPlugin)
  .dependsOn(`msgs`, `msgs-json-test` % "test->test")
  .settings(
    commonSettings,
    name := "ocpi-msgs-spray-json",
    description := "OCPI serialization library Spray Json",
    libraryDependencies := `spray-json` ++ specs2
  )

val `msgs-circe` = project
  .enablePlugins(OssLibPlugin)
  .dependsOn(`msgs`, `msgs-json-test` % "test->test")
  .settings(
    commonSettings,
    name := "ocpi-msgs-circe",
    description := "OCPI serialization library Circe",
    libraryDependencies := `circe` ++ specs2
  )

val `endpoints-common` = project
  .enablePlugins(OssLibPlugin)
  .dependsOn(`msgs`, `msgs-spray-json` % "test->test")
  .settings(
    commonSettings,
    name := "ocpi-endpoints-common",
    description := "OCPI endpoints common",
    libraryDependencies := logging ++ akka ++ cats ++ specs2 ++ akkaHttpTestKit ++ akkaStreamTestKit
  )

val `endpoints-msp-locations` = project
  .enablePlugins(OssLibPlugin)
  .dependsOn(`endpoints-common`, `msgs-spray-json` % "test->test")
  .settings(
    commonSettings,
    name := "ocpi-endpoints-msp-locations",
    description := "OCPI endpoints MSP Locations",
    libraryDependencies := specs2 ++ akkaHttpTestKit
  )

val `endpoints-msp-tokens` = project
  .enablePlugins(OssLibPlugin)
  .dependsOn(`endpoints-common`, `msgs-spray-json` % "test->test")
  .settings(
    commonSettings,
    name := "ocpi-endpoints-msp-tokens",
    description := "OCPI endpoints MSP Tokens",
    libraryDependencies := specs2 ++ akkaHttpTestKit
  )

val `endpoints-msp-cdrs` = project
  .enablePlugins(OssLibPlugin)
  .dependsOn(`endpoints-common`, `msgs-spray-json` % "test->test")
  .settings(
    commonSettings,
    name := "ocpi-endpoints-msp-cdrs",
    description := "OCPI endpoints MSP Cdrs",
    libraryDependencies := specs2 ++ akkaHttpTestKit
  )

val `endpoints-msp-commands` = project
  .enablePlugins(OssLibPlugin)
  .dependsOn(`endpoints-common`, `msgs-spray-json` % "test->test")
  .settings(
    commonSettings,
    name := "ocpi-endpoints-msp-commands",
    description := "OCPI endpoints MSP Commands",
    libraryDependencies := specs2 ++ akkaHttpTestKit
  )

val `endpoints-cpo-locations` = project
  .enablePlugins(OssLibPlugin)
  .dependsOn(`endpoints-common`, `msgs-spray-json` % "test->test")
  .settings(
    commonSettings,
    name := "ocpi-endpoints-cpo-locations",
    description := "OCPI endpoints CPO Locations",
    libraryDependencies := specs2 ++ akkaHttpTestKit
  )

val `endpoints-cpo-tokens` = project
  .enablePlugins(OssLibPlugin)
  .dependsOn(`endpoints-common`, `msgs-spray-json` % "test->test")
  .settings(
    commonSettings,
    name := "ocpi-endpoints-cpo-tokens",
    description := "OCPI endpoints CPO Tokens",
    libraryDependencies := specs2 ++ akkaHttpTestKit
  )

val `endpoints-versions` = project
  .enablePlugins(OssLibPlugin)
  .dependsOn(`endpoints-common`, `msgs-spray-json` % "test->test")
  .settings(
    commonSettings,
    name := "ocpi-endpoints-versions",
    description := "OCPI endpoints versions",
    libraryDependencies := specs2 ++ akkaHttpTestKit ++ jsonLenses.map(_ % "test")
  )

val `endpoints-registration` = project
  .enablePlugins(OssLibPlugin)
  .dependsOn(`endpoints-common`, `msgs-spray-json` % "test->test")
  .settings(
    commonSettings,
    name := "ocpi-endpoints-registration",
    description := "OCPI endpoints registration",
    libraryDependencies := specs2 ++ akkaHttpTestKit ++ jsonLenses.map(_ % "test")
  )

val `example` = project
  .enablePlugins(AppPlugin)
  .dependsOn(`endpoints-registration`, `endpoints-versions`, `msgs-spray-json`)
  .settings(
    commonSettings,
    publish := { },
    description := "OCPI endpoints example app"
  )

val `ocpi-endpoints-root` = (project in file("."))
  .aggregate(
    `prelude`,
    `msgs`,
    `msgs-spray-json`,
    `msgs-circe`,
    `endpoints-common`,
    `endpoints-versions`,
    `endpoints-registration`,
    `endpoints-msp-locations`,
    `endpoints-msp-tokens`,
    `endpoints-msp-cdrs`,
    `endpoints-msp-commands`,
    `endpoints-cpo-locations`,
    `endpoints-cpo-tokens`,
    `example`)
  .enablePlugins(OssLibPlugin)
  .settings(
    commonSettings,
    publish := {}
  )
