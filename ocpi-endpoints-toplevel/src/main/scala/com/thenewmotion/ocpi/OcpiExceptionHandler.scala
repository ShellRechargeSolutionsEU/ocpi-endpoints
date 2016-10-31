package com.thenewmotion.ocpi

import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.ErrorResp
import com.thenewmotion.ocpi.msgs.v2_1.OcpiStatusCodes._
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport
import spray.routing._
import spray.routing.directives.BasicDirectives
import spray.routing.directives.RouteDirectives._

object OcpiExceptionHandler extends BasicDirectives with SprayJsonSupport {

  import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._

  val Default = ExceptionHandler {

    case exception => complete {
        ( InternalServerError,
            ErrorResp(
              GenericClientFailure.code,
              exception.toString))
      }
  }
}
