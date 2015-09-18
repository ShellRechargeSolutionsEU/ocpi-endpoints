import sbt.Keys._
import sbt._



val logging = Seq(
  "ch.qos.logback"               % "logback-classic"          %   "1.1.3" % "test",
  "com.typesafe.scala-logging"  %% "scala-logging"            %   "3.1.0" withSources(),
  "org.slf4j"                    % "log4j-over-slf4j"         %   "1.7.7" % "test",
  "org.slf4j"                    % "slf4j-api"                %   "1.7.7" % "test")

val `spray-json` = Seq("io.spray" %% "spray-json"             %   "1.3.2")

val spray = Seq(

  "io.spray"                    %% "spray-routing-shapeless2" %   "1.3.3",
  "io.spray"                    %% "spray-testkit"            %   "1.3.3" % "test",
  "io.spray"                    %% "spray-client"             %   "1.3.3")

val akka = Seq("com.typesafe.akka"   %% "akka-actor"          %   "2.3.12")

val scalaz = Seq("org.scalaz"        %% "scalaz-core"         %   "7.1.3")

val misc = Seq(
  "com.thenewmotion"            %% "joda-money-ext"           %   "1.0.0",
  "com.thenewmotion"            %% "time"                     %   "2.8",
  "com.thenewmotion"            %% "mobilityid"               %   "0.9")

val testing = Seq(
  "org.specs2"                  %% "specs2-core"              %   "2.4.17" % "test",
  "org.specs2"                  %% "specs2-junit"             %   "2.4.17" % "test",
  "org.specs2"                  %% "specs2-mock"              %   "2.4.17" % "test")

val commonSettings = Seq(
  organization := "com.thenewmotion.ocpi"
)

val protocol = project
  .enablePlugins(LibPlugin)
  .settings(
    commonSettings,
    description := "OCPI serialization library",
    name := "ocpi-msgs",
    libraryDependencies :=`spray-json` ++ misc ++ testing )

val endpoints = project
  .enablePlugins(LibPlugin)
  .dependsOn(protocol)
  .settings(
    commonSettings,
    description := "OCPI endpoints",
    name := "ocpi-endpoints",
    libraryDependencies := logging ++ spray ++ akka ++ scalaz ++ misc ++ testing
  )

enablePlugins(LibPlugin)



