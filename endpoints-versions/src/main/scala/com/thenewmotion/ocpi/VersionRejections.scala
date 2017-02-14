package com.thenewmotion.ocpi

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.{Rejection, RejectionHandler}
import com.thenewmotion.ocpi.common.OcpiRejectionHandler
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.ErrorResp
import com.thenewmotion.ocpi.msgs.v2_1.OcpiStatusCode.UnsupportedVersion
import SprayJsonSupport._

object VersionRejections {
  import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._

  case class UnsupportedVersionRejection(version: String) extends Rejection
  case class NoVersionsRejection() extends Rejection

  val Handler = RejectionHandler.newBuilder().handle {
    case UnsupportedVersionRejection(version: String) => complete {
      ( OK,
        ErrorResp(
          UnsupportedVersion,
          Some(s"Unsupported version: $version")))
    }
  }.result().withFallback(OcpiRejectionHandler.Default)
}
