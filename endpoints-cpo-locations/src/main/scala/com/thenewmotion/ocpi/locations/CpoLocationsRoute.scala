package com.thenewmotion.ocpi
package locations

import java.time.ZonedDateTime

import akka.http.scaladsl.marshalling.{ToEntityMarshaller, ToResponseMarshaller}
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Route
import com.thenewmotion.ocpi.msgs.v2_1.Locations._
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
)(
  implicit errorM: ToEntityMarshaller[ErrorResp],
  successIterableLocM: ToEntityMarshaller[SuccessResp[Iterable[Location]]],
  successLocM: ToEntityMarshaller[SuccessResp[Location]],
  successEvseM: ToEntityMarshaller[SuccessResp[Evse]],
  successConnM: ToEntityMarshaller[SuccessResp[Connector]]
) extends JsonApi with PaginatedRoute with EitherUnmarshalling {

  private val DefaultErrorMsg = Some("An error occurred.")

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

  def route(
    apiUser: GlobalPartyId
  )(
    implicit executionContext: ExecutionContext
  ): Route =
    handleRejections(OcpiRejectionHandler.Default) (routeWithoutRh(apiUser))

  private val LocationIdSegment = Segment.map(LocationId(_))
  private val EvseUidSegment = Segment.map(EvseUid(_))
  private val ConnectorIdSegment = Segment.map(ConnectorId(_))

  private [locations] def routeWithoutRh(
    apiUser: GlobalPartyId
  )(
    implicit executionContext: ExecutionContext
  ) = {
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
      pathPrefix(LocationIdSegment) { locId =>
        pathEndOrSingleSlash {
          complete {
            service.location(locId).mapRight { location =>
              SuccessResp(GenericSuccess, data = location)
            }
          }
        } ~
        pathPrefix(EvseUidSegment) { evseId =>
          pathEndOrSingleSlash {
            complete {
              service.evse(locId, evseId).mapRight { evse =>
                SuccessResp(GenericSuccess, data = evse)
              }
            }
          } ~
          (path(ConnectorIdSegment) & pathEndOrSingleSlash) { connId =>
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
