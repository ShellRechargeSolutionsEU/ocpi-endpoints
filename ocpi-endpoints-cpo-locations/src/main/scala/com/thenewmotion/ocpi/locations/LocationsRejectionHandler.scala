package com.thenewmotion.ocpi.locations

import com.thenewmotion.ocpi.locations.LocationsError._
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.ErrorResp
import com.thenewmotion.ocpi.msgs.v2_1.OcpiStatusCodes.GenericClientFailure
import org.joda.time.DateTime
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport
import spray.routing._
import spray.routing.directives.RouteDirectives._
import spray.routing.directives.{BasicDirectives, MiscDirectives}

object LocationsRejectionHandler extends BasicDirectives with MiscDirectives with SprayJsonSupport {

  import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._

  val DefaultErrorMsg = "An error occurred."
  val Default = RejectionHandler {

    case AuthorizationFailedRejection :: _ =>

      requestUri { uri => complete {
        ( Forbidden,
          ErrorResp(
            GenericClientFailure.code,
            s"The client is not authorized to access ${uri.toRelative}",
            DateTime.now()))
        }
      }

    case (LocationsErrorRejection(e@LocationNotFound(reason))) :: _ => complete {
      ( NotFound,
        ErrorResp(
          GenericClientFailure.code,
          reason getOrElse DefaultErrorMsg,
          DateTime.now()))
    }


    case (LocationsErrorRejection(e@EvseNotFound(reason))) :: _ => complete {
        ( NotFound,
            ErrorResp(
              GenericClientFailure.code,
              reason getOrElse DefaultErrorMsg,
              DateTime.now()))
      }


    case (LocationsErrorRejection(e@ConnectorNotFound(reason))) :: _ => complete {
        ( NotFound,
            ErrorResp(
              GenericClientFailure.code,
              reason getOrElse DefaultErrorMsg,
              DateTime.now()))
      }

  }
}
