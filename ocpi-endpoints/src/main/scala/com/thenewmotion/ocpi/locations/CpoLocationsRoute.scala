package com.thenewmotion.ocpi.locations

import com.thenewmotion.ocpi.common.{Pager, PaginatedRoute}
import com.thenewmotion.ocpi.msgs.v2_0.Locations._
import com.thenewmotion.ocpi.{ApiUser, JsonApi}
import org.joda.time.DateTime
import spray.routing.Route
import scala.concurrent.{ExecutionContext, Future}
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

  def route(apiUser: ApiUser) =
    handleRejections(LocationsRejectionHandler.Default) (routeWithoutRh(apiUser))

  private [locations] def routeWithoutRh(apiUser: ApiUser) = {
    pathEndOrSingleSlash {
      get {
        paged { (offset: Int, limit: Int) =>
          leftToRejection(service.locations(Pager(offset, limit))) { pagLocations =>
            respondWithPaginationHeaders( offset, pagLocations ) {
              complete(LocationsResp(GenericSuccess.code, None, data = pagLocations.result))
            }
          }
        }
      }
    }
  }
}

