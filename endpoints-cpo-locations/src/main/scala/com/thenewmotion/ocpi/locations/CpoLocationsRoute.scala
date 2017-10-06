package com.thenewmotion.ocpi
package locations

import java.time.ZonedDateTime

import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes._
import msgs.{ErrorResp, GlobalPartyId, SuccessResp}
import common._
import locations.LocationsError._
import msgs.OcpiStatusCode.GenericClientFailure
import msgs.OcpiStatusCode.GenericSuccess

import scala.concurrent.ExecutionContext

class CpoLocationsRoute(
  service: CpoLocationsService,
  val DefaultLimit: Int = 1000,
  val MaxLimit: Int = 1000,
  currentTime: => ZonedDateTime = ZonedDateTime.now
) extends JsonApi with PaginatedRoute with EitherUnmarshalling {

  private val DefaultErrorMsg = Some("An error occurred.")

  import msgs.v2_1.OcpiJsonProtocol._

  implicit def locationsErrorResp(
    implicit em: ToResponseMarshaller[(StatusCode, ErrorResp)]
  ): ToResponseMarshaller[LocationsError] = {
    em.compose[LocationsError] { locationsError =>
      val statusCode = locationsError match {
        case (_: LocationNotFound | _: EvseNotFound | _: ConnectorNotFound) => NotFound
        case _ => InternalServerError
      }
      statusCode -> ErrorResp(GenericClientFailure, locationsError.reason.orElse(DefaultErrorMsg))
    }
  }

  def route(apiUser: GlobalPartyId)(implicit executionContext: ExecutionContext) =
    handleRejections(OcpiRejectionHandler.Default) (routeWithoutRh(apiUser))

  private [locations] def routeWithoutRh(apiUser: GlobalPartyId)(implicit executionContext: ExecutionContext) = {
    get {
      pathEndOrSingleSlash {
        paged { (pager: Pager, dateFrom: Option[ZonedDateTime], dateTo: Option[ZonedDateTime]) =>
          onSuccess(service.locations(pager, dateFrom, dateTo
          )) {
             _.fold(complete(_), pagLocations => {
              respondWithPaginationHeaders(pager, pagLocations) {
                complete {
                  SuccessResp(GenericSuccess, data = pagLocations.result)
                }
              }
            })
          }
        }
      } ~
      pathPrefix(Segment) { locId =>
        pathEndOrSingleSlash {
          complete {
            service.location(locId).mapRight { location =>
              SuccessResp(GenericSuccess, data = location)
            }
          }
        } ~
        pathPrefix(Segment) { evseId =>
          pathEndOrSingleSlash {
            complete {
              service.evse(locId, evseId).mapRight { evse =>
                SuccessResp(GenericSuccess, data = evse)
              }
            }
          } ~
          (path(Segment) & pathEndOrSingleSlash) { connId =>
            complete {
              service.connector(locId, evseId, connId).mapRight { connector =>
                SuccessResp(GenericSuccess, data = connector)
              }
            }
          }
        }
      }
    }
  }
}
