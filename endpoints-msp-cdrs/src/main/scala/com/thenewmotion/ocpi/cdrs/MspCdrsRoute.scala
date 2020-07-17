package com.thenewmotion.ocpi
package cdrs

import _root_.akka.http.scaladsl.marshalling.ToResponseMarshaller
import _root_.akka.http.scaladsl.model.StatusCode
import _root_.akka.http.scaladsl.model.StatusCodes._
import _root_.akka.http.scaladsl.server.Route
import _root_.akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import cats.Functor
import com.thenewmotion.ocpi.cdrs.CdrsError._
import com.thenewmotion.ocpi.common._
import com.thenewmotion.ocpi.msgs.OcpiStatusCode._
import com.thenewmotion.ocpi.msgs.{ErrorResp, _}
import com.thenewmotion.ocpi.msgs.v2_1.Cdrs.{Cdr, CdrId}

object MspCdrsRoute {
  def apply[F[_]: Functor: HktMarshallable](
    service: MspCdrsService[F]
  )(
    implicit errorM: ErrRespMar,
    successUnit: SuccessRespMar[Unit],
    successCdr: SuccessRespMar[Cdr],
    cdrU: FromEntityUnmarshaller[Cdr]
  ): MspCdrsRoute[F] = new MspCdrsRoute(service)
}

class MspCdrsRoute[F[_]: Functor: HktMarshallable] private[ocpi](
  service: MspCdrsService[F]
)(
  implicit errorM: ErrRespMar,
  successUnit: SuccessRespMar[Unit],
  successCdr: SuccessRespMar[Cdr],
  cdrU: FromEntityUnmarshaller[Cdr]
) extends EitherUnmarshalling
    with OcpiDirectives {

  implicit def cdrsErrorResp(
    implicit em: ToResponseMarshaller[(StatusCode, ErrorResp)]
  ): ToResponseMarshaller[CdrsError] = {
    em.compose[CdrsError] { cdrsError =>
      val statusCode = cdrsError match {
        case _: CdrNotFound       => NotFound
        case _: CdrCreationFailed => OK
        case _                    => InternalServerError
      }
      statusCode -> ErrorResp(GenericClientFailure, cdrsError.reason)
    }
  }

  def apply(
    apiUser: GlobalPartyId
  ): Route =
    handleRejections(OcpiRejectionHandler.Default)(routeWithoutRh(apiUser))

  private val CdrIdSegment = Segment.map(CdrId(_))

  import HktMarshallableSyntax._

  private[cdrs] def routeWithoutRh(
    apiUser: GlobalPartyId
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
