package com.thenewmotion.ocpi

import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.ErrorResp
import com.thenewmotion.ocpi.msgs.v2_1.OcpiStatusCode
import OcpiStatusCode._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.directives.BasicDirectives
import akka.http.scaladsl.model._
import akka.http.scaladsl.server._
import StatusCodes._
import Directives._

object OcpiRejectionHandler extends BasicDirectives with SprayJsonSupport {

  import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._

  val Default = RejectionHandler.newBuilder().handle {

    case MalformedRequestContentRejection(msg, cause) => complete {
      ( BadRequest,
        ErrorResp(
          GenericClientFailure,
          Some(msg)))
    }

    case UnsupportedVersionRejection(version: String) => complete {
      ( OK,
        ErrorResp(
          UnsupportedVersion,
          Some(s"Unsupported version: $version")))
    }

    case AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsMissing, challengeHeaders) =>
      complete {
        ( BadRequest,
          ErrorResp(
            MissingHeader,
            Some("Header not found")))
      }

    case AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsRejected, challengeHeaders) =>
      complete {
        ( BadRequest,
          ErrorResp(
            AuthenticationFailed,
            Some("Invalid authentication token")))
      }

    case MissingHeaderRejection(header) => complete {
      ( BadRequest,
        ErrorResp(
          MissingHeader,
          Some(s"Header not found: '$header'")))
    }
  }.handleAll[Rejection] { rejections =>
    complete {
      (BadRequest,
        ErrorResp(
          GenericClientFailure,
          Some(rejections.mkString(", "))))
    }
  }.result()
}
