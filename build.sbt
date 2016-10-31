val logging = Seq(
  "ch.qos.logback"               % "logback-classic"          %   "1.1.3" % "test",
  "org.slf4j"                    % "slf4j-api"                %   "1.7.15")

val `spray-json` = Seq("io.spray" %% "spray-json"             %   "1.3.2")

val spray = Seq(
  "io.spray"                    %% "spray-routing-shapeless2" %   "1.3.3",
  "io.spray"                    %% "spray-testkit"            %   "1.3.3" % "test",
  "io.spray"                    %% "spray-client"             %   "1.3.3")

def akka(scalaVersion: String) = {
  val version = scalaVersion match {
    case tnm.ScalaVersion.curr => "2.4.1"
    case tnm.ScalaVersion.prev => "2.3.14"
  }

  Seq("com.typesafe.akka" %% s"akka-actor" % version)
}

val scalaz = Seq("org.scalaz"        %% "scalaz-core"         %   "7.1.6")

val misc = Seq(
  "com.thenewmotion"            %% "joda-money-ext"           %   "1.0.0",
  "com.thenewmotion"            %% "time"                     %   "2.8",
  "com.thenewmotion"            %% "mobilityid"               %   "0.13")

val shapeless = Seq("com.chuusai" %% "shapeless"  %  "2.1.0")

val testing = Seq(
  "org.specs2"                  %% "specs2-core"              %   "2.4.17" % "test",
  "org.specs2"                  %% "specs2-junit"             %   "2.4.17" % "test",
  "org.specs2"                  %% "specs2-mock"              %   "2.4.17" % "test",
  "net.virtual-void"            %% "json-lenses"              %   "0.6.1"  % "test")

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
    description := "OCPI serialization library",
    libraryDependencies := misc ++ testing)

val `ocpi-msgs-spray-json` = project
  .enablePlugins(OssLibPlugin)
  .dependsOn(`ocpi-msgs`)
  .settings(
    commonSettings,
    description := "OCPI serialization library Spray Json",
    libraryDependencies :=`spray-json` ++ shapeless ++ misc ++ testing)

val `ocpi-endpoints-common` = project
  .enablePlugins(OssLibPlugin)
  .dependsOn(`ocpi-msgs-spray-json`)
  .settings(
    commonSettings,
    description := "OCPI endpoints common",
    libraryDependencies := logging ++ spray ++ akka(scalaVersion.value) ++ scalaz ++ misc ++ testing)

val `ocpi-endpoints-toplevel` = project
  .enablePlugins(OssLibPlugin)
  .dependsOn(`ocpi-endpoints-common`)
  .settings(
    commonSettings,
    description := "OCPI endpoints toplevel",
    libraryDependencies := logging ++ spray ++ akka(scalaVersion.value) ++ scalaz ++ misc ++ testing)

val `ocpi-endpoints-root` = (project in file("."))
  .aggregate(
    `ocpi-prelude`,
    `ocpi-msgs`,
    `ocpi-endpoints-common`,
    `ocpi-endpoints-toplevel`)
  .enablePlugins(OssLibPlugin)
  .settings(
    commonSettings,
    publish := {}
  )

