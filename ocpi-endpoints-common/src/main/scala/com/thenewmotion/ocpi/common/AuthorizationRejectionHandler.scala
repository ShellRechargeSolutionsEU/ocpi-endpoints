package com.thenewmotion.ocpi.common

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.directives.BasicDirectives
import akka.http.scaladsl.server.directives.MiscDirectives
import com.thenewmotion.ocpi.msgs

object AuthorizationRejectionHandler extends BasicDirectives
  with MiscDirectives with SprayJsonSupport {

  import akka.http.scaladsl.model.StatusCodes._
  import akka.http.scaladsl.server.AuthorizationFailedRejection
  import akka.http.scaladsl.server.Directives._
  import akka.http.scaladsl.server.RejectionHandler
  import msgs.v2_1.CommonTypes.ErrorResp
  import msgs.v2_1.OcpiJsonProtocol._
  import msgs.v2_1.OcpiStatusCode.GenericClientFailure

  val Default = RejectionHandler.newBuilder().handle {
    case AuthorizationFailedRejection => extractUri { uri =>
      complete {
        Forbidden -> ErrorResp(
          GenericClientFailure,
          Some(s"The client is not authorized to access ${uri.toRelative}")
        )
      }
    }
  }.result()
}
