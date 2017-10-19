package com.thenewmotion.ocpi

import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.{Rejection, RejectionHandler}
import common.OcpiRejectionHandler
import msgs.OcpiStatusCode.UnsupportedVersion
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import msgs.ErrorResp
import msgs.Versions.VersionNumber

object VersionRejections {
  case class UnsupportedVersionRejection(version: VersionNumber) extends Rejection
  case class NoVersionsRejection() extends Rejection

  def Handler(
    implicit errorRespM: ToEntityMarshaller[ErrorResp]
  ): RejectionHandler = RejectionHandler.newBuilder().handle {
    case UnsupportedVersionRejection(version: VersionNumber) => complete {
      ( OK,
        ErrorResp(
          UnsupportedVersion,
          Some(s"Unsupported version: $version")))
    }
  }.result().withFallback(OcpiRejectionHandler.Default)
}
