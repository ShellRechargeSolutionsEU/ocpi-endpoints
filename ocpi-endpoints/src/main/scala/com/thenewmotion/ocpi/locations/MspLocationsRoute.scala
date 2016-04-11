package com.thenewmotion.ocpi.locations

import com.thenewmotion.mobilityid.{CountryCode, OperatorId}
import com.thenewmotion.ocpi.{ApiUser, JsonApi}
import com.thenewmotion.ocpi.msgs.v2_0.CommonTypes.SuccessResp
import com.thenewmotion.ocpi.msgs.v2_0.Locations._
import org.joda.time.DateTime
import spray.http.{HttpMethods, StatusCodes}
import spray.routing.{MethodRejection, PathMatcher1, Rejection, Route}

import scala.concurrent.{ExecutionContext, Future}
import scalaz._

case class LocationsErrorRejection(error: LocationsError) extends Rejection

class MspLocationsRoute(
  service: MspLocationsService,
  currentTime: => DateTime = DateTime.now
) (implicit ec: ExecutionContext) extends JsonApi {


  import com.thenewmotion.ocpi.msgs.v2_0.OcpiJsonProtocol._
  import com.thenewmotion.ocpi.msgs.v2_0.OcpiStatusCodes.GenericSuccess

  private def leftToRejection[T](errOrX: Future[LocationsError \/ T])(f: T => Route)(implicit ec: ExecutionContext): Route =
    onSuccess(errOrX) {
      case -\/(e) => reject(LocationsErrorRejection(e))
      case \/-(r) => f(r)
    }

  def route(apiUser: ApiUser) =
    handleRejections(LocationsRejectionHandler.Default) (routeWithoutRh(apiUser))

  private val CountryCodeSegment: PathMatcher1[CountryCode] = Segment map (CountryCode(_))
  private val OperatorIdSegment: PathMatcher1[OperatorId] = Segment map (OperatorId(_))


  private [locations] def routeWithoutRh(apiUser: ApiUser) = {
    pathPrefix(CountryCodeSegment / OperatorIdSegment / Segment) { (cc, opId, locId) =>
      pathEndOrSingleSlash {
        cancelRejection(MethodRejection(HttpMethods.PUT)){
          put {
            authorize(CountryCode(apiUser.countryCode) == cc) {
              entity(as[Location]) { location =>
                leftToRejection(service.createOrUpdateLocation(cc, opId, locId, location)) { res =>
                  complete((if(res)StatusCodes.Created else StatusCodes.OK, SuccessResp(GenericSuccess.code))) }
              }
            }
          }
        }
      } ~
      authorize(CountryCode(apiUser.countryCode) == cc && OperatorId(apiUser.partyId) == opId) {
        pathEndOrSingleSlash {
          patch {
            entity(as[LocationPatch]) { location =>
              leftToRejection(service.updateLocation(cc, opId, locId, location)){ _ =>
                complete(SuccessResp(GenericSuccess.code)) }
            }
          } ~
          get {
            dynamic {
              leftToRejection(service.location(cc, opId, locId)) { location =>
                complete(LocationResp(GenericSuccess.code, None, data = location)) }
            }
          }
        } ~
        pathPrefix(Segment) { evseId =>
          pathEndOrSingleSlash {
            put {
              entity(as[Evse]) { evse =>
                leftToRejection(service.addOrUpdateEvse(cc, opId, locId, evseId, evse)) { res =>
                  complete((if(res)StatusCodes.Created else StatusCodes.OK, SuccessResp(GenericSuccess.code))) }
              }
            } ~
            patch {
              entity(as[EvsePatch]) { evse =>
                leftToRejection(service.updateEvse(cc, opId, locId, evseId, evse)) { _ =>
                  complete(SuccessResp(GenericSuccess.code)) }
              }
            } ~
            get {
              dynamic {
                leftToRejection(service.evse(cc, opId, locId, evseId)) { evse =>
                  complete(EvseResp(GenericSuccess.code, None, data = evse)) }
              }
            }
          } ~
          (path(Segment) & pathEndOrSingleSlash) { connId =>
            put {
              entity(as[Connector]) { conn =>
                leftToRejection(service.addOrUpdateConnector(cc, opId, locId, evseId, connId, conn)) { res =>
                  complete((if(res)StatusCodes.Created else StatusCodes.OK, SuccessResp(GenericSuccess.code))) }
              }
            } ~
            patch {
              entity(as[ConnectorPatch]) { conn =>
                leftToRejection(service.updateConnector(cc, opId, locId, evseId, connId, conn)) { _ =>
                  complete(SuccessResp(GenericSuccess.code)) }
              }
            } ~
            get {
              dynamic {
                leftToRejection(service.connector(cc, opId, locId, evseId, connId)) { connector =>
                  complete(ConnectorResp(GenericSuccess.code, None, data = connector)) }
              }
            }
          }
        }
      }
    }
  }
}

