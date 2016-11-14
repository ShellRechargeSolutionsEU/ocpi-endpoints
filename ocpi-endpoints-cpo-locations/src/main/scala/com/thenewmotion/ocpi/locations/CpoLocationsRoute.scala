package com.thenewmotion.ocpi.locations

import com.thenewmotion.ocpi.common.{Pager, PaginatedRoute}
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.SuccessWithDataResp
import com.thenewmotion.ocpi.{ApiUser, JsonApi}
import org.joda.time.DateTime
import spray.routing.Route
import scala.concurrent.{ExecutionContext, Future}
import scalaz._



class CpoLocationsRoute(
  service: CpoLocationsService,
  val DefaultLimit: Int = 1000,
  currentTime: => DateTime = DateTime.now
) (implicit ec: ExecutionContext) extends JsonApi with PaginatedRoute {


  import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._
  import com.thenewmotion.ocpi.msgs.v2_1.OcpiStatusCode.GenericSuccess

  private def leftToRejection[T](errOrX: Future[LocationsError \/ T])(f: T => Route)(implicit ec: ExecutionContext): Route =
    onSuccess(errOrX) {
      case -\/(e) => reject(LocationsErrorRejection(e))
      case \/-(r) => f(r)
    }

  val dateLimiters = parameters(('date_from.as[String] ? "", 'date_to.as[String] ? ""))

  def route(apiUser: ApiUser) =
    handleRejections(LocationsRejectionHandler.Default) (routeWithoutRh(apiUser))



  private def limitToUse(clientLimit: Int) = Math.min(DefaultLimit, clientLimit)

  private [locations] def routeWithoutRh(apiUser: ApiUser) = {
    import com.thenewmotion.ocpi.msgs.OcpiDatetimeParser.toOcpiDateTime
    get {
      pathEndOrSingleSlash {
        dateLimiters { (dateFrom: String, dateTo: String) =>
          paged { (offset: Int, clientLimit: Int) =>
            leftToRejection(service.locations(Pager(offset, limitToUse(clientLimit)),
              toOcpiDateTime(dateFrom), toOcpiDateTime(dateTo))) { pagLocations =>
              respondWithPaginationHeaders( offset, limitToUse(clientLimit), pagLocations ) {
                complete(SuccessWithDataResp(GenericSuccess, None, data = pagLocations.result))
              }
            }
          }
        }
      } ~
      pathPrefix(Segment) { locId =>
        pathEndOrSingleSlash {
          leftToRejection(service.location(locId)) { location =>
            complete(SuccessWithDataResp(GenericSuccess, None, data = location))
          }
        } ~
          pathPrefix(Segment) { evseId =>
            pathEndOrSingleSlash {
              leftToRejection(service.evse(locId, evseId)) { evse =>
                complete(SuccessWithDataResp(GenericSuccess, None, data = evse))
              }
            } ~
              (path(Segment) & pathEndOrSingleSlash) { connId =>
                leftToRejection(service.connector(locId, evseId, connId)) { connector =>
                  complete(SuccessWithDataResp(GenericSuccess, None, data = connector))
                }
              }
          }
      }
    }
  }
}

