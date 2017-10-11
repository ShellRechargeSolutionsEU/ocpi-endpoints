package com.thenewmotion.ocpi

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.{Rejection, RejectionHandler}
import common.OcpiRejectionHandler
import msgs.OcpiStatusCode.UnsupportedVersion
import SprayJsonSupport._
import msgs.ErrorResp
import msgs.Versions.VersionNumber

object VersionRejections {
  import com.thenewmotion.ocpi.msgs.v2_1.DefaultJsonProtocol._

  case class UnsupportedVersionRejection(version: VersionNumber) extends Rejection
  case class NoVersionsRejection() extends Rejection

  val Handler = RejectionHandler.newBuilder().handle {
    case UnsupportedVersionRejection(version: VersionNumber) => complete {
      ( OK,
        ErrorResp(
          UnsupportedVersion,
          Some(s"Unsupported version: $version")))
    }
  }.result().withFallback(OcpiRejectionHandler.Default)
}
