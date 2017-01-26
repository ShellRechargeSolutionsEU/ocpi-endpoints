package com.thenewmotion.ocpi
package locations

import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.PathMatcher1
import com.thenewmotion.mobilityid.CountryCode
import com.thenewmotion.mobilityid.OperatorIdIso
import common.{DisjunctionMarshalling, OcpiRejectionHandler, OcpiResponseUnmarshalling}
import locations.LocationsError._
import msgs.v2_1.CommonTypes.ErrorResp
import msgs.v2_1.CommonTypes.SuccessResp
import msgs.v2_1.CommonTypes.SuccessWithDataResp
import msgs.v2_1.Locations._
import msgs.v2_1.OcpiStatusCode._
import scala.concurrent.ExecutionContext

class MspLocationsRoute(
  service: MspLocationsService
) extends JsonApi with OcpiResponseUnmarshalling with DisjunctionMarshalling {

  import msgs.v2_1.OcpiJsonProtocol._

  implicit def locationsErrorResp(
    implicit em: ToResponseMarshaller[(StatusCode, ErrorResp)]
  ): ToResponseMarshaller[LocationsError] = {
      em.compose[LocationsError] { locationsError =>
      val statusCode = locationsError match {
        case (_: LocationNotFound | _: EvseNotFound | _: ConnectorNotFound) => NotFound
        case (_: LocationCreationFailed | _: EvseCreationFailed | _: ConnectorCreationFailed) => OK
        case _ => InternalServerError
      }
      statusCode -> ErrorResp(GenericClientFailure, locationsError.reason)
    }
  }

  def route(apiUser: ApiUser)(implicit executionContext: ExecutionContext) =
    handleRejections(OcpiRejectionHandler.Default)(routeWithoutRh(apiUser))

  private val CountryCodeSegment: PathMatcher1[CountryCode] = Segment.map(CountryCode(_))
  private val OperatorIdSegment: PathMatcher1[OperatorIdIso] = Segment.map(OperatorIdIso(_))
  private def isResourceAccessAuthorized(apiUser: ApiUser, cc: CountryCode, opId: OperatorIdIso) =
    authorize(CountryCode(apiUser.countryCode) == cc && OperatorIdIso(apiUser.partyId) == opId)

  private[locations] def routeWithoutRh(apiUser: ApiUser)(implicit executionContext: ExecutionContext) = {
    pathPrefix(CountryCodeSegment / OperatorIdSegment / Segment) { (cc, opId, locId) =>
      pathEndOrSingleSlash {
        put {
          isResourceAccessAuthorized(apiUser, cc, opId) {
            entity(as[Location]) { location =>
              complete {
                service.createOrUpdateLocation(cc, opId, locId, location).mapRight { created =>
                  (if (created) Created else OK, SuccessResp(GenericSuccess))
                }
              }
            }
          }
        } ~
        patch {
          isResourceAccessAuthorized(apiUser, cc, opId) {
            entity(as[LocationPatch]) { location =>
              complete {
                service.updateLocation(cc, opId, locId, location).mapRight { _ =>
                  SuccessResp(GenericSuccess)
                }
              }
            }
          }
        } ~
        get {
          isResourceAccessAuthorized(apiUser, cc, opId) {
            complete {
              service.location(cc, opId, locId).mapRight { location =>
                SuccessWithDataResp(GenericSuccess, None, data = location)
              }
            }
          }
        }
      } ~
      isResourceAccessAuthorized(apiUser, cc, opId) {
        pathPrefix(Segment) { evseId =>
          pathEndOrSingleSlash {
            put {
              entity(as[Evse]) { evse =>
                complete {
                  service.addOrUpdateEvse(cc, opId, locId, evseId, evse).mapRight { created =>
                    (if (created) Created else OK, SuccessResp(GenericSuccess))
                  }
                }
              }
            } ~
            patch {
              entity(as[EvsePatch]) { evse =>
                complete {
                  service.updateEvse(cc, opId, locId, evseId, evse).mapRight { _ =>
                    SuccessResp(GenericSuccess)
                  }
                }
              }
            } ~
            get {
              complete {
                service.evse(cc, opId, locId, evseId).mapRight { evse =>
                  SuccessWithDataResp(GenericSuccess, None, data = evse)
                }
              }
            }
          } ~
          (path(Segment) & pathEndOrSingleSlash) { connId =>
            put {
              entity(as[Connector]) { conn =>
                complete {
                  service.addOrUpdateConnector(cc, opId, locId, evseId, connId, conn).mapRight { created =>
                    (if (created) Created else OK, SuccessResp(GenericSuccess))
                  }
                }
              }
            } ~
            patch {
              entity(as[ConnectorPatch]) { conn =>
                complete {
                  service.updateConnector(cc, opId, locId, evseId, connId, conn).mapRight { _ =>
                  SuccessResp(GenericSuccess)
                  }
                }
              }
            } ~
            get {
              complete {
                service.connector(cc, opId, locId, evseId, connId).mapRight {
                  connector =>
                    SuccessWithDataResp(GenericSuccess, None, data = connector)
                }
              }
            }
          }
        }
      }
    }
  }
}
