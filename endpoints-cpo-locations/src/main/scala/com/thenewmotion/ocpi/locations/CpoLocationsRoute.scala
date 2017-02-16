package com.thenewmotion.ocpi
package locations

import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes._
import msgs.{ErrorResp, GlobalPartyId, SuccessWithDataResp}
import common._
import locations.LocationsError._
import msgs.OcpiStatusCode.GenericClientFailure
import msgs.OcpiStatusCode.GenericSuccess
import org.joda.time.DateTime
import scala.concurrent.ExecutionContext

class CpoLocationsRoute(
  service: CpoLocationsService,
  val DefaultLimit: Int = 1000,
  val MaxLimit: Int = 1000,
  currentTime: => DateTime = DateTime.now
) extends JsonApi with PaginatedRoute with DisjunctionMarshalling {

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
        paged { (pager: Pager, dateFrom: Option[DateTime], dateTo: Option[DateTime]) =>
          onSuccess(service.locations(pager, dateFrom, dateTo
          )) {
             _.fold(complete(_), pagLocations => {
              respondWithPaginationHeaders(pager, pagLocations) {
                complete {
                  SuccessWithDataResp(GenericSuccess, None, data = pagLocations.result)
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
              SuccessWithDataResp(GenericSuccess, None, data = location)
            }
          }
        } ~
        pathPrefix(Segment) { evseId =>
          pathEndOrSingleSlash {
            complete {
              service.evse(locId, evseId).mapRight { evse =>
                SuccessWithDataResp(GenericSuccess, None, data = evse)
              }
            }
          } ~
          (path(Segment) & pathEndOrSingleSlash) { connId =>
            complete {
              service.connector(locId, evseId, connId).mapRight { connector =>
                SuccessWithDataResp(GenericSuccess, None, data = connector)
              }
            }
          }
        }
      }
    }
  }
}
