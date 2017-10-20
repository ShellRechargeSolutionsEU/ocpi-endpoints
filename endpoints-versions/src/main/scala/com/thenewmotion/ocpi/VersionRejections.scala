package com.thenewmotion.ocpi

import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.{Rejection, RejectionHandler}
import common.{ErrRespMar, OcpiRejectionHandler}
import msgs.OcpiStatusCode.UnsupportedVersion
import msgs.ErrorResp
import msgs.Versions.VersionNumber

object VersionRejections {
  case class UnsupportedVersionRejection(version: VersionNumber) extends Rejection
  case class NoVersionsRejection() extends Rejection

  def Handler(
    implicit errorRespM: ErrRespMar
  ): RejectionHandler = RejectionHandler.newBuilder().handle {
    case UnsupportedVersionRejection(version: VersionNumber) => complete {
      ( OK,
        ErrorResp(
          UnsupportedVersion,
          Some(s"Unsupported version: $version")))
    }
  }.result().withFallback(OcpiRejectionHandler.Default)
}
