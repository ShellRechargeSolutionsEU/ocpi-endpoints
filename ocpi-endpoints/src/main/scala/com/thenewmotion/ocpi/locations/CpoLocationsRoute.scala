package com.thenewmotion.ocpi.locations

import com.thenewmotion.ocpi.common.{Pager, PaginatedRoute}
import com.thenewmotion.ocpi.msgs.v2_0.Locations._
import com.thenewmotion.ocpi.{ApiUser, JsonApi}
import org.joda.time.DateTime
import spray.routing.Route
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scalaz._



class CpoLocationsRoute(
  service: CpoLocationsService,
  currentTime: => DateTime = DateTime.now
) (implicit ec: ExecutionContext) extends JsonApi with PaginatedRoute {


  import com.thenewmotion.ocpi.msgs.v2_0.OcpiJsonProtocol._
  import com.thenewmotion.ocpi.msgs.v2_0.OcpiStatusCodes.GenericSuccess

  private def leftToRejection[T](errOrX: Future[LocationsError \/ T])(f: T => Route)(implicit ec: ExecutionContext): Route =
    onSuccess(errOrX) {
      case -\/(e) => reject(LocationsErrorRejection(e))
      case \/-(r) => f(r)
    }

  lazy val DateMin = DateTime.parse("")
  lazy val DateMax = DateTime.now
  val dateLimiters = parameters(('date_from.as[String] ? "", 'date_to.as[String] ? ""))

  def route(apiUser: ApiUser) =
    handleRejections(LocationsRejectionHandler.Default) (routeWithoutRh(apiUser))

  private def toDateTime(dt: String) = Try(DateTime.parse(dt)).toOption

  private [locations] def routeWithoutRh(apiUser: ApiUser) = {
    pathEndOrSingleSlash {
      get {
        dateLimiters { (dateFrom: String, dateTo: String) =>
          paged { (offset: Int, limit: Int) =>
            leftToRejection(service.locations(Pager(offset, limit),
              toDateTime(dateFrom), toDateTime(dateTo))) { pagLocations =>
              respondWithPaginationHeaders( offset, pagLocations ) {
                complete(LocationsResp(GenericSuccess.code, None, data = pagLocations.result))
              }
            }
          }
        }
      }
    }
  }
}

