val logging = Seq(
  "ch.qos.logback"               % "logback-classic"          %   "1.1.7",
  "org.slf4j"                    % "slf4j-api"                %   "1.7.21")

val `spray-json` = Seq("io.spray" %% "spray-json"             %   "1.3.2")

val spray = Seq(
  "io.spray"                    %% "spray-routing-shapeless2" %   "1.3.3",
  "io.spray"                    %% "spray-client"             %   "1.3.4")

val akka = Seq("com.typesafe.akka" %% s"akka-actor" % "2.4.12")

val akkaTestkit = Seq("com.typesafe.akka" %% s"akka-testkit" % "2.4.12")

val scalaz = Seq("org.scalaz"        %% "scalaz-core"         %   "7.2.7")

val misc = Seq(
  "com.thenewmotion"            %% "joda-money-ext"           %   "1.0.0",
  "com.thenewmotion"            %% "time"                     %   "2.8",
  "com.thenewmotion"            %% "mobilityid"               %   "0.13")

val specs2 = Seq(
  "org.specs2"                  %% "specs2-core"              %   "3.8.5.1",
  "org.specs2"                  %% "specs2-junit"             %   "3.8.5.1",
  "org.specs2"                  %% "specs2-mock"              %   "3.8.5.1")

val jsonLenses = Seq("net.virtual-void" %% "json-lenses" %   "0.6.1")

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
    libraryDependencies := `spray-json` ++ specs2.map(_ % "test"))

val `spray-testkit-specs2` = project
  .enablePlugins(LibPlugin)
  .settings(
    description := "Spray testkit that works with Specs2 3.x",
    libraryDependencies := spray ++ specs2 ++ akkaTestkit)

val `ocpi-endpoints-common` = project
  .enablePlugins(OssLibPlugin)
  .dependsOn(`ocpi-msgs-spray-json`, `spray-testkit-specs2` % "test->test")
  .settings(
    commonSettings,
    description := "OCPI endpoints common",
    libraryDependencies := logging ++ spray ++ akka ++ scalaz)

val `ocpi-endpoints-msp-locations` = project
  .enablePlugins(OssLibPlugin)
  .dependsOn(`ocpi-endpoints-common`, `spray-testkit-specs2` % "test->test")
  .settings(
    commonSettings,
    description := "OCPI endpoints MSP Locations")

val `ocpi-endpoints-toplevel` = project
  .enablePlugins(OssLibPlugin)
  .dependsOn(`ocpi-endpoints-common`, `spray-testkit-specs2` % "test->test")
  .settings(
    commonSettings,
    description := "OCPI endpoints toplevel",
    libraryDependencies := jsonLenses.map(_ % "test"))

val `ocpi-endpoints-root` = (project in file("."))
  .aggregate(
    `ocpi-prelude`,
    `ocpi-msgs`,
    `ocpi-msgs-spray-json`,
    `ocpi-endpoints-common`,
    `ocpi-endpoints-toplevel`,
    `ocpi-endpoints-msp-locations`)
  .enablePlugins(OssLibPlugin)
  .settings(
    commonSettings,
    publish := {}
  )

