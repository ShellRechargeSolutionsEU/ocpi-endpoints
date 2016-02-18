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

object OcpiExceptionHandler extends BasicDirectives with SprayJsonSupport {

  import com.thenewmotion.ocpi.msgs.v2_0.OcpiJsonProtocol._

  val Default = ExceptionHandler {

    case exception => complete {
        ( InternalServerError,
            ErrorResp(
              GenericClientFailure.code,
              Some(exception.toString)).toJson.compactPrint)
      }
  }
}
