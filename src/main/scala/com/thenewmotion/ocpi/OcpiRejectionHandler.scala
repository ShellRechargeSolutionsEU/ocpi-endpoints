package com.thenewmotion.ocpi


import com.thenewmotion.ocpi.msgs.v2_0.CommonTypes.GenericResp
import com.thenewmotion.ocpi.msgs.v2_0.OcpiStatusCodes.UnsupportedVersionError
import com.thenewmotion.spray.ApiRejectionHandler
import org.joda.time.DateTime
import spray.http.StatusCodes._
import spray.http.{ContentTypes, HttpEntity, HttpResponse}
import spray.httpx.SprayJsonSupport
import spray.json._
import spray.routing.RejectionHandler
import spray.routing.directives.BasicDirectives
import spray.routing.directives.RouteDirectives._

object OcpiRejectionHandler extends BasicDirectives with SprayJsonSupport {

  import com.thenewmotion.ocpi.msgs.v2_0.OcpiJsonProtocol._

  val Default = RejectionHandler {

    case (r@UnknownVersionRejection(version: String)) :: _ ⇒
      complete {
        HttpResponse(
          BadRequest,
          HttpEntity(ContentTypes.`application/json`,
            GenericResp(
              UnsupportedVersionError.code,
              s"Version not known $version",
              DateTime.now(),
              List()).toJson.compactPrint)
        )
      }


    case rejections => ApiRejectionHandler.Default(rejections)
  }
}
