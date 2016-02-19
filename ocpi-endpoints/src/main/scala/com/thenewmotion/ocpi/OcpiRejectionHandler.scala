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
            Some(AuthenticationFailed.default_message),
            DateTime.now()).toJson.compactPrint)
      }

    case (r@MissingHeaderRejection(header)) :: _ => complete {
      ( BadRequest,
        ErrorResp(
          MissingHeader.code,
          Some(s"Header not found: '$header'"),
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
