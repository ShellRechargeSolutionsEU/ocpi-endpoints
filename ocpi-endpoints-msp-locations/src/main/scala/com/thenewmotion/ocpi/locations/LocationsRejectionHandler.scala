package com.thenewmotion.ocpi.locations

import LocationsError._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.directives.{BasicDirectives, MiscDirectives}
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.ErrorResp
import com.thenewmotion.ocpi.msgs.v2_1.OcpiStatusCode.GenericClientFailure
import org.joda.time.DateTime
import akka.http.scaladsl.model._
import akka.http.scaladsl.server._
import StatusCodes._
import Directives._

object LocationsRejectionHandler extends BasicDirectives with MiscDirectives with SprayJsonSupport {

  import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._

  val Default = RejectionHandler.newBuilder().handle {

    case AuthorizationFailedRejection =>

      extractUri { uri => complete {
        ( Forbidden,
          ErrorResp(
            GenericClientFailure,
            Some(s"The client is not authorized to access ${uri.toRelative}"),
            DateTime.now()))
        }
      }

    case LocationsErrorRejection(LocationNotFound(reason)) => complete {
      ( NotFound,
        ErrorResp(
          GenericClientFailure,
          reason,
          DateTime.now()))
    }

    case LocationsErrorRejection(LocationCreationFailed(reason)) => complete {
        ( OK,
            ErrorResp(
              GenericClientFailure,
              reason,
              DateTime.now()))
      }

    case LocationsErrorRejection(EvseNotFound(reason)) => complete {
        ( NotFound,
            ErrorResp(
              GenericClientFailure,
              reason,
              DateTime.now()))
      }

    case LocationsErrorRejection(EvseCreationFailed(reason)) => complete {
      ( OK,
        ErrorResp(
          GenericClientFailure,
          reason,
          DateTime.now()))
    }

    case LocationsErrorRejection(ConnectorNotFound(reason)) => complete {
        ( NotFound,
            ErrorResp(
              GenericClientFailure,
              reason,
              DateTime.now()))
      }

    case LocationsErrorRejection(ConnectorCreationFailed(reason)) => complete {
      ( OK,
        ErrorResp(
          GenericClientFailure,
          reason,
          DateTime.now()))
    }
  }.result()
}
