package com.thenewmotion.ocpi


import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.ErrorResp
import com.thenewmotion.ocpi.msgs.v2_1.OcpiStatusCodes._
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport
import spray.routing._
import spray.routing.directives.BasicDirectives
import spray.routing.directives.RouteDirectives._

object OcpiRejectionHandler extends BasicDirectives with SprayJsonSupport {

  import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._

  val Default = RejectionHandler {

    case (MalformedRequestContentRejection(msg, cause)) :: _ => complete {
      ( BadRequest,
        ErrorResp(
          GenericClientFailure.code,
          msg))
    }

    case (r@UnsupportedVersionRejection(version: String)) :: _ => complete {
      ( BadRequest,
        ErrorResp(
          UnsupportedVersion.code,
          s"${UnsupportedVersion.defaultMessage}: $version"))
    }

    case (r@AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsMissing, challengeHeaders)) :: _ =>
      complete {
        ( BadRequest,
          ErrorResp(
            MissingHeader.code,
            MissingHeader.defaultMessage))
      }

    case (r@AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsRejected, challengeHeaders)) :: _ =>
      complete {
        ( BadRequest,
          ErrorResp(
            AuthenticationFailed.code,
            s"${AuthenticationFailed.defaultMessage}"))
      }

    case (r@MissingHeaderRejection(header)) :: _ => complete {
      ( BadRequest,
        ErrorResp(
          MissingHeader.code,
          s"${MissingHeader.defaultMessage}: '$header'"))
    }

    case rejections => complete {
      (BadRequest,
        ErrorResp(
          GenericClientFailure.code,
          rejections.mkString(", ")))
    }

  }
}
