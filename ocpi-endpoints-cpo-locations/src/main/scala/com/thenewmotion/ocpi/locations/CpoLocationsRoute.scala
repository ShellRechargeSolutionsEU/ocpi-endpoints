package com.thenewmotion.ocpi
package locations

import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes._
import common.AuthorizationRejectionHandler
import common.DateDeserializers
import common.Pager
import common.PaginatedRoute
import common.ResponseMarshalling
import locations.LocationsError._
import msgs.v2_1.CommonTypes.ErrorResp
import msgs.v2_1.CommonTypes.SuccessWithDataResp
import msgs.v2_1.OcpiStatusCode.GenericClientFailure
import msgs.v2_1.OcpiStatusCode.GenericSuccess
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

import scala.concurrent.ExecutionContext

class CpoLocationsRoute(
  service: CpoLocationsService,
  val DefaultLimit: Int = 1000,
  currentTime: => DateTime = DateTime.now
) extends JsonApi with PaginatedRoute with DateDeserializers
  with ResponseMarshalling {

  val dateFromParam = "date_from"
  val dateToParam = "date_to"

  val dateLimiters = parameters((dateFromParam.as[DateTime].?, dateToParam.as[DateTime].?))

  val formatter = ISODateTimeFormat.dateTime.withZoneUTC

  private val DefaultErrorMsg = Some("An error occurred.")

  import msgs.v2_1.OcpiJsonProtocol._

  implicit def locationsErrorResp(implicit errorMarshaller: ToResponseMarshaller[(StatusCode, ErrorResp)]): ToResponseMarshaller[LocationsError] = {
      errorMarshaller.compose[LocationsError] { locationsError =>
      val statusCode = locationsError match {
        case (_: LocationNotFound | _: EvseNotFound | _: ConnectorNotFound) => NotFound
        case _ => InternalServerError
      }
      statusCode -> ErrorResp(GenericClientFailure, locationsError.reason.orElse(DefaultErrorMsg))
    }
  }

  def route(apiUser: ApiUser)(implicit executionContext: ExecutionContext) =
    handleRejections(AuthorizationRejectionHandler.Default) (routeWithoutRh(apiUser))

  private def limitToUse(clientLimit: Int) = Math.min(DefaultLimit, clientLimit)

  private[locations] def routeWithoutRh(apiUser: ApiUser)(implicit executionContext: ExecutionContext) = {
    get {
      pathEndOrSingleSlash {
        dateLimiters { (dateFrom: Option[DateTime], dateTo: Option[DateTime]) =>
          paged { (offset: Int, clientLimit: Int) =>
            onSuccess(service.locations(
              Pager(offset, limitToUse(clientLimit)), dateFrom, dateTo
            )) { _.fold(complete(_), pagLocations => {
              val params = Map.empty ++
                dateFrom.map(x => dateFromParam -> formatter.print(x)) ++
                dateTo.map(x => dateToParam -> formatter.print(x))

              respondWithPaginationHeaders(
                offset, limitToUse(clientLimit), params, pagLocations
              ) {
                complete {
                  SuccessWithDataResp(GenericSuccess, None, data = pagLocations.result)
                }
              }
            })}
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
