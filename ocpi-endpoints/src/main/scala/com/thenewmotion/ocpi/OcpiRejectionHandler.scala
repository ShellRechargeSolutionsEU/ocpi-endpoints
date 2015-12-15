package com.thenewmotion.ocpi


import com.thenewmotion.ocpi.msgs.v2_0.CommonTypes.ErrorResp
import com.thenewmotion.ocpi.msgs.v2_0.OcpiStatusCodes._
import org.joda.time.DateTime
import spray.http.StatusCodes._
import spray.http.{ContentTypes, HttpEntity, HttpResponse}
import spray.httpx.SprayJsonSupport
import spray.json._
import spray.routing._
import spray.routing.directives.BasicDirectives
import spray.routing.directives.RouteDirectives._

object OcpiRejectionHandler extends BasicDirectives with SprayJsonSupport {

  import com.thenewmotion.ocpi.msgs.v2_0.OcpiJsonProtocol._

  val Default = RejectionHandler {

    case (r@UnsupportedVersionRejection(version: String)) :: _ =>
      complete {
        HttpResponse(
          BadRequest,
          HttpEntity(ContentTypes.`application/json`,
            ErrorResp(
              UnsupportedVersion.code,
              Some(s"Version not known: $version"),
              DateTime.now()).toJson.compactPrint)
        )
      }

    case (r@NoVersionsRejection()) :: _ =>
      complete {
        HttpResponse(
          InternalServerError,
          HttpEntity(ContentTypes.`application/json`,
            ErrorResp(
              3010,
              Some(s"No versions registered"),
              DateTime.now()).toJson.compactPrint)
        )
      }

    case (r@AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsMissing, challengeHeaders)) :: _ =>
      complete {
        HttpResponse(
          BadRequest,
          HttpEntity(ContentTypes.`application/json`,
            ErrorResp(
              AuthenticationFailed.code,
              Some(AuthenticationFailed.default_message),
              DateTime.now()).toJson.compactPrint)
        )
      }

    case (r@AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsRejected, challengeHeaders)) :: _ =>
      complete {
        HttpResponse(
          BadRequest,
          HttpEntity(ContentTypes.`application/json`,
            ErrorResp(
              AuthenticationFailed.code,
              Some(AuthenticationFailed.default_message),
              DateTime.now()).toJson.compactPrint)
        )
      }

    case (r@MissingHeaderRejection(header)) :: _ =>
      complete {
        HttpResponse(
          BadRequest,
          HttpEntity(ContentTypes.`application/json`,
            ErrorResp(
              MissingHeader.code,
              Some(s"Header not found: '$header'"),
              DateTime.now()).toJson.compactPrint)
        )
      }

    //TODO: TNM-2013: It doesn't work yet, it must be used to fail with that error when required endpoints not included
//    case (r@ValidationRejection(msg, cause)) :: _ =>
//      complete {
//        HttpResponse(
//          BadRequest,
//          HttpEntity(ContentTypes.`application/json`,
//            ErrorResp(
//              MissingExpectedEndpoints.code,
//              Some(s"${MissingExpectedEndpoints.default_message} $msg"),
//              DateTime.now()).toJson.compactPrint)
//        )
//      }

    // FIXME: TNM-1987 We generate Server Error even when the resource doesn't exist
//    case rejections => complete {
//      HttpResponse(
//        InternalServerError,
//        HttpEntity(ContentTypes.`application/json`,
//          ErrorResp(
//            GenericServerFailure.code,
//            Some(GenericServerFailure.default_message),
//            DateTime.now()).toJson.compactPrint)
//      )
//    }

  }
}
