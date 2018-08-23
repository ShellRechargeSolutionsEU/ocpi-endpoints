package com.thenewmotion.ocpi
package common

import _root_.akka.http.scaladsl.model.StatusCodes._
import _root_.akka.http.scaladsl.server.Directives._
import _root_.akka.http.scaladsl.server._
import _root_.akka.http.scaladsl.server.directives.BasicDirectives
import msgs.ErrorResp
import msgs.OcpiStatusCode._

object OcpiRejectionHandler extends BasicDirectives {

  def Default(
    implicit m: ErrRespMar
  ): RejectionHandler =
    RejectionHandler.newBuilder().handle {
      case MalformedRequestContentRejection(msg, _) =>
        complete {
        (BadRequest, ErrorResp(GenericClientFailure, Some(msg)))
      }
    }.handle {
      case AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsMissing, _) =>
        complete {
          ( Unauthorized,
            ErrorResp(
              MissingHeader,
              Some("Authorization Token not supplied")))
        }
    }.handle {
      case AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsRejected, _) =>
        complete {
          ( Unauthorized,
            ErrorResp(
              AuthenticationFailed,
              Some("Invalid Authorization Token")))
        }
    }.handle {
      case AuthorizationFailedRejection => extractUri { uri =>
        complete {
          Forbidden -> ErrorResp(
            GenericClientFailure,
            Some(s"The client is not authorized to access ${uri.toRelative}")
          )
        }
      }
    }.handle {
      case MissingHeaderRejection(header) => complete {
        ( BadRequest,
          ErrorResp(
            MissingHeader,
            Some(s"Header not found: '$header'")))
      }
    }.handleAll[Rejection] { rejections =>
      complete {
        ( BadRequest,
          ErrorResp(
            GenericClientFailure,
            Some(rejections.mkString(", "))))
      }
    }.result()
}
