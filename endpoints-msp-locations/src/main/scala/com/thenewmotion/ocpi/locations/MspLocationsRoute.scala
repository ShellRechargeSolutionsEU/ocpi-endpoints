package com.thenewmotion.ocpi
package locations

import akka.http.scaladsl.marshalling.{ToEntityMarshaller, ToResponseMarshaller}
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import msgs.ErrorResp
import common.{EitherUnmarshalling, OcpiDirectives, OcpiRejectionHandler}
import locations.LocationsError._
import msgs.v2_1.Locations._
import msgs._
import msgs.OcpiStatusCode._

import scala.concurrent.ExecutionContext

class MspLocationsRoute(
  service: MspLocationsService
)(
  implicit locationU: FromEntityUnmarshaller[Location],
  locationPU: FromEntityUnmarshaller[LocationPatch],
  evseU: FromEntityUnmarshaller[Evse],
  evsePU: FromEntityUnmarshaller[EvsePatch],
  connectorU: FromEntityUnmarshaller[Connector],
  connectorPU: FromEntityUnmarshaller[ConnectorPatch],
  errorM: ToEntityMarshaller[ErrorResp],
  successUnitM: ToEntityMarshaller[SuccessResp[Unit]],
  successLocM: ToEntityMarshaller[SuccessResp[Location]],
  successEvseM: ToEntityMarshaller[SuccessResp[Evse]],
  successConnectorM: ToEntityMarshaller[SuccessResp[Connector]]
) extends JsonApi with EitherUnmarshalling with OcpiDirectives {

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

  def route(
    apiUser: GlobalPartyId
  )(
    implicit executionContext: ExecutionContext
  ): Route =
    handleRejections(OcpiRejectionHandler.Default)(routeWithoutRh(apiUser))

  private val LocationIdSegment = Segment.map(LocationId(_))
  private val EvseUidSegment = Segment.map(EvseUid(_))
  private val ConnectorIdSegment = Segment.map(ConnectorId(_))

  private[locations] def routeWithoutRh(
    apiUser: GlobalPartyId
  )(
    implicit executionContext: ExecutionContext
  ) = {
    (authPathPrefixGlobalPartyIdEquality(apiUser) & pathPrefix(LocationIdSegment)) { locId =>
      pathEndOrSingleSlash {
        put {
          entity(as[Location]) { location =>
            complete {
              service.createOrUpdateLocation(apiUser, locId, location).mapRight { created =>
                (if (created) Created else OK, SuccessResp(GenericSuccess))
              }
            }
          }
        } ~
        patch {
          entity(as[LocationPatch]) { location =>
            complete {
              service.updateLocation(apiUser, locId, location).mapRight { _ =>
                SuccessResp(GenericSuccess)
              }
            }
          }
        } ~
        get {
          complete {
            service.location(apiUser, locId).mapRight { location =>
              SuccessResp(GenericSuccess, None, data = location)
            }
          }
        }
      } ~
      pathPrefix(EvseUidSegment) { evseId =>
        pathEndOrSingleSlash {
          put {
            entity(as[Evse]) { evse =>
              complete {
                service.addOrUpdateEvse(apiUser, locId, evseId, evse).mapRight { created =>
                  (if (created) Created else OK, SuccessResp(GenericSuccess))
                }
              }
            }
          } ~
          patch {
            entity(as[EvsePatch]) { evse =>
              complete {
                service.updateEvse(apiUser, locId, evseId, evse).mapRight { _ =>
                  SuccessResp(GenericSuccess)
                }
              }
            }
          } ~
          get {
            complete {
              service.evse(apiUser, locId, evseId).mapRight { evse =>
                SuccessResp(GenericSuccess, None, data = evse)
              }
            }
          }
        } ~
        (path(ConnectorIdSegment) & pathEndOrSingleSlash) { connId =>
          put {
            entity(as[Connector]) { conn =>
              complete {
                service.addOrUpdateConnector(apiUser, locId, evseId, connId, conn).mapRight { created =>
                  (if (created) Created else OK, SuccessResp(GenericSuccess))
                }
              }
            }
          } ~
          patch {
            entity(as[ConnectorPatch]) { conn =>
              complete {
                service.updateConnector(apiUser, locId, evseId, connId, conn).mapRight { _ =>
                  SuccessResp(GenericSuccess)
                }
              }
            }
          } ~
          get {
            complete {
              service.connector(apiUser, locId, evseId, connId).mapRight {
                connector =>
                  SuccessResp(GenericSuccess, data = connector)
              }
            }
          }
        }
      }
    }
  }
}
