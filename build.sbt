import sbt.Keys._
import sbt._
import tnm.LibPlugin

import java.io.File

lazy val commonSettings = Seq(
    name := "ocpi-endpoints",
    organization := "com.thenewmotion.ocpi",
    version := "0.5-SNAPSHOT",
    cancelable in Global := true,
    parallelExecution in Test := true
  )

  lazy val root = (project in file("."))
    .settings(commonSettings :+ (description := "OCPI endpoints"):_*)
    .settings(
      libraryDependencies  ++= Seq(

        "ch.qos.logback"               % "logback-classic"          %   "1.1.3" % "test",
        "com.typesafe.scala-logging"  %% "scala-logging"            %   "3.1.0" withSources(),
        "org.slf4j"                    % "log4j-over-slf4j"         %   "1.7.7" % "test",
        "org.slf4j"                    % "slf4j-api"                %   "1.7.7" % "test",

        "io.spray"                    %% "spray-json"               %   "1.3.2",
        "io.spray"                    %% "spray-routing-shapeless2" %   "1.3.3",
        "io.spray"                    %% "spray-testkit"            %   "1.3.3" % "test",

        "com.typesafe.akka"           %% "akka-actor"               %   "2.3.12",
        "org.scalaz"                  %% "scalaz-core"              %   "7.1.3",
        "com.thenewmotion"            %% "joda-money-ext"           %   "1.0.0",
        "com.thenewmotion"            %% "time"                     %   "2.8",
        "com.thenewmotion"            %% "mobilityid"               %   "0.8",
        "com.thenewmotion.ocpi"       %% "ocpi-msgs"                %   "0.3-SNAPSHOT" changing(),

        "org.specs2"                  %% "specs2-core"              %   "2.4.17" % "test",
        "org.specs2"                  %% "specs2-junit"             %   "2.4.17" % "test",
        "org.specs2"                  %% "specs2-mock"              %   "2.4.17" % "test"
      ))
    .enablePlugins(LibPlugin)



