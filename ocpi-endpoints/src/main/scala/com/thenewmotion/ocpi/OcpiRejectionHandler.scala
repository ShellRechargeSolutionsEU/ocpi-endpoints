package com.thenewmotion.ocpi


import com.thenewmotion.ocpi.msgs.v2_0.CommonTypes.ErrorResp
import com.thenewmotion.ocpi.msgs.v2_0.OcpiStatusCodes._
import org.joda.time.DateTime
import spray.http.StatusCodes._
import spray.http.{ContentTypes, HttpEntity, HttpResponse}
import spray.httpx.SprayJsonSupport
import spray.json._
import spray.routing.{MissingHeaderRejection, AuthenticationFailedRejection, RejectionHandler}
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

    case rejections => complete {
      HttpResponse(
        InternalServerError,
        HttpEntity(ContentTypes.`application/json`,
          ErrorResp(
            GenericServerFailure.code,
            Some(GenericServerFailure.default_message),
            DateTime.now()).toJson.compactPrint)
      )
    }

  }
}
