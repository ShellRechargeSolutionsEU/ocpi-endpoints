package com.thenewmotion.ocpi
package cdrs

import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import msgs.ErrorResp
import common._
import cdrs.CdrsError._
import com.thenewmotion.ocpi.msgs.v2_1.Cdrs.{Cdr, CdrId}
import msgs._
import msgs.OcpiStatusCode._

import scala.concurrent.ExecutionContext

class MspCdrsRoute(
  service: MspCdrsService
)(
  implicit errorM: ErrRespMar,
  successUnit: SuccessRespMar[Unit],
  successCdr: SuccessRespMar[Cdr],
  cdrU: FromEntityUnmarshaller[Cdr]
) extends JsonApi with EitherUnmarshalling with OcpiDirectives {

  implicit def cdrsErrorResp(
    implicit em: ToResponseMarshaller[(StatusCode, ErrorResp)]
  ): ToResponseMarshaller[CdrsError] = {
      em.compose[CdrsError] { cdrsError =>
      val statusCode = cdrsError match {
        case _: CdrNotFound => NotFound
        case _: CdrCreationFailed => OK
        case _ => InternalServerError
      }
      statusCode -> ErrorResp(GenericClientFailure, cdrsError.reason)
    }
  }

  def route(
    apiUser: GlobalPartyId
  )(
    implicit executionContext: ExecutionContext
  ): Route =
    handleRejections(OcpiRejectionHandler.Default)(routeWithoutRh(apiUser))

  private val CdrIdSegment = Segment.map(CdrId(_))

  private[cdrs] def routeWithoutRh(
    apiUser: GlobalPartyId
  )(
    implicit executionContext: ExecutionContext
  ) = {
    authPathPrefixGlobalPartyIdEquality(apiUser) {
      (post & pathEndOrSingleSlash) {
        entity(as[Cdr]) { cdr =>
          complete {
            service.createCdr(apiUser, cdr).mapRight { _ =>
              (Created, SuccessResp(GenericSuccess))
            }
          }
        }
      } ~
      (get & pathPrefix(CdrIdSegment) & pathEndOrSingleSlash) { cdrId =>
        complete {
          service.cdr(apiUser, cdrId).mapRight { cdr =>
            SuccessResp(GenericSuccess, data = cdr)
          }
        }
      }
    }
  }
}
