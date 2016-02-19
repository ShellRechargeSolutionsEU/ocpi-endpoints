package com.thenewmotion.ocpi


import com.thenewmotion.ocpi.msgs.v2_0.CommonTypes.ErrorResp
import com.thenewmotion.ocpi.msgs.v2_0.OcpiStatusCodes._
import org.joda.time.DateTime
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport
import spray.json._
import spray.routing._
import spray.routing.directives.BasicDirectives
import spray.routing.directives.RouteDirectives._

object OcpiRejectionHandler extends BasicDirectives with SprayJsonSupport {

  import com.thenewmotion.ocpi.msgs.v2_0.OcpiJsonProtocol._

  val Default = RejectionHandler {

    case (MalformedRequestContentRejection(msg, cause)) :: _ => complete {
      ( BadRequest,
        ErrorResp(
          GenericClientFailure.code,
          Some(msg),
          DateTime.now()).toJson.compactPrint)
    }

    case (r@UnsupportedVersionRejection(version: String)) :: _ => complete {
      ( BadRequest,
        ErrorResp(
          UnsupportedVersion.code,
          Some(s"${UnsupportedVersion.default_message}: $version")).toJson.compactPrint)
    }

    case (r@NoVersionsRejection()) :: _ => complete {
      ( InternalServerError,
        ErrorResp(
          NotVersionsRegistered.code,
          Some(NotVersionsRegistered.default_message)).toJson.compactPrint)
    }

    case (r@AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsMissing, challengeHeaders)) :: _ =>
      complete {
        ( BadRequest,
          ErrorResp(
            MissingHeader.code,
            Some(MissingHeader.default_message),
            DateTime.now()).toJson.compactPrint)
      }

    case (r@AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsRejected, challengeHeaders)) :: _ =>
      complete {
        ( BadRequest,
          ErrorResp(
            AuthenticationFailed.code,
            Some(s"${AuthenticationFailed.default_message}: $challengeHeaders"),
            DateTime.now()).toJson.compactPrint)
      }

    case (r@MissingHeaderRejection(header)) :: _ => complete {
      ( BadRequest,
        ErrorResp(
          MissingHeader.code,
          Some(s"${MissingHeader.default_message}: '$header'"),
          DateTime.now()).toJson.compactPrint)

    }

    case rejections => complete {
      (BadRequest,
        ErrorResp(
          GenericClientFailure.code,
          Option(rejections.mkString(", ")).filter(_.trim.nonEmpty),
          DateTime.now()).toJson.compactPrint)
    }

  }
}
