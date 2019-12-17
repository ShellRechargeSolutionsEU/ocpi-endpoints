package com.thenewmotion.ocpi
package cdrs

import java.time.ZonedDateTime

import _root_.akka.http.scaladsl.marshalling.ToResponseMarshaller
import _root_.akka.http.scaladsl.model.StatusCode
import _root_.akka.http.scaladsl.model.StatusCodes._
import _root_.akka.http.scaladsl.server.Route
import com.thenewmotion.ocpi.cdrs.CpoCdrsRoute.ToTransportLayerError
import com.thenewmotion.ocpi.common._
import com.thenewmotion.ocpi.msgs.OcpiStatusCode.{GenericClientFailure, GenericSuccess}
import com.thenewmotion.ocpi.msgs._
import com.thenewmotion.ocpi.msgs.v2_1.Cdrs.Cdr

object CpoCdrsRoute {
  type ToTransportLayerError = CdrError => StatusCode

  def apply(service: CpoCdrsService,
            DefaultLimit: Int = 1000,
            MaxLimit: Int = 1000,
            errorMapping: ToTransportLayerError = _ => InternalServerError)(
             implicit successIterableCdrM: SuccessRespMar[Iterable[Cdr]],
             errorM: ErrRespMar
           ): CpoCdrsRoute = new CpoCdrsRoute(service, DefaultLimit, MaxLimit, errorMapping)
}

class CpoCdrsRoute private[ocpi](service: CpoCdrsService,
                                 val DefaultLimit: Int,
                                 val MaxLimit: Int,
                                 toTransportLayerErr: ToTransportLayerError)(
                                  implicit successIterableCdrM: SuccessRespMar[Iterable[Cdr]],
                                  errorM: ErrRespMar
                                ) extends OcpiDirectives with PaginatedRoute with EitherUnmarshalling {

  implicit def cdrsErrorResp(
    implicit em: ToResponseMarshaller[(StatusCode, ErrorResp)]
  ): ToResponseMarshaller[CdrError] = {
    em.compose[CdrError] { cdrError =>
      toTransportLayerErr(cdrError) -> ErrorResp(GenericClientFailure, cdrError.reason)
    }
  }

  def apply(apiUser: GlobalPartyId): Route =
    handleRejections(OcpiRejectionHandler.Default)(routeWithoutRh(apiUser))

  def routeWithoutRh(apiUser: GlobalPartyId): Route =
    get {
      pathEndOrSingleSlash {
        paged { (pager: Pager, dateFrom: Option[ZonedDateTime], dateTo: Option[ZonedDateTime]) =>
          onSuccess(service.cdrs(apiUser, pager, dateFrom, dateTo)) {
            _.fold(complete(_), pagCdrs => {
              respondWithPaginationHeaders(pager, pagCdrs) {
                complete {
                  SuccessResp(GenericSuccess, data = pagCdrs.result)
                }
              }
            })
          }
        }
      }
    }
}

