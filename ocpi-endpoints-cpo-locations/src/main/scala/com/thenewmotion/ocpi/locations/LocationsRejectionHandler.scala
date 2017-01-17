package com.thenewmotion.ocpi.locations

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.{AuthorizationFailedRejection, RejectionHandler}
import akka.http.scaladsl.server.directives.{BasicDirectives, MiscDirectives}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server._
import StatusCodes._
import Directives._
import com.thenewmotion.ocpi.locations.LocationsError._
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.ErrorResp
import com.thenewmotion.ocpi.msgs.v2_1.OcpiStatusCode.GenericClientFailure
import org.joda.time.DateTime

object LocationsRejectionHandler extends BasicDirectives with MiscDirectives with SprayJsonSupport {

  import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._

  val DefaultErrorMsg = "An error occurred."
  val Default = RejectionHandler.newBuilder().handle {
    case AuthorizationFailedRejection =>
      extractUri { uri =>
        complete {
          (Forbidden,
            ErrorResp(
              GenericClientFailure,
              Some(s"The client is not authorized to access ${uri.toRelative}"),
              DateTime.now()))
        }
      }

    case LocationsErrorRejection(e@LocationNotFound(reason)) => complete {
      ( NotFound,
        ErrorResp(
          GenericClientFailure,
          Some(reason getOrElse DefaultErrorMsg),
          DateTime.now()))
    }


    case LocationsErrorRejection(e@EvseNotFound(reason)) => complete {
        ( NotFound,
            ErrorResp(
              GenericClientFailure,
              Some(reason getOrElse DefaultErrorMsg),
              DateTime.now()))
      }


    case LocationsErrorRejection(e@ConnectorNotFound(reason)) => complete {
        ( NotFound,
            ErrorResp(
              GenericClientFailure,
              Some(reason getOrElse DefaultErrorMsg),
              DateTime.now()))
      }
  }.result()
}
