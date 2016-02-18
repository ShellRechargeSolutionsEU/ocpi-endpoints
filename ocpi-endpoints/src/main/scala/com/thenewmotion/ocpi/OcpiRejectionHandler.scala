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
              Some(msg)).toJson.compactPrint)
      }

    case (r@UnsupportedVersionRejection(version: String)) :: _ => complete {
        ( BadRequest,
            ErrorResp(
              UnsupportedVersion.code,
              Some(s"Version not known: $version")).toJson.compactPrint)
      }

    case (r@NoVersionsRejection()) :: _ => complete {
        ( InternalServerError,
            ErrorResp(
              3010,
              Some(s"No versions registered")).toJson.compactPrint)
      }

    case (r@AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsMissing, challengeHeaders)) :: _ =>
      complete {
        ( BadRequest,
            ErrorResp(
              MissingHeader.code,
              Some(MissingHeader.default_message)).toJson.compactPrint)
      }

    case (r@AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsRejected, challengeHeaders)) :: _ =>
      complete {
        ( BadRequest,
            ErrorResp(
              AuthenticationFailed.code,
              Some(AuthenticationFailed.default_message)).toJson.compactPrint)
      }

    case (r@MissingHeaderRejection(header)) :: _ => complete {
        ( BadRequest,
            ErrorResp(
              MissingHeader.code,
              Some(s"Header not found: '$header'")).toJson.compactPrint)

      }

    //TODO: TNM-2013: It doesn't work yet, it must be used to fail with that error when required endpoints not included
//    case (r@ValidationRejection(msg, cause)) :: _ => complete {
//        ( BadRequest,
//            ErrorResp(
//              MissingExpectedEndpoints.code,
//              Some(s"${MissingExpectedEndpoints.default_message} $msg"),
//              DateTime.now()).toJson.compactPrint)
//      }


    case rejections => complete {
      (BadRequest,
        ErrorResp(
          GenericClientFailure.code,
          Option(rejections.mkString(", ")).filter(_.trim.nonEmpty)).toJson.compactPrint)
    }

  }
}
